package wordchain.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import wordchain.common.Message;
import wordchain.common.Protocol;
import wordchain.common.WordRules;

/**
 * One game room: lobby, settings and the full turn-based game state machine.
 * All public entry points are synchronized, so timer and network threads never interleave.
 */
final class GameRoom {
    private static final int BASE_POINTS = 10, SPEED_POINTS = 2, DEAD_END_BONUS = 25;
    private static final int HINTS_PER_PLAYER = 2, HINT_COST = 5;
    private static final int MIN_TURN_SECONDS = 8, WORDS_PER_SPEEDUP = 4;

    private enum Phase { LOBBY, PLAYING }

    private static final class PlayerState {
        final ClientSession session;
        String name;
        int lives, score, hints = HINTS_PER_PLAYER;
        boolean connected = true;

        PlayerState(ClientSession session, String name, int lives) {
            this.session = session;
            this.name = name;
            this.lives = lives;
        }
    }

    private final String code;
    private final GameServer server;
    private final Dictionary dict;
    private final Random rnd = new Random();
    private final GameSettings settings = new GameSettings();
    private final List<PlayerState> players = new ArrayList<>();
    private final Set<String> used = new HashSet<>();

    private Phase phase = Phase.LOBBY;
    private PlayerState host;
    private PlayerState lastMover;
    private String word = "";
    private int turn, chain;
    private long turnId, deadlineMillis;
    private ScheduledFuture<?> timer;

    GameRoom(String code, GameServer server) {
        this.code = code;
        this.server = server;
        this.dict = server.dictionary();
    }

    // ---------------------------------------------------------------- lobby

    synchronized String join(ClientSession s) {
        if (phase == Phase.PLAYING) return "That game is already in progress.";
        if (players.size() >= settings.maxPlayers) return "That room is full.";
        String name = uniqueName(s.name());
        s.setName(name);
        PlayerState p = new PlayerState(s, name, settings.lives);
        players.add(p);
        if (host == null) host = p;
        s.send(new Message(Protocol.JOINED, code, name));
        broadcast(new Message(Protocol.INFO, name + " joined the room"));
        broadcastRoom();
        return null;
    }

    synchronized void leave(ClientSession s) {
        PlayerState p = find(s);
        if (p == null) return;
        p.connected = false;
        if (phase == Phase.PLAYING) {
            if (p.lives > 0) {
                broadcast(new Message(Protocol.INFO, p.name + " left the game"));
                eliminate(p);
            }
        } else {
            players.remove(p);
            broadcast(new Message(Protocol.INFO, p.name + " left the room"));
        }
        if (host == null || !host.connected) host = firstConnected();
        if (firstConnected() == null) {
            cancelTimer();
            server.removeRoom(code);
            return;
        }
        broadcastRoom();
    }

    synchronized void updateSettings(ClientSession s, int maxPlayers, int seconds, int lives, String custom) {
        PlayerState p = find(s);
        if (p == null || p != host || phase != Phase.LOBBY) return;
        String w = WordRules.normalise(custom);
        if (!w.isEmpty()) {
            if (!WordRules.isWellFormed(w)) { error(s, "Custom word must be 3-8 letters (A-Z)."); return; }
            if (!dict.contains(w)) { error(s, "\"" + w.toUpperCase() + "\" is not in the dictionary."); return; }
            if (!dict.hasMove(w, Set.of(w))) { error(s, "No words can be made from \"" + w.toUpperCase() + "\"."); return; }
        }
        settings.maxPlayers = Math.max(players.size(),
                GameSettings.clamp(maxPlayers, GameSettings.MIN_PLAYERS, GameSettings.MAX_PLAYERS));
        settings.turnSeconds = GameSettings.clamp(seconds, GameSettings.MIN_SECONDS, GameSettings.MAX_SECONDS);
        settings.lives = GameSettings.clamp(lives, GameSettings.MIN_LIVES, GameSettings.MAX_LIVES);
        settings.customWord = w;
        for (PlayerState ps : players) ps.lives = settings.lives;
        broadcastRoom();
    }

