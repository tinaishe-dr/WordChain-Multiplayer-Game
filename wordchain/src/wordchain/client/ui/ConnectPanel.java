package wordchain.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import wordchain.common.Protocol;

/** First screen: pick a name and either join a server or host one on this computer. */
public final class ConnectPanel extends JPanel {
    public interface Handler {
        void connect(String name, String host, int port, boolean hostHere);
    }

    private final JTextField name = Widgets.textField(16);
    private final JTextField host = Widgets.textField(16);
    private final JTextField port = Widgets.textField(6);
    private final JCheckBox hostHere = new JCheckBox("Host the game on this computer");
    private final JLabel status = Widgets.label(" ", Theme.font(Font.PLAIN, 13f), Theme.MUTED);
    private final Widgets.RoundedButton go = new Widgets.RoundedButton("Play", Widgets.RoundedButton.Kind.PRIMARY);

    public ConnectPanel(Handler handler) {
        super(new GridBagLayout());
        setBackground(Theme.BG);

        Widgets.RoundedPanel card = new Widgets.RoundedPanel(26, Theme.SURFACE).outline(Theme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(30, 36, 30, 36));
        card.setPreferredSize(new Dimension(440, 520));

        JLabel logo = Widgets.label("WordChain", Theme.font(Font.BOLD, 38f), Theme.TEXT);
        JLabel tag = Widgets.label("Swap one letter. Make a new word. Don't run out of time.",
                Theme.font(Font.PLAIN, 13f), Theme.MUTED);
        card.add(left(logo));
        card.add(left(tag));
        card.add(javax.swing.Box.createVerticalStrut(26));

        name.setText(System.getProperty("user.name", "Player"));
        host.setText("localhost");
        port.setText(String.valueOf(Protocol.DEFAULT_PORT));
        hostHere.setOpaque(false);
        hostHere.setForeground(Theme.TEXT);
        hostHere.setFont(Theme.font(Font.PLAIN, 14f));
        hostHere.setFocusPainted(false);
        hostHere.addActionListener(e -> host.setEnabled(!hostHere.isSelected()));

        card.add(field("Your name", name));
        card.add(javax.swing.Box.createVerticalStrut(14));
        JPanel serverRow = new JPanel(new GridBagLayout());
        serverRow.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        serverRow.add(field("Server address", host), c);
        c.weightx = 0;
        c.insets = new Insets(0, 10, 0, 0);
        serverRow.add(field("Port", port), c);
        card.add(serverRow);
        card.add(javax.swing.Box.createVerticalStrut(14));
        card.add(left(hostHere));
        card.add(javax.swing.Box.createVerticalStrut(22));

        go.setAlignmentX(LEFT_ALIGNMENT);
        go.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        go.addActionListener(e -> submit(handler));
        name.addActionListener(e -> submit(handler));
        card.add(go);
        card.add(javax.swing.Box.createVerticalStrut(14));
        card.add(left(status));
        add(card);
    }

    private void submit(Handler handler) {
        int p;
        try {
            p = Integer.parseInt(port.getText().trim());
            if (p < 1 || p > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            setStatus("Port must be a number from 1 to 65535.", Theme.DANGER);
            return;
        }
        String n = name.getText().trim();
        if (n.isEmpty()) { setStatus("Please enter a name.", Theme.DANGER); return; }
        String h = host.getText().trim();
        if (!hostHere.isSelected() && h.isEmpty()) { setStatus("Enter the server address.", Theme.DANGER); return; }
        setBusy(true, hostHere.isSelected() ? "Starting server\u2026" : "Connecting\u2026");
        handler.connect(n, hostHere.isSelected() ? "127.0.0.1" : h, p, hostHere.isSelected());
    }

    public void setBusy(boolean busy, String text) {
        go.setEnabled(!busy);
        setStatus(text, Theme.MUTED);
    }

    public void fail(String text) {
        go.setEnabled(true);
        setStatus(text, Theme.DANGER);
    }

    private void setStatus(String text, Color color) {
        status.setText("<html>" + text + "</html>");
        status.setForeground(color);
    }

    private static JPanel field(String title, JTextField f) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.add(left(Widgets.label(title, Theme.font(Font.BOLD, 12f), Theme.MUTED)));
        p.add(javax.swing.Box.createVerticalStrut(5));
        f.setAlignmentX(LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, f.getPreferredSize().height));
        p.add(f);
        return p;
    }

    private static JPanel left(java.awt.Component c) {
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.add(c);
        return p;
    }
}
