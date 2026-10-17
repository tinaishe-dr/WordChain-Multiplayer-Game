package wordchain.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import wordchain.client.RoomState;
import wordchain.common.Message;
import wordchain.common.Protocol;

/** The match screen: timer, tiles, scoreboard, word-chain log and chat. */
public final class GamePanel extends JPanel {
    private final Consumer<Message> send;
    private final TileBoard board = new TileBoard();
    private final TimerBar timer = new TimerBar();
    private final PlayersPanel players = new PlayersPanel(true);
    private final HistoryPanel history = new HistoryPanel();
    private final ChatPanel chat = new ChatPanel();
    private final JLabel roomLabel = Widgets.label("", Theme.font(Font.BOLD, 14f), Theme.MUTED);
    private final JLabel banner = Widgets.label(" ", Theme.font(Font.BOLD, 30f), Theme.TEXT);
    private final JLabel sub = Widgets.label(" ", Theme.font(Font.PLAIN, 14f), Theme.MUTED);
    private final JLabel message = Widgets.label(" ", Theme.font(Font.BOLD, 14f), Theme.MUTED);
    private final Widgets.RoundedButton submit = new Widgets.RoundedButton("Submit", Widgets.RoundedButton.Kind.PRIMARY);
    private final Widgets.RoundedButton hint = new Widgets.RoundedButton("Hint", Widgets.RoundedButton.Kind.SECONDARY);
    private RoomState room;
    private String me = "";
    private String active = "";

    public GamePanel(Consumer<Message> send) {
        super(new BorderLayout(18, 0));
        this.send = send;
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildCenter(), BorderLayout.CENTER);
        add(buildSide(), BorderLayout.EAST);

        board.setListener(new TileBoard.Listener() {
            @Override public void onSubmit(int pos, char letter) {
                send.accept(new Message(Protocol.MOVE, pos, String.valueOf(letter)));
            }
            @Override public void onProblem(String text) { flash(text, Theme.WARN); }
        });
        chat.onSend(t -> send.accept(new Message(Protocol.CHAT, t)));
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(roomLabel, BorderLayout.WEST);
        Widgets.RoundedButton resign = new Widgets.RoundedButton("Resign", Widgets.RoundedButton.Kind.DANGER);
        resign.setBorder(new EmptyBorder(6, 14, 6, 14));
        resign.addActionListener(e -> send.accept(new Message(Protocol.RESIGN)));
        top.add(resign, BorderLayout.EAST);
        center.add(top, c);

        banner.setHorizontalAlignment(JLabel.CENTER);
        sub.setHorizontalAlignment(JLabel.CENTER);
        c.insets = new Insets(18, 0, 2, 0);
        center.add(banner, c);
        c.insets = new Insets(0, 0, 14, 0);
        center.add(sub, c);
        c.insets = new Insets(0, 40, 14, 40);
        center.add(timer, c);

        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        center.add(board, c);

        message.setHorizontalAlignment(JLabel.CENTER);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        center.add(message, c);

        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        submit.addActionListener(e -> board.submit());
        hint.addActionListener(e -> send.accept(new Message(Protocol.HINT)));
        buttons.add(hint);
        buttons.add(submit);
        c.insets = new Insets(14, 0, 4, 0);
        center.add(buttons, c);
        return center;
    }

    private JPanel buildSide() {
        JPanel side = new JPanel(new GridBagLayout());
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(320, 100));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 12, 0);
        side.add(players, c);
        c.weighty = 0.42;
        side.add(history, c);
        c.weighty = 0.58;
        c.insets = new Insets(0, 0, 0, 0);
        side.add(chat, c);
        return side;
    }

    public ChatPanel chat() { return chat; }

    // ----------------------------------------------------- server events

    public void onRoom(RoomState r, String myName) {
        room = r;
        me = myName;
        roomLabel.setText("ROOM " + r.code());
        players.update(r, me, active);
        updateHint();
    }

    public void onGameStart(String word) {
        history.clear();
        history.addStart(word);
        board.setWord(word);
        active = "";
        banner.setText("Get ready\u2026");
        banner.setForeground(Theme.TEXT);
        sub.setText(" ");
        flash(" ", Theme.MUTED);
    }

    public void onTurn(String player, int seconds, String word, int chainLength) {
        active = player;
        boolean mine = player.equals(me);
        board.setWord(word);
        board.setInteractive(mine);
        timer.start(seconds);
        banner.setText(mine ? "Your turn!" : player + "'s turn");
        banner.setForeground(mine ? Theme.GOOD : Theme.TEXT);
        sub.setText(mine ? "Click a letter to erase it, type a replacement, press Enter"
                : "Waiting for " + player + "\u2026   \u2022   chain: " + chainLength + " words   \u2022   " + seconds + "s per turn");
        if (mine && !chat.isTyping()) board.requestFocusInWindow();
        if (room != null) players.update(room, me, active);
        updateHint();
        flash(" ", Theme.MUTED);
    }

    public void onAccepted(String player, String oldWord, String newWord, int pos, int points) {
        history.addMove(player, oldWord, newWord, pos, points);
        board.setInteractive(false);
        board.setWord(newWord);
        board.popTile(pos);
        timer.stop("");
        flash(player + " made " + newWord.toUpperCase() + "  (+" + points + ")", Theme.GOOD);
    }

    public void onRejected(String reason, int pos) {
        board.shake();
        flash(reason, Theme.DANGER);
    }

    public void onTimeout(String player, int livesLeft) {
        history.addNote("<span style='color:#F87171'>\u23F1 " + player + " ran out of time ("
                + livesLeft + (livesLeft == 1 ? " life" : " lives") + " left)</span>");
        board.setInteractive(false);
        board.shake();
        flash(player.equals(me) ? "Time's up! You lost a life." : player + " ran out of time!", Theme.DANGER);
    }

    public void onEliminated(String player) {
        history.addNote("<span style='color:#F87171'>\u2716 " + player + " is out</span>");
    }

    public void onDeadEnd(String stuckWord, String bonusPlayer, int bonus, String newWord) {
        String who = bonusPlayer.isEmpty() ? "" : " \u2014 " + bonusPlayer + " +" + bonus;
        history.addNote("<span style='color:#FBBF24'>\uD83D\uDEA7 Dead end at " + stuckWord.toUpperCase() + who + "</span>");
        history.addStart(newWord);
        board.setWord(newWord);
        flash("Dead end! New word: " + newWord.toUpperCase() + (bonusPlayer.isEmpty() ? "" : "  (" + bonusPlayer + " +" + bonus + " bonus)"),
                Theme.WARN);
    }

    public void onHint(int pos) {
        board.showHint(pos);
        flash("Hint: a word can be made by changing the highlighted letter.", Theme.WARN);
    }

    public void stopClock(String text) { timer.stop(text); }

    private void updateHint() {
        int left = 0;
        if (room != null) {
            for (RoomState.Player p : room.players()) if (p.name().equals(me)) left = p.hints();
        }
        hint.setText("Hint (" + left + ")");
        hint.setEnabled(left > 0 && me.equals(active));
        submit.setEnabled(me.equals(active));
    }

    private void flash(String text, Color color) {
        message.setText(text);
        message.setForeground(color);
    }
}
