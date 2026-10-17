import java.util.*;
import java.util.concurrent.*;
import wordchain.client.net.Connection;
import wordchain.common.*;
import wordchain.server.GameServer;
import wordchain.server.Dictionary;

public class BotTest {
    static Dictionary dict;
    static final Set<String> used = ConcurrentHashMap.newKeySet();
    static final BlockingQueue<String> log = new LinkedBlockingQueue<>();

    static class Bot implements Connection.Listener {
        final String name; Connection c; String code = ""; boolean idle;  // idle bot lets its timer expire
        int moves;
        Bot(String n, boolean idle) { name = n; this.idle = idle; }
        void connect() throws Exception { c = new Connection(this); c.connect("127.0.0.1", 5099); c.send(new Message(Protocol.HELLO, name)); }
        public void onClosed(String r) { log.add(name + " closed " + r); }
        public void onMessage(Message m) {
            log.add(name + " <- " + m);
            switch (m.type()) {
                case Protocol.JOINED -> code = m.arg(0);
                case Protocol.GAME_START -> { used.clear(); used.add(m.arg(0)); }
                case Protocol.ACCEPTED -> used.add(m.arg(2));
                case Protocol.DEAD_END -> used.add(m.arg(3));
                case Protocol.TURN -> {
                    if (!m.arg(0).equals(name) || idle) return;
                    String w = m.arg(2);
                    if (moves == 0) { c.send(new Message(Protocol.MOVE, 0, "q")); c.send(new Message(Protocol.HINT)); }  // invalid word + hint
                    List<String> n = dict.neighbours(w, used);
                    if (n.isEmpty()) return;
                    String pick = n.get(0);
                    int pos = 0; for (; pos < w.length(); pos++) if (w.charAt(pos) != pick.charAt(pos)) break;
                    moves++;
                    c.send(new Message(Protocol.MOVE, pos, String.valueOf(pick.charAt(pos))));
                }
                default -> {}
            }
        }
    }

    public static void main(String[] a) throws Exception {
        dict = Dictionary.load();
        GameServer s = new GameServer(dict); s.start(5099);
        Bot A = new Bot("Ana", false), B = new Bot("Ben", false), C = new Bot("Cy", true);
        A.connect(); B.connect(); C.connect(); Thread.sleep(300);
        A.c.send(new Message(Protocol.CREATE)); Thread.sleep(300);
        B.c.send(new Message(Protocol.JOIN, A.code.toLowerCase())); Thread.sleep(200);
        C.c.send(new Message(Protocol.JOIN, "ZZZZ")); Thread.sleep(200);          // not found
        C.c.send(new Message(Protocol.JOIN, A.code)); Thread.sleep(200);
        A.c.send(new Message(Protocol.SETTINGS, 3, 10, 1, "xyzzy")); Thread.sleep(200);   // invalid custom
        A.c.send(new Message(Protocol.SETTINGS, 3, 10, 1, "talk")); Thread.sleep(200);    // valid, 1 life
        A.c.send(new Message(Protocol.CHAT, "hello there")); Thread.sleep(100);
        B.c.send(new Message(Protocol.START)); Thread.sleep(100);                  // non-host: ignored
        A.c.send(new Message(Protocol.START));
        long end = System.currentTimeMillis() + 28000;
        while (System.currentTimeMillis() < end) {
            String l = log.poll(500, TimeUnit.MILLISECONDS);
            if (l != null && !l.contains("<- ROOM")) System.out.println(l);
            if (l != null && l.contains("GAME_OVER") && l.startsWith("Ana")) break;
        }
        s.stop();
        System.exit(0);
    }
}
