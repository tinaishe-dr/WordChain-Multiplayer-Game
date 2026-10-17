package wordchain.client.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/** Second screen: create a room or join one with a 4-character code, plus a quick rules card. */
public final class MenuPanel extends JPanel {
    public interface Handler {
        void create();
        void join(String code);
        void disconnect();
    }

    private final JLabel info = Widgets.label(" ", Theme.font(Font.PLAIN, 13f), Theme.MUTED);
    private final JLabel serverInfo = Widgets.label(" ", Theme.font(Font.PLAIN, 13f), Theme.ACCENT_2);
    private final JTextField code = Widgets.textField(6);

    public MenuPanel(Handler handler) {
        super(new GridBagLayout());
        setBackground(Theme.BG);

        Widgets.RoundedPanel card = new Widgets.RoundedPanel(26, Theme.SURFACE).outline(Theme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(30, 36, 30, 36));
        card.setPreferredSize(new Dimension(500, 620));

        card.add(left(Widgets.label("Ready to play?", Theme.font(Font.BOLD, 28f), Theme.TEXT)));
        card.add(left(info));
        card.add(left(serverInfo));
        card.add(javax.swing.Box.createVerticalStrut(22));

        Widgets.RoundedButton create = new Widgets.RoundedButton("Create a room", Widgets.RoundedButton.Kind.PRIMARY);
        create.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        create.setAlignmentX(LEFT_ALIGNMENT);
        create.addActionListener(e -> handler.create());
        card.add(create);
        card.add(javax.swing.Box.createVerticalStrut(18));

        card.add(left(Widgets.label("or join with a room code", Theme.font(Font.BOLD, 12f), Theme.MUTED)));
        card.add(javax.swing.Box.createVerticalStrut(6));
        code.setDocument(new PlainDocument() {
            @Override public void insertString(int off, String s, AttributeSet a) throws BadLocationException {
                String up = s.toUpperCase().replaceAll("[^A-Z0-9]", "");
                if (getLength() + up.length() <= 4) super.insertString(off, up, a);
            }
        });
        code.setFont(Theme.font(Font.BOLD, 22f));
        Widgets.RoundedButton join = new Widgets.RoundedButton("Join", Widgets.RoundedButton.Kind.SECONDARY);
        java.awt.event.ActionListener doJoin = e -> handler.join(code.getText());
        join.addActionListener(doJoin);
        code.addActionListener(doJoin);
        JPanel joinRow = new JPanel(new java.awt.BorderLayout(10, 0));
        joinRow.setOpaque(false);
        joinRow.setAlignmentX(LEFT_ALIGNMENT);
        joinRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        joinRow.add(code, java.awt.BorderLayout.CENTER);
        joinRow.add(join, java.awt.BorderLayout.EAST);
        card.add(joinRow);
        card.add(javax.swing.Box.createVerticalStrut(26));

        Widgets.RoundedPanel rules = new Widgets.RoundedPanel(18, Theme.SURFACE_ALT);
        rules.setLayout(new BoxLayout(rules, BoxLayout.Y_AXIS));
        rules.setAlignmentX(LEFT_ALIGNMENT);
        rules.add(left(Widgets.label("How to play", Theme.font(Font.BOLD, 14f), Theme.TEXT)));
        rules.add(javax.swing.Box.createVerticalStrut(6));
        String[] lines = {
            "Change exactly one letter to make a new real word (TALK \u2192 WALK).",
            "Click a tile, type the new letter, press Enter.",
            "Run out of time and you lose a life. Lose them all and you're out.",
            "Faster answers score more, and the clock speeds up as the chain grows.",
            "Trap the next player in a dead end for a bonus. Each player gets 2 hints.",
        };
        for (String s : lines) {
            JLabel l = Widgets.label("<html><div style='width:310px'>\u2022 " + s + "</div></html>",
                    Theme.font(Font.PLAIN, 13f), Theme.MUTED);
            rules.add(left(l));
            rules.add(javax.swing.Box.createVerticalStrut(4));
        }
        card.add(rules);
        card.add(javax.swing.Box.createVerticalGlue());

        Widgets.RoundedButton out = new Widgets.RoundedButton("Disconnect", Widgets.RoundedButton.Kind.GHOST);
        out.setAlignmentX(LEFT_ALIGNMENT);
        out.addActionListener(e -> handler.disconnect());
        card.add(out);
        add(card);
    }

    public void setInfo(String text, String serverText) {
        info.setText(text);
        serverInfo.setText(serverText.isEmpty() ? " " : serverText);
        code.setText("");
    }

    private static JPanel left(java.awt.Component c) {
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.add(c);
        return p;
    }
}
