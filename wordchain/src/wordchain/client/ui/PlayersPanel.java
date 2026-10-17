package wordchain.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import wordchain.client.RoomState;

/** Vertical stack of player cards: avatar, name, lives and score; highlights whose turn it is. */
public final class PlayersPanel extends JPanel {
    private final boolean showStats;

    public PlayersPanel(boolean showStats) {
        this.showStats = showStats;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void update(RoomState room, String me, String active) {
        removeAll();
        for (RoomState.Player p : room.players()) {
            add(new Card(p, p.name().equals(me), p.name().equals(room.host()), p.name().equals(active), room.lives()));
            add(javax.swing.Box.createVerticalStrut(8));
        }
        revalidate();
        repaint();
    }

    private final class Card extends JComponent {
        private final RoomState.Player p;
        private final boolean me, host, active;
        private final int maxLives;

        Card(RoomState.Player p, boolean me, boolean host, boolean active, int maxLives) {
            this.p = p;
            this.me = me;
            this.host = host;
            this.active = active;
            this.maxLives = maxLives;
            setPreferredSize(new Dimension(200, 58));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            setMinimumSize(new Dimension(120, 58));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.quality(g2);
            int w = getWidth(), h = getHeight();
            boolean out = showStats && !p.alive();
            g2.setColor(active ? Theme.mix(Theme.SURFACE_ALT, Theme.ACCENT, 0.25f) : Theme.SURFACE_ALT);
            g2.fillRoundRect(0, 0, w, h, 16, 16);
            g2.setColor(active ? Theme.ACCENT_2 : Theme.BORDER);
            g2.setStroke(new java.awt.BasicStroke(active ? 2.5f : 1f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);

            Color av = out ? Theme.MUTED : Theme.avatar(p.name());
            g2.setColor(av);
            g2.fillOval(12, 11, 36, 36);
            g2.setColor(Theme.BG);
            g2.setFont(Theme.font(Font.BOLD, 17f));
            String initial = p.name().substring(0, 1).toUpperCase();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initial, 30 - fm.stringWidth(initial) / 2, 11 + (36 + fm.getAscent() - fm.getDescent()) / 2);

            g2.setFont(Theme.font(Font.BOLD, 14f));
            g2.setColor(out ? Theme.MUTED : Theme.TEXT);
            String name = p.name() + (me ? " (you)" : "") + (host ? "  \u2605" : "");
            g2.drawString(name, 58, showStats ? 26 : 35);

            if (showStats) {
                if (out) {
                    g2.setFont(Theme.font(Font.BOLD, 12f));
                    g2.setColor(Theme.DANGER);
                    g2.drawString("OUT", 58, 45);
                } else {
                    for (int i = 0; i < maxLives; i++) heart(g2, 58 + i * 17, 34, 13, i < p.lives());
                }
                g2.setFont(Theme.font(Font.BOLD, 15f));
                String score = p.score() + " pts";
                FontMetrics sm = g2.getFontMetrics();
                g2.setColor(out ? Theme.MUTED : Theme.ACCENT_2);
                g2.drawString(score, w - sm.stringWidth(score) - 14, h / 2 + 5);
            }
            g2.dispose();
        }

        private void heart(Graphics2D g2, double x, double y, double s, boolean full) {
            Path2D h = new Path2D.Double();
            h.moveTo(x + s / 2, y + s);
            h.curveTo(x - s * 0.25, y + s * 0.45, x + s * 0.1, y - s * 0.2, x + s / 2, y + s * 0.3);
            h.curveTo(x + s * 0.9, y - s * 0.2, x + s * 1.25, y + s * 0.45, x + s / 2, y + s);
            g2.setColor(full ? Theme.DANGER : Theme.alpha(Theme.MUTED, 90));
            g2.fill(h);
        }
    }
}
