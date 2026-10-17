package wordchain.client.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.net.BindException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import wordchain.client.RoomState;
import wordchain.client.net.Connection;
import wordchain.common.Message;
import wordchain.common.Protocol;
import wordchain.server.Dictionary;
import wordchain.server.GameServer;

/** Top-level window: owns the connection, routes server messages to screens and swaps between them. */
public final class MainFrame extends JFrame implements Connection.Listener {
    private static final String CONNECT = "connect", MENU = "menu", LOBBY = "lobby", GAME = "game";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final JLabel status = Widgets.label(" ", Theme.font(Font.PLAIN, 13f), Theme.MUTED);
    private final Timer statusReset = new Timer(4500, e -> resetStatus());
    private final ResultOverlay overlay = new ResultOverlay();

    private final ConnectPanel connectPanel = new ConnectPanel(this::connect);
    private final MenuPanel menuPanel = new MenuPanel(new MenuPanel.Handler() {
        public void create() { send(new Message(Protocol.CREATE)); }
        public void join(String code) {
            if (code.length() != 4) toast("Room codes are 4 characters.", true);
            else send(new Message(Protocol.JOIN, code));
        }
        public void disconnect() { dropConnection("Disconnected."); }
    });
    private final LobbyPanel lobbyPanel = new LobbyPanel(this::send);
    private final GamePanel gamePanel = new GamePanel(this::send);

    private Connection connection;
    private GameServer localServer;
    private RoomState room;
    private String me = "";
    private String baseStatus = " ";

    public MainFrame() {
        super("WordChain");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 700));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG);

        root.add(connectPanel, CONNECT);
        root.add(menuPanel, MENU);
        root.add(lobbyPanel, LOBBY);
        root.add(gamePanel, GAME);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.SURFACE);
        bar.setBorder(new EmptyBorder(6, 16, 6, 16));
        bar.add(status, BorderLayout.WEST);
        statusReset.setRepeats(false);

        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);
        add(bar, BorderLayout.SOUTH);
        setGlassPane(overlay);
        overlay.setVisible(false);
        cards.show(root, CONNECT);
    }

    // ------------------------------------------------------------ actions

    private void send(Message m) {
        if (connection != null) connection.send(m);
    }

    private void connect(String name, String host, int port, boolean hostHere) {
        Thread t = new Thread(() -> {
            String serverNote = "";
            try {
                if (hostHere) {
                    try {
                        localServer = new GameServer(Dictionary.load());
                        localServer.start(port);
                        List<String> ips = GameServer.localAddresses();
                        serverNote = "Hosting on port " + port + (ips.isEmpty() ? "" : " \u2014 friends join at " + String.join(" or ", ips));
                    } catch (BindException e) {
                        localServer = null;
                        serverNote = "Port " + port + " already has a server \u2014 joined that one.";
                    }
                }
                Connection c = new Connection(this);
                c.connect(host, port);
                c.send(new Message(Protocol.HELLO, name));
                String note = serverNote;
                SwingUtilities.invokeLater(() -> {
                    connection = c;
                    menuPanel.setInfo("Connected to " + host + ":" + port, note);
                });
            } catch (Exception ex) {
                String msg = ex instanceof java.io.FileNotFoundException ? ex.getMessage()
                        : "Couldn't connect to " + host + ":" + port + ". Is the server running?";
                SwingUtilities.invokeLater(() -> connectPanel.fail(msg));
            }
        }, "wordchain-connect");
        t.setDaemon(true);
        t.start();
    }

    private void dropConnection(String text) {
        if (connection != null) connection.close();
        connection = null;
        room = null;
        overlay.dismiss();
        connectPanel.fail(text);
        cards.show(root, CONNECT);
    }

    // ------------------------------------------------------------ incoming

    @Override public void onMessage(Message m) { SwingUtilities.invokeLater(() -> handle(m)); }

    @Override public void onClosed(String reason) {
        SwingUtilities.invokeLater(() -> { if (connection != null) dropConnection(reason); });
    }

    private void handle(Message m) {
        switch (m.type()) {
            case Protocol.WELCOME -> { me = m.arg(0); cards.show(root, MENU); }
            case Protocol.JOINED -> {
                me = m.arg(1);
                lobbyPanel.chat().clear();
                gamePanel.chat().clear();
                cards.show(root, LOBBY);
                setBase("Room " + m.arg(0) + "  \u2022  playing as " + me);
            }
            case Protocol.LEFT -> { room = null; overlay.dismiss(); cards.show(root, MENU); setBase(" "); }
            case Protocol.ROOM -> {
                room = RoomState.parse(m);
                lobbyPanel.update(room, me);
                gamePanel.onRoom(room, me);
            }
            case Protocol.GAME_START -> { overlay.dismiss(); gamePanel.onGameStart(m.arg(0)); cards.show(root, GAME); }
            case Protocol.TURN -> gamePanel.onTurn(m.arg(0), m.intArg(1, 20), m.arg(2), m.intArg(3, 0));
            case Protocol.ACCEPTED -> gamePanel.onAccepted(m.arg(0), m.arg(1), m.arg(2), m.intArg(3, 0), m.intArg(4, 0));
            case Protocol.REJECTED -> gamePanel.onRejected(m.arg(0), m.intArg(1, -1));
            case Protocol.TIMEOUT -> gamePanel.onTimeout(m.arg(0), m.intArg(1, 0));
            case Protocol.ELIMINATED -> gamePanel.onEliminated(m.arg(0));
            case Protocol.DEAD_END -> gamePanel.onDeadEnd(m.arg(0), m.arg(1), m.intArg(2, 0), m.arg(3));
            case Protocol.HINT_POS -> gamePanel.onHint(m.intArg(0, -1));
            case Protocol.GAME_OVER -> showResult(m);
            case Protocol.CHAT_MSG -> {
                lobbyPanel.chat().addChat(m.arg(0), m.arg(1), m.arg(0).equals(me));
                gamePanel.chat().addChat(m.arg(0), m.arg(1), m.arg(0).equals(me));
            }
            case Protocol.INFO -> { lobbyPanel.chat().addSystem(m.arg(0)); gamePanel.chat().addSystem(m.arg(0)); }
            case Protocol.ERROR -> toast(m.arg(0), true);
            default -> { }
        }
    }

    private void showResult(Message m) {
        gamePanel.stopClock("Game over");
        List<ResultOverlay.Standing> list = new ArrayList<>();
        for (int i = 1; i < m.size(); i++) {
            String[] f = m.arg(i).split(":");
            if (f.length >= 3) list.add(new ResultOverlay.Standing(f[0], Integer.parseInt(f[1]), "1".equals(f[2])));
        }
        overlay.show(m.arg(0), me, list, () -> { overlay.dismiss(); cards.show(root, LOBBY); });
    }

    // -------------------------------------------------------------- status

    private void setBase(String text) { baseStatus = text; resetStatus(); }

    private void resetStatus() { status.setText(baseStatus); status.setForeground(Theme.MUTED); }

    private void toast(String text, boolean error) {
        status.setText(text);
        status.setForeground(error ? Theme.DANGER : Theme.GOOD);
        statusReset.restart();
    }

    @Override public void dispose() {
        if (connection != null) connection.close();
        if (localServer != null) localServer.stop();
        super.dispose();
    }
}
