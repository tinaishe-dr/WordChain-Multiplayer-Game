package wordchain.client.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;


/** Newest-first log of every word in the chain, with the changed letter highlighted. */
public final class HistoryPanel extends Widgets.RoundedPanel {
    private final DefaultListModel<String> model = new DefaultListModel<>();

    public HistoryPanel() {
        super(18, Theme.SURFACE);
        setLayout(new BorderLayout(0, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        JList<String> list = new JList<>(model);
        list.setOpaque(false);
        list.setFocusable(false);
        list.setCellRenderer(renderer());
        JScrollPane sp = new JScrollPane(list);
        Widgets.styleScroll(sp);
        add(Widgets.label("Word chain", Theme.font(Font.BOLD, 14f), Theme.MUTED), BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
    }

    public void clear() { model.clear(); }

    public void addStart(String word) {
        model.add(0, "<html><span style='color:#8F9ABB'>Start &nbsp;</span><b>" + word.toUpperCase() + "</b></html>");
    }

    public void addMove(String player, String oldWord, String newWord, int pos, int points) {
        StringBuilder w = new StringBuilder();
        for (int i = 0; i < newWord.length(); i++) {
            String ch = String.valueOf(newWord.charAt(i)).toUpperCase();
            w.append(i == pos ? "<span style='color:#22D3EE'>" + ch + "</span>" : ch);
        }
        model.add(0, "<html><b>" + w + "</b> &nbsp;<span style='color:#8F9ABB'>" + player + "</span> "
                + "<span style='color:#34D399'>+" + points + "</span></html>");
    }

    public void addNote(String html) { model.add(0, "<html>" + html + "</html>"); }

    private static ListCellRenderer<String> renderer() {
        return (JList<? extends String> l, String v, int i, boolean sel, boolean focus) -> {
            JLabel c = new JLabel(v);
            c.setFont(Theme.font(Font.PLAIN, 13f));
            c.setForeground(Theme.TEXT);
            c.setBorder(new EmptyBorder(4, 2, 4, 2));
            return (Component) c;
        };
    }
}
