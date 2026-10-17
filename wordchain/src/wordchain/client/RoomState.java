package wordchain.client;

import java.util.ArrayList;
import java.util.List;

import wordchain.common.Message;
import wordchain.common.Protocol;

/** Immutable snapshot of a room as last reported by the server. */
public record RoomState(String code, String host, String phase, int maxPlayers, int turnSeconds,
                        int lives, String customWord, List<Player> players) {

    public record Player(String name, int lives, int score, int hints) {
        public boolean alive() { return lives > 0; }
    }

    public static RoomState parse(Message m) {
        List<Player> list = new ArrayList<>();
        String raw = m.arg(7);
        if (!raw.isEmpty()) {
            for (String entry : raw.split(",")) {
                String[] f = entry.split(":");
                if (f.length < 4) continue;
                list.add(new Player(f[0], toInt(f[1]), toInt(f[2]), toInt(f[3])));
            }
        }
        return new RoomState(m.arg(0), m.arg(1), m.arg(2), m.intArg(3, 4), m.intArg(4, 20),
                m.intArg(5, 3), m.arg(6), list);
    }

    public boolean isHost(String me) { return me != null && me.equals(host); }

    public boolean playing() { return Protocol.PHASE_PLAYING.equals(phase); }

    private static int toInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}