    synchronized void chat(ClientSession s, String text) {
        PlayerState p = find(s);
        String t = text.trim();
        if (p == null || t.isEmpty()) return;
        broadcast(new Message(Protocol.CHAT_MSG, p.name, t.length() > 200 ? t.substring(0, 200) : t));
    }

    // ----------------------------------------------------------------- game

    synchronized void start(ClientSession s) {
        PlayerState p = find(s);
        if (p == null || p != host || phase != Phase.LOBBY) return;
        if (players.size() < GameSettings.MIN_PLAYERS) { error(s, "You need at least 2 players to start."); return; }

        used.clear();
        chain = 0;
        lastMover = null;
        for (PlayerState ps : players) {
            ps.lives = settings.lives;
            ps.score = 0;
            ps.hints = HINTS_PER_PLAYER;
        }
        word = settings.customWord.isEmpty() ? dict.randomStarter(rnd, used) : settings.customWord;
        used.add(word);
        turn = rnd.nextInt(players.size());
        phase = Phase.PLAYING;
        broadcast(new Message(Protocol.GAME_START, word));
        broadcastRoom();
        beginTurn();
    }

    synchronized void move(ClientSession s, int pos, String letter) {
        PlayerState p = find(s);
        if (phase != Phase.PLAYING || p == null || players.get(turn) != p) {
            reject(s, "It's not your turn.", -1);
            return;
        }
        String l = WordRules.normalise(letter);
        if (pos < 0 || pos >= word.length()) { reject(s, "Pick a letter in the word first.", -1); return; }
        if (l.length() != 1 || l.charAt(0) < 'a' || l.charAt(0) > 'z') { reject(s, "Type a letter from A to Z.", pos); return; }
        char c = l.charAt(0);
        if (c == word.charAt(pos)) { reject(s, "Use a different letter.", pos); return; }

        String next = WordRules.replaceAt(word, pos, c);
        if (!dict.contains(next)) { reject(s, next.toUpperCase() + " is not a valid English word.", pos); return; }
        if (used.contains(next)) { reject(s, next.toUpperCase() + " was already played.", pos); return; }

        cancelTimer();
        int secondsLeft = (int) Math.max(0, (deadlineMillis - System.currentTimeMillis()) / 1000);
        chain++;
        int points = BASE_POINTS + secondsLeft * SPEED_POINTS + Math.min(chain, 10);
        p.score += points;
        String old = word;
        word = next;
        used.add(next);
        lastMover = p;
        broadcast(new Message(Protocol.ACCEPTED, p.name, old, next, pos, points));
        broadcastRoom();
        advanceTurn();
        beginTurn();
    }

    synchronized void hint(ClientSession s) {
        PlayerState p = find(s);
        if (phase != Phase.PLAYING || p == null || players.get(turn) != p) { error(s, "Hints only work on your turn."); return; }
        if (p.hints <= 0) { error(s, "You have no hints left."); return; }
        List<Integer> positions = dict.playablePositions(word, used);
        if (positions.isEmpty()) return;
        p.hints--;
        p.score = Math.max(0, p.score - HINT_COST);
        s.send(new Message(Protocol.HINT_POS, positions.get(rnd.nextInt(positions.size()))));
        broadcast(new Message(Protocol.INFO, p.name + " used a hint (-" + HINT_COST + " pts)"));
        broadcastRoom();
    }

    synchronized void resign(ClientSession s) {
        PlayerState p = find(s);
        if (phase != Phase.PLAYING || p == null || p.lives <= 0) return;
        broadcast(new Message(Protocol.INFO, p.name + " resigned"));
        eliminate(p);
    }

    private void beginTurn() {
        if (!dict.hasMove(word, used)) {
            String stuck = word;
            int bonus = lastMover == null ? 0 : DEAD_END_BONUS;
            if (lastMover != null) lastMover.score += bonus;
            word = dict.randomStarter(rnd, used);
            used.add(word);
            broadcast(new Message(Protocol.DEAD_END, stuck, lastMover == null ? "" : lastMover.name, bonus, word));
            broadcastRoom();
            lastMover = null;
        }
        PlayerState p = players.get(turn);
        int seconds = turnSeconds();
        long id = ++turnId;
        deadlineMillis = System.currentTimeMillis() + seconds * 1000L;
        broadcast(new Message(Protocol.TURN, p.name, seconds, word, chain));
        timer = server.scheduler().schedule(() -> onTimeout(id), seconds, TimeUnit.SECONDS);
    }

