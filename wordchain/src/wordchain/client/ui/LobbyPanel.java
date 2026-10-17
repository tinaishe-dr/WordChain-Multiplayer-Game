package wordchain.client.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import wordchain.client.RoomState;
import wordchain.common.Message;
import wordchain.common.Protocol;

/** Waiting room: share the code, see who's here, host tweaks the rules and starts the game. */
public final class LobbyPanel extends JPanel {
    private final Consumer<Message> send;
    private final JLabel code = Widgets.label("----", Theme.font(Font.BOLD, 46f), Theme.ACCENT_2);
    private final JLabel hint = Widgets.label(" ", Theme.font(Font.PLAIN, 13f), Theme.MUTED);
    private final PlayersPanel players = new PlayersPanel(false);
    private final Widgets.Stepper maxPlayers = new Widgets.Stepper(2, 4, 1, "");
    private final Widgets.Stepper seconds = new Widgets.Stepper(10, 60, 5, "s");
    private final Widgets.Stepper lives = new Widgets.Stepper(1, 5, 1, "");
    private final JTextField custom = Widgets.textField(10);
    private final Widgets.RoundedButton apply = new Widgets.RoundedButton("Set", Widgets.RoundedButton.Kind.SECONDARY);
    private final Widgets.RoundedButton start = new Widgets.RoundedButton("Start game", Widgets.RoundedButton.Kind.PRIMARY);
    private final ChatPanel chat = new ChatPanel();
    private String appliedWord = "";

    public LobbyPanel(Consumer<Message> send) {
        super(new GridBagLayout());
        this.send = send;
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        Widgets.RoundedPanel left = new Widgets.RoundedPanel(24, Theme.SURFACE).outline(Theme.BORDER);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(22, 24, 22, 24));
        left.add(row(Widgets.label("ROOM CODE", Theme.font(Font.BOLD, 12f), Theme.MUTED)));
        JPanel codeRow = new JPanel(new BorderLayout(14, 0));
        codeRow.setOpaque(false);
        codeRow.setAlignmentX(LEFT_ALIGNMENT);
        codeRow.add(code, BorderLayout.WEST);
        Widgets.RoundedButton copy = new Widgets.RoundedButton("Copy", Widgets.RoundedButton.Kind.SECONDARY);
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code.getText()), null);
            copy.setText("Copied!");
            new javax.swing.Timer(1400, ev -> copy.setText("Copy")) {{ setRepeats(false); start(); }};
        });
        JPanel copyWrap = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 12));
        copyWrap.setOpaque(false);
        copyWrap.add(copy);
        codeRow.add(copyWrap, BorderLayout.CENTER);
        left.add(codeRow);
        left.add(row(hint));
        left.add(javax.swing.Box.createVerticalStrut(16));
        left.add(row(Widgets.label("PLAYERS", Theme.font(Font.BOLD, 12f), Theme.MUTED)));
        left.add(javax.swing.Box.createVerticalStrut(8));
        players.setAlignmentX(LEFT_ALIGNMENT);
        left.add(players);
        left.add(javax.swing.Box.createVerticalStrut(10));
        left.add(row(Widgets.label("RULES", Theme.font(Font.BOLD, 12f), Theme.MUTED)));
        left.add(javax.swing.Box.createVerticalStrut(8));
        left.add(setting("Max players", maxPlayers));
        left.add(setting("Seconds per turn", seconds));
        left.add(setting("Lives each", lives));

        JPanel wordRow = new JPanel(new BorderLayout(8, 0));
        wordRow.setOpaque(false);
        wordRow.setAlignmentX(LEFT_ALIGNMENT);
        wordRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        wordRow.add(Widgets.label("Custom start word", Theme.font(Font.PLAIN, 14f), Theme.TEXT), BorderLayout.WEST);
        JPanel wf = new JPanel(new BorderLayout(8, 0));
        wf.setOpaque(false);
        custom.setToolTipText("Leave empty for a random word");
        wf.add(custom, BorderLayout.CENTER);
        wf.add(apply, BorderLayout.EAST);
        wordRow.add(wf, BorderLayout.CENTER);
        left.add(wordRow);
        left.add(javax.swing.Box.createVerticalGlue());

        start.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        start.setAlignmentX(LEFT_ALIGNMENT);
        Widgets.RoundedButton leave = new Widgets.RoundedButton("Leave room", Widgets.RoundedButton.Kind.GHOST);
        leave.setAlignmentX(LEFT_ALIGNMENT);
        left.add(javax.swing.Box.createVerticalStrut(14));
        left.add(start);
        left.add(javax.swing.Box.createVerticalStrut(8));
        left.add(leave);

        maxPlayers.onChange(v -> pushSettings(appliedWord));
        seconds.onChange(v -> pushSettings(appliedWord));
        lives.onChange(v -> pushSettings(appliedWord));
        apply.addActionListener(e -> pushSettings(custom.getText()));
        custom.addActionListener(e -> pushSettings(custom.getText()));
        start.addActionListener(e -> send.accept(new Message(Protocol.START)));
        leave.addActionListener(e -> send.accept(new Message(Protocol.LEAVE)));
        chat.onSend(t -> send.accept(new Message(Protocol.CHAT, t)));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.55;
        c.insets = new Insets(0, 0, 0, 12);
        add(left, c);
        c.weightx = 0.45;
        c.insets = new Insets(0, 12, 0, 0);
        add(chat, c);
    }

    public ChatPanel chat() { return chat; }

    public void update(RoomState room, String me) {
        boolean host = room.isHost(me);
        code.setText(room.code());
        players.update(room, me, "");
        maxPlayers.setEnabled(host);
        seconds.setEnabled(host);
        lives.setEnabled(host);
        custom.setEnabled(host);
        apply.setEnabled(host);
        maxPlayers.set(room.maxPlayers());
        seconds.set(room.turnSeconds());
        lives.set(room.lives());
        appliedWord = room.customWord();
        if (!custom.isFocusOwner() || !host) custom.setText(appliedWord.toUpperCase());
        start.setVisible(host);
        start.setEnabled(room.players().size() >= 2);
        hint.setText(host
                ? (room.players().size() >= 2 ? "You're the host \u2014 start whenever everyone's in."
                        : "Share this code with a friend. You need at least 2 players.")
                : "Waiting for " + room.host() + " to start the game\u2026");
    }

    private void pushSettings(String word) {
        send.accept(new Message(Protocol.SETTINGS, maxPlayers.get(), seconds.get(), lives.get(), word.trim()));
    }

    private static JPanel setting(String label, Widgets.Stepper stepper) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        p.add(Widgets.label(label, Theme.font(Font.PLAIN, 14f), Theme.TEXT), BorderLayout.WEST);
        p.add(stepper, BorderLayout.EAST);
        return p;
    }

    private static JPanel row(java.awt.Component c) {
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        p.add(c);
        return p;
    }
}
