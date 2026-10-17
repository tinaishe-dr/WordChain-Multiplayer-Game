package wordchain.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import wordchain.common.Message;
import wordchain.common.Protocol;

/** Server-side handler for one connected client: reads commands and routes them to its room. */
final class ClientSession implements Runnable {
    private final Socket socket;
    private final GameServer server;
    private final BufferedReader in;
    private final PrintWriter out;
    private volatile String name = "Player";
    private volatile GameRoom room;

    ClientSession(Socket socket, GameServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
    }

    String name() { return name; }

    void setName(String n) { name = n; }

    synchronized void send(Message m) { out.println(m.encode()); }

    @Override
    public void run() {
        try {
            for (String line; (line = in.readLine()) != null; ) {
                if (!line.trim().isEmpty()) handle(Message.decode(line));
            }
        } catch (IOException ignored) {
            // client went away
        } finally {
            disconnect();
        }
    }

    private void handle(Message m) {
        switch (m.type()) {
            case Protocol.HELLO:
                name = sanitiseName(m.arg(0));
                send(new Message(Protocol.WELCOME, name));
                break;
            case Protocol.CREATE:
                enter(server.createRoom());
                break;
            case Protocol.JOIN:
                GameRoom r = server.findRoom(m.arg(0).trim().toUpperCase());
                if (r == null) send(new Message(Protocol.ERROR, "Room not found. Check the code and try again."));
                else enter(r);
                break;
            case Protocol.LEAVE:
                leaveRoom();
                send(new Message(Protocol.LEFT));
                break;
            default:
                routeToRoom(m);
        }
    }

    private void enter(GameRoom target) {
        if (room != null) { send(new Message(Protocol.ERROR, "You are already in a room.")); return; }
        String error = target.join(this);
        if (error == null) room = target;
        else send(new Message(Protocol.ERROR, error));
    }

    private void routeToRoom(Message m) {
        GameRoom r = room;
        if (r == null) return;
        switch (m.type()) {
            case Protocol.SETTINGS:
                r.updateSettings(this, m.intArg(0, 4), m.intArg(1, 20), m.intArg(2, 3), m.arg(3));
                break;
            case Protocol.START: r.start(this); break;
            case Protocol.MOVE: r.move(this, m.intArg(0, -1), m.arg(1)); break;
            case Protocol.HINT: r.hint(this); break;
            case Protocol.CHAT: r.chat(this, m.arg(0)); break;
            case Protocol.RESIGN: r.resign(this); break;
            default: break;
        }
    }

    private void leaveRoom() {
        GameRoom r = room;
        room = null;
        if (r != null) r.leave(this);
    }

    void disconnect() {
        leaveRoom();
        try { socket.close(); } catch (IOException ignored) { }
    }

    private static String sanitiseName(String raw) {
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-') sb.append(c);
        }
        String n = sb.toString().trim().replaceAll("\\s+", " ");
        if (n.length() > 14) n = n.substring(0, 14).trim();
        return n.isEmpty() ? "Player" + (100 + ThreadLocalRandom.current().nextInt(900)) : n;
    }
}