    private synchronized void onTimeout(long id) {
        if (phase != Phase.PLAYING || id != turnId) return;
        PlayerState p = players.get(turn);
        p.lives--;
        broadcast(new Message(Protocol.TIMEOUT, p.name, p.lives));
        if (p.lives <= 0) broadcast(new Message(Protocol.ELIMINATED, p.name));
        broadcastRoom();
        if (checkGameOver()) return;
        advanceTurn();
        beginTurn();
    }

    /** Takes a player out of the game (resign or disconnect) and moves play along. */
    private void eliminate(PlayerState p) {
        boolean wasCurrent = players.get(turn) == p;
        p.lives = 0;
        broadcast(new Message(Protocol.ELIMINATED, p.name));
        broadcastRoom();
        if (checkGameOver()) return;
        if (wasCurrent) {
            cancelTimer();
            advanceTurn();
            beginTurn();
        }
    }

    private boolean checkGameOver() {
        List<PlayerState> alive = players.stream().filter(x -> x.lives > 0).toList();
        if (alive.size() > 1) return false;
        finish(alive.isEmpty() ? null : alive.get(0));
        return true;
    }

    private void finish(PlayerState winner) {
        cancelTimer();
        turnId++;
        phase = Phase.LOBBY;
        List<PlayerState> ranking = new ArrayList<>(players);
        ranking.sort(Comparator.<PlayerState>comparingInt(x -> x.lives > 0 ? 0 : 1).thenComparing(x -> -x.score));
        List<Object> args = new ArrayList<>();
        args.add(winner == null ? "" : winner.name);
        for (PlayerState x : ranking) args.add(x.name + ":" + x.score + ":" + (x.lives > 0 ? 1 : 0));
        broadcast(new Message(Protocol.GAME_OVER, args.toArray()));

        players.removeIf(x -> !x.connected);
        if (host == null || !host.connected) host = firstConnected();
        for (PlayerState x : players) { x.lives = settings.lives; x.score = 0; x.hints = HINTS_PER_PLAYER; }
        broadcastRoom();
    }

    private void advanceTurn() {
        do { turn = (turn + 1) % players.size(); } while (players.get(turn).lives <= 0);
    }

    private int turnSeconds() {
        int floor = Math.min(MIN_TURN_SECONDS, settings.turnSeconds);
        return Math.max(floor, settings.turnSeconds - chain / WORDS_PER_SPEEDUP);
    }

    private void cancelTimer() {
        if (timer != null) timer.cancel(false);
    }

    // -------------------------------------------------------------- helpers

    private PlayerState find(ClientSession s) {
        for (PlayerState p : players) if (p.session == s) return p;
        return null;
    }

    private PlayerState firstConnected() {
        for (PlayerState p : players) if (p.connected) return p;
        return null;
    }

    private String uniqueName(String base) {
        String name = base;
        for (int n = 2; taken(name); n++) name = base + " " + n;
        return name;
    }

    private boolean taken(String name) {
        for (PlayerState p : players) if (p.name.equalsIgnoreCase(name)) return true;
        return false;
    }

    private void broadcast(Message m) {
        for (PlayerState p : players) if (p.connected) p.session.send(m);
    }

    private void broadcastRoom() {
        StringBuilder sb = new StringBuilder();
        for (PlayerState p : players) {
            if (sb.length() > 0) sb.append(',');
            sb.append(p.name).append(':').append(p.lives).append(':').append(p.score).append(':').append(p.hints);
        }
        broadcast(new Message(Protocol.ROOM, code, host == null ? "" : host.name, phase.name(), settings.maxPlayers,
                settings.turnSeconds, settings.lives, settings.customWord, sb));
    }

    private static void error(ClientSession s, String text) { s.send(new Message(Protocol.ERROR, text)); }

    private static void reject(ClientSession s, String reason, int pos) { s.send(new Message(Protocol.REJECTED, reason, pos)); }
}
