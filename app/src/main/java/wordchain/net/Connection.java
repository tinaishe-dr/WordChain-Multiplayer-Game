package wordchain.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.nio.charset.StandardCharsets;

import wordchain.common.Message;

/** Client-side socket wrapper: a reader thread delivers messages to a listener. UI-agnostic. */
public final class Connection {
    public interface Listener {
        void onMessage(Message m);
        void onClosed(String reason);
    }

    private final Listener listener;
    /** Android forbids socket writes on the UI thread, so every send goes through one writer thread (keeps order). */
    private final ExecutorService writer = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "wordchain-writer");
            t.setDaemon(true);
            return t;
        }
    });
    private Socket socket;
    private volatile PrintWriter out;
    private volatile boolean closed;

    public Connection(Listener listener) { this.listener = listener; }

    /** Blocking connect; call off the UI thread. */
    public void connect(String host, int port) throws IOException {
        socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.connect(new InetSocketAddress(host, port), 4000);
        out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
        final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        Thread reader = new Thread(new Runnable() {
            @Override public void run() { readLoop(in); }
        }, "wordchain-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop(BufferedReader in) {
        try {
            for (String line; (line = in.readLine()) != null; ) {
                if (!line.trim().isEmpty()) listener.onMessage(Message.decode(line));
            }
        } catch (IOException ignored) {
            // fall through to onClosed
        }
        if (!closed) listener.onClosed("Connection to the server was lost.");
    }

    public void send(final Message m) {
        if (closed) return;
        try {
            writer.execute(new Runnable() {
                @Override public void run() {
                    PrintWriter w = out;
                    if (w != null && !closed) w.println(m.encode());
                }
            });
        } catch (RejectedExecutionException ignored) {
            // connection already closed
        }
    }

    public void close() {
        closed = true;
        writer.shutdownNow();
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
    }
}
