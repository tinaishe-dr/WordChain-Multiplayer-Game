package com.wordchain.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.BindException;
import java.util.ArrayList;
import java.util.List;

import wordchain.common.Message;
import wordchain.common.Protocol;
import wordchain.net.Connection;
import wordchain.server.Dictionary;
import wordchain.server.GameServer;

/** Single-activity host: owns the connection, routes server messages and swaps between screens. */
public final class MainActivity extends Activity implements Connection.Listener, Sender {
    private final FeedModel feed = new FeedModel();
    private FrameLayout content;
    private TextView status;
    private ConnectScreen connectScreen;
    private MenuScreen menuScreen;
    private LobbyScreen lobbyScreen;
    private GameScreen gameScreen;
    private FeedDialog feedDialog;
    private ResultDialog resultDialog;

    private Connection connection;
    private GameServer localServer;
    private RoomState room;
    private String me = "";
    private String baseStatus = " ";
    private View current;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        installCrashReporter();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        content = new FrameLayout(this);
        root.addView(content, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        status = Ui.text(this, " ", 12, Ui.MUTED, false);
        status.setBackgroundColor(Ui.SURFACE);
        status.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 6));
        root.addView(status, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(root);

        connectScreen = new ConnectScreen(this, new ConnectScreen.Handler() {
            @Override public void connect(String name, String host, int port, boolean hostHere) {
                startConnection(name, host, port, hostHere);
            }
        });
        menuScreen = new MenuScreen(this, new MenuScreen.Handler() {
            @Override public void create() { send(new Message(Protocol.CREATE)); }
            @Override public void join(String code) {
                if (code.length() != 4) toast("Room codes are 4 characters.", true);
                else send(new Message(Protocol.JOIN, code));
            }
            @Override public void disconnect() { dropConnection("Disconnected."); }
        });
        lobbyScreen = new LobbyScreen(this, this, new Runnable() { @Override public void run() { showFeed(); } });
        gameScreen = new GameScreen(this, this, feed, new Runnable() { @Override public void run() { showFeed(); } });
        feedDialog = new FeedDialog(this, feed, this);
        show(connectScreen);
        showPreviousCrash();
    }

    // ------------------------------------------------------------ crash report

    /** Saves any uncaught exception so the next launch can show it (handy when no logcat is available). */
    private void installCrashReporter() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                getSharedPreferences("wordchain", MODE_PRIVATE).edit().putString("lastCrash", "Thread " + t.getName() + "\n" + sw).commit();
                if (previous != null) previous.uncaughtException(t, e);
            }
        });
    }

    private void showPreviousCrash() {
        final android.content.SharedPreferences prefs = getSharedPreferences("wordchain", MODE_PRIVATE);
        String crash = prefs.getString("lastCrash", null);
        if (crash == null) return;
        prefs.edit().remove("lastCrash").apply();
        TextView body = Ui.text(this, crash.length() > 2500 ? crash.substring(0, 2500) : crash, 11, Ui.TEXT, false);
        body.setTextIsSelectable(true);
        body.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(body);
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("The app crashed last time")
                .setView(sv)
                .setPositiveButton("OK", null)
                .show();
    }

    // ------------------------------------------------------------ plumbing

    private void show(View screen) {
        if (current == screen) return;
        content.removeAllViews();
        content.addView(screen, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        current = screen;
        if (screen == gameScreen) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override public void send(Message m) { if (connection != null) connection.send(m); }

    private void showFeed() { if (!feedDialog.isShowing()) feedDialog.show(); }

    private void startConnection(final String name, final String requestedHost, final int port, final boolean hostHere) {
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                String note = "";
                try {
                    if (hostHere) {
                        try {
                            localServer = new GameServer(Dictionary.load(getAssets().open("words.txt"), getAssets().open("starters.txt")));
                            localServer.start(port);
                            List<String> ips = GameServer.localAddresses();
                            StringBuilder sb = new StringBuilder("Hosting on port " + port);
                            if (!ips.isEmpty()) sb.append(" \u2014 friends join at ").append(join(ips));
                            note = sb.toString();
                        } catch (BindException e) {
                            localServer = null;
                            note = "Port " + port + " already has a server \u2014 joined that one.";
                        }
                    }
                    final Connection c = new Connection(MainActivity.this);
                    c.connect(requestedHost, port);
                    c.send(new Message(Protocol.HELLO, name));
                    final String hostNote = note;
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            connection = c;
                            menuScreen.setInfo("Connected to " + requestedHost + ":" + port, hostNote);
                        }
                    });
                } catch (final Throwable ex) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            connectScreen.fail(hostHere ? "Couldn't start the server: " + ex.getMessage()
                                    : "Couldn't connect to " + requestedHost + ":" + port + ". Is the server running?");
                        }
                    });
                }
            }
        }, "wordchain-connect");
        t.setDaemon(true);
        t.start();
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(" or ");
            sb.append(p);
        }
        return sb.toString();
    }

    private void dropConnection(String text) {
        if (connection != null) connection.close();
        connection = null;
        room = null;
        dismissResult();
        if (feedDialog.isShowing()) feedDialog.dismiss();
        if (localServer != null) { localServer.stop(); localServer = null; }
        connectScreen.fail(text);
        show(connectScreen);
    }

    // ------------------------------------------------------------ incoming

    @Override public void onMessage(final Message m) {
        runOnUiThread(new Runnable() { @Override public void run() { handle(m); } });
    }

    @Override public void onClosed(final String reason) {
        runOnUiThread(new Runnable() {
            @Override public void run() { if (connection != null) dropConnection(reason); }
        });
    }

    private void handle(Message m) {
        switch (m.type()) {
            case Protocol.WELCOME:
                me = m.arg(0);
                show(menuScreen);
                break;
            case Protocol.JOINED:
                me = m.arg(1);
                feed.clear();
                show(lobbyScreen);
                setBase("Room " + m.arg(0) + "  \u2022  playing as " + me);
                break;
            case Protocol.LEFT:
                room = null;
                dismissResult();
                show(menuScreen);
                setBase(" ");
                break;
            case Protocol.ROOM:
                room = RoomState.parse(m);
                lobbyScreen.update(room, me);
                gameScreen.onRoom(room, me);
                break;
            case Protocol.GAME_START:
                dismissResult();
                gameScreen.onGameStart(m.arg(0));
                show(gameScreen);
                break;
            case Protocol.TURN: gameScreen.onTurn(m.arg(0), m.intArg(1, 20), m.arg(2), m.intArg(3, 0)); break;
            case Protocol.ACCEPTED:
                gameScreen.onAccepted(m.arg(0), m.arg(1), m.arg(2), m.intArg(3, 0), m.intArg(4, 0));
                break;
            case Protocol.REJECTED: gameScreen.onRejected(m.arg(0)); break;
            case Protocol.TIMEOUT: gameScreen.onTimeout(m.arg(0), m.intArg(1, 0)); break;
            case Protocol.ELIMINATED: gameScreen.onEliminated(m.arg(0)); break;
            case Protocol.DEAD_END: gameScreen.onDeadEnd(m.arg(0), m.arg(1), m.intArg(2, 0), m.arg(3)); break;
            case Protocol.HINT_POS: gameScreen.onHint(m.intArg(0, -1)); break;
            case Protocol.GAME_OVER: showResult(m); break;
            case Protocol.CHAT_MSG: feed.addChat(m.arg(0), m.arg(1), m.arg(0).equals(me)); break;
            case Protocol.INFO: feed.addSystem(m.arg(0)); break;
            case Protocol.ERROR: toast(m.arg(0), true); break;
            default: break;
        }
    }

    private void showResult(Message m) {
        gameScreen.stopClock("Game over");
        List<ResultDialog.Standing> list = new ArrayList<>();
        for (int i = 1; i < m.size(); i++) {
            String[] f = m.arg(i).split(":");
            if (f.length >= 3) list.add(new ResultDialog.Standing(f[0], Integer.parseInt(f[1]), "1".equals(f[2])));
        }
        dismissResult();
        if (feedDialog.isShowing()) feedDialog.dismiss();
        resultDialog = new ResultDialog(this, m.arg(0), me, list, new Runnable() {
            @Override public void run() { show(lobbyScreen); }
        });
        resultDialog.show();
    }

    private void dismissResult() {
        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();
        resultDialog = null;
    }

    // -------------------------------------------------------------- status

    private void setBase(String text) { baseStatus = text; resetStatus(); }

    private final Runnable resetStatusTask = new Runnable() { @Override public void run() { resetStatus(); } };

    private void resetStatus() { status.setText(baseStatus); status.setTextColor(Ui.MUTED); }

    private void toast(String text, boolean error) {
        status.setText(text);
        status.setTextColor(error ? Ui.DANGER : Ui.GOOD);
        status.removeCallbacks(resetStatusTask);
        status.postDelayed(resetStatusTask, 4500);
    }

    // ----------------------------------------------------------- lifecycle

    @Override public void onBackPressed() {
        if (current == gameScreen) gameScreen.confirmResign();
        else if (current == lobbyScreen) send(new Message(Protocol.LEAVE));
        else if (current == menuScreen) {
            new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setTitle("Disconnect?")
                    .setNegativeButton("Stay", null)
                    .setPositiveButton("Disconnect", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int w) { dropConnection("Disconnected."); }
                    }).show();
        } else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (connection != null) connection.close();
        if (localServer != null) localServer.stop();
        super.onDestroy();
    }
}
