package wordchain.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.function.Consumer;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** Room chat: scrolling transcript plus a send box. Also shows system notices. */
public final class ChatPanel extends Widgets.RoundedPanel {
    private final JTextPane pane = new JTextPane();
    private final JTextField input = Widgets.textField(10);
    private final JScrollPane scroll;
    private Consumer<String> onSend = s -> { };

    public ChatPanel() {
        super(18, Theme.SURFACE);
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setFont(Theme.font(Font.PLAIN, 13f));
        pane.setForeground(Theme.TEXT);
        scroll = new JScrollPane(pane);
        Widgets.styleScroll(scroll);
        input.addActionListener(e -> send());
        Widgets.RoundedButton btn = new Widgets.RoundedButton("Send", Widgets.RoundedButton.Kind.PRIMARY);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.addActionListener(e -> send());
        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(btn, BorderLayout.EAST);
        add(Widgets.label("Chat", Theme.font(Font.BOLD, 14f), Theme.MUTED), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void onSend(Consumer<String> c) { onSend = c; }

    public boolean isTyping() { return input.isFocusOwner() && !input.getText().isEmpty(); }

    public void clear() { pane.setText(""); }

    public void addChat(String name, String text, boolean mine) {
        append(name + "  ", Theme.avatar(name), true);
        append(text + "\n", mine ? Theme.TEXT : new Color(0xC9D1EA), false);
    }

    public void addSystem(String text) { append("\u2022 " + text + "\n", Theme.MUTED, false); }

    private void append(String text, Color color, boolean bold) {
        StyledDocument doc = pane.getStyledDocument();
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, color);
        StyleConstants.setBold(a, bold);
        try {
            doc.insertString(doc.getLength(), text, a);
        } catch (BadLocationException ignored) { }
        pane.setCaretPosition(doc.getLength());
    }

    private void send() {
        String t = input.getText().trim();
        if (t.isEmpty()) return;
        input.setText("");
        onSend.accept(t);
    }
}
