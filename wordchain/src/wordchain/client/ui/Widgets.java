package wordchain.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Small reusable, custom-painted building blocks shared by every screen. */
public final class Widgets {
    private Widgets() {}

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static JTextField textField(int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(Theme.font(Font.PLAIN, 15f));
        f.setForeground(Theme.TEXT);
        f.setCaretColor(Theme.ACCENT_2);
        f.setBackground(Theme.BG);
        f.setSelectionColor(Theme.ACCENT);
        f.setSelectedTextColor(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(Theme.BORDER, 12), new EmptyBorder(8, 12, 8, 12)));
        return f;
    }

    public static void styleScroll(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(14);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        sp.getVerticalScrollBar().setOpaque(false);
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { thumbColor = Theme.BORDER; trackColor = Theme.SURFACE; }
            @Override protected JButton createDecreaseButton(int o) { return zero(); }
            @Override protected JButton createIncreaseButton(int o) { return zero(); }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.quality(g2);
                g2.setColor(Theme.BORDER);
                g2.fillRoundRect(r.x + 1, r.y, r.width - 2, r.height, 6, 6);
                g2.dispose();
            }
            private JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        });
    }

    /** Rounded 1px outline, used by text fields and cards. */
    public static final class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        public RoundedBorder(Color color, int radius) { this.color = color; this.radius = radius; }

        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.quality(g2);
            g2.setColor(c.isFocusOwner() ? Theme.ACCENT : color);
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
    }

    /** Filled rounded surface that other components can sit on. */
    public static class RoundedPanel extends JPanel {
        private final int radius;
        private Color fill;
        private Color outline;

        public RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
            setBorder(new EmptyBorder(14, 14, 14, 14));
        }

        public RoundedPanel outline(Color c) { outline = c; return this; }

        public void setFill(Color c) { fill = c; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.quality(g2);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class RoundedButton extends JButton {
        public enum Kind { PRIMARY, SECONDARY, DANGER, GHOST }

        private final Kind kind;
        private boolean hover;

        public RoundedButton(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Theme.TEXT);
            setFont(Theme.font(Font.BOLD, 14f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 20, 10, 20));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.quality(g2);
            Color base = switch (kind) {
                case PRIMARY -> Theme.ACCENT;
                case DANGER -> Theme.mix(Theme.DANGER, Theme.BG, 0.45f);
                case SECONDARY -> Theme.SURFACE_ALT;
                case GHOST -> Theme.alpha(Theme.SURFACE_ALT, 0);
            };
            if (!isEnabled()) base = Theme.mix(base, Theme.BG, 0.55f);
            else if (getModel().isPressed()) base = Theme.mix(base, Color.BLACK, 0.2f);
            else if (hover) base = Theme.mix(base, Color.WHITE, 0.12f);
            g2.setColor(base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            if (kind == Kind.SECONDARY || kind == Kind.GHOST) {
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Compact "[-] value [+]" control for bounded integers. */
    public static final class Stepper extends JPanel {
        private final int min, max, step;
        private final String suffix;
        private final JLabel value = label("", Theme.font(Font.BOLD, 15f), Theme.TEXT);
        private final RoundedButton minus = new RoundedButton("\u2212", RoundedButton.Kind.SECONDARY);
        private final RoundedButton plus = new RoundedButton("+", RoundedButton.Kind.SECONDARY);
        private int current;
        private IntConsumer onChange = v -> { };

        public Stepper(int min, int max, int step, String suffix) {
            super(new BorderLayout(8, 0));
            this.min = min;
            this.max = max;
            this.step = step;
            this.suffix = suffix;
            setOpaque(false);
            for (RoundedButton b : new RoundedButton[] {minus, plus}) b.setBorder(new EmptyBorder(4, 12, 4, 12));
            value.setHorizontalAlignment(JLabel.CENTER);
            value.setPreferredSize(new Dimension(64, 28));
            add(minus, BorderLayout.WEST);
            add(value, BorderLayout.CENTER);
            add(plus, BorderLayout.EAST);
            minus.addActionListener(e -> change(-step));
            plus.addActionListener(e -> change(step));
            set(min);
        }

        public void onChange(IntConsumer c) { onChange = c; }

        public int get() { return current; }

        /** Updates the shown value without firing the change callback. */
        public void set(int v) {
            current = Math.max(min, Math.min(max, v));
            value.setText(current + suffix);
            minus.setEnabled(isEnabled() && current > min);
            plus.setEnabled(isEnabled() && current < max);
        }

        private void change(int delta) {
            set(current + delta);
            onChange.accept(current);
        }

        @Override public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            set(current);
        }
    }

    public static JPanel row(int hgap, Component... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, 0));
        p.setOpaque(false);
        for (Component c : parts) p.add(c);
        return p;
    }
}
