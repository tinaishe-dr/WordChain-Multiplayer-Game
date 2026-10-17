package wordchain.server;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** Accepts TCP connections and owns the set of live rooms. Can run standalone or inside the client. */
public final class GameServer {
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final Dictionary dictionary;
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> daemon(r, "timer"));
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> daemon(r, "session"));
    private final Random random = new Random();
    private volatile ServerSocket serverSocket;

    public GameServer(Dictionary dictionary) { this.dictionary = dictionary; }

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        Thread t = daemon(this::acceptLoop, "accept");
        t.start();
    }

    public void stop() {
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) { }
        scheduler.shutdownNow();
        pool.shutdownNow();
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket s = serverSocket.accept();
                s.setTcpNoDelay(true);
                pool.execute(new ClientSession(s, this));
            } catch (IOException e) {
                if (serverSocket.isClosed()) return;
            }
        }
    }

    Dictionary dictionary() { return dictionary; }

    ScheduledExecutorService scheduler() { return scheduler; }

    GameRoom findRoom(String code) { return rooms.get(code); }

    void removeRoom(String code) { rooms.remove(code); }

    GameRoom createRoom() {
        while (true) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            String code = sb.toString();
            GameRoom room = new GameRoom(code, this);
            if (rooms.putIfAbsent(code, room) == null) return room;
        }
    }

    /** Non-loopback IPv4 addresses, handy for telling friends how to connect. */
    public static List<String> localAddresses() {
        List<String> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address) out.add(a.getHostAddress());
                }
            }
        } catch (IOException ignored) { }
        return out;
    }

    private static Thread daemon(Runnable r, String name) {
        Thread t = new Thread(r, "wordchain-" + name);
        t.setDaemon(true);
        return t;
    }
}
