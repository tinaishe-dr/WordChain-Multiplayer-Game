package wordchain.client.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import wordchain.common.Message;

/** Client-side socket wrapper: a reader thread delivers messages to a listener. UI-agnostic. */
public final class Connection {
    public interface Listener {
        void onMessage(Message m);
        void onClosed(String reason);
    }

    private final Listener listener;
    private Socket socket;
    private PrintWriter out;
    private volatile boolean closed;

    public Connection(Listener listener) { this.listener = listener; }

    /** Blocking connect; call off the UI thread. */
    public void connect(String host, int port) throws IOException {
        socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.connect(new InetSocketAddress(host, port), 4000);
        out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        Thread reader = new Thread(() -> readLoop(in), "wordchain-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop(BufferedReader in) {
        try {
            for (String line; (line = in.readLine()) != null; ) {
                if (!line.isBlank()) listener.onMessage(Message.decode(line));
            }
        } catch (IOException ignored) {
            // fall through to onClosed
        }
        if (!closed) listener.onClosed("Connection to the server was lost.");
    }

    public synchronized void send(Message m) {
        if (out != null && !closed) out.println(m.encode());
    }

    public void close() {
        closed = true;
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
    }
}
