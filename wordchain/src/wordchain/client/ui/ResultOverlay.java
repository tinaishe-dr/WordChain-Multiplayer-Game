package wordchain.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.util.List;
import java.util.Random;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/** Full-window dim layer with the final standings and a little confetti for everyone. */
public final class ResultOverlay extends JPanel {
    public record Standing(String name, int score, boolean survived) {}

    private static final class Piece {
        double x, y, vy, vx, size;
        Color color;
    }

    private final Piece[] confetti = new Piece[90];
    private final Random rnd = new Random();
    private final Timer ticker = new Timer(20, e -> step());
    private final JPanel body = new JPanel();
    private Runnable onBack = () -> { };

    public ResultOverlay() {
        super(new GridBagLayout());
        setOpaque(false);
        addMouseListener(new MouseAdapter() { });
        addMouseMotionListener(new MouseAdapter() { });

        Widgets.RoundedPanel card = new Widgets.RoundedPanel(26, Theme.SURFACE).outline(Theme.ACCENT);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(28, 36, 28, 36));
        card.setPreferredSize(new Dimension(440, 440));
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        card.add(body, BorderLayout.CENTER);
        add(card);
    }

    public void show(String winner, String me, List<Standing> standings, Runnable back) {
        onBack = back;
        body.removeAll();
        boolean won = winner.equals(me);
        JLabel title = Widgets.label(winner.isEmpty() ? "Game over" : won ? "You win! \uD83C\uDFC6" : winner + " wins!",
                Theme.font(Font.BOLD, 30f), won ? Theme.GOOD : Theme.TEXT);
        title.setAlignmentX(CENTER_ALIGNMENT);
        body.add(title);
        JLabel sub = Widgets.label("Final standings", Theme.font(Font.PLAIN, 14f), Theme.MUTED);
        sub.setAlignmentX(CENTER_ALIGNMENT);
        body.add(sub);
        body.add(javax.swing.Box.createVerticalStrut(18));

        int rank = 1;
        for (Standing s : standings) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            JLabel name = Widgets.label(rank++ + ".  " + s.name() + (s.name().equals(me) ? " (you)" : "")
                    + (s.survived() ? "  \u2605" : ""), Theme.font(Font.BOLD, 16f), s.survived() ? Theme.WARN : Theme.TEXT);
            JLabel pts = Widgets.label(s.score() + " pts", Theme.font(Font.BOLD, 16f), Theme.ACCENT_2);
            row.add(name, BorderLayout.WEST);
            row.add(pts, BorderLayout.EAST);
            body.add(row);
        }
        body.add(javax.swing.Box.createVerticalGlue());
        Widgets.RoundedButton back1 = new Widgets.RoundedButton("Back to lobby", Widgets.RoundedButton.Kind.PRIMARY);
        back1.setAlignmentX(CENTER_ALIGNMENT);
        back1.addActionListener(e -> onBack.run());
        body.add(back1);

        for (int i = 0; i < confetti.length; i++) confetti[i] = spawn(true);
        setVisible(true);
        ticker.start();
        revalidate();
    }

    public void dismiss() {
        ticker.stop();
        setVisible(false);
    }

    private Piece spawn(boolean anywhere) {
        Piece p = new Piece();
        p.x = rnd.nextDouble() * Math.max(1, getWidth());
        p.y = anywhere ? -rnd.nextDouble() * Math.max(400, getHeight()) : -10;
        p.vy = 2 + rnd.nextDouble() * 3;
        p.vx = rnd.nextDouble() * 2 - 1;
        p.size = 5 + rnd.nextDouble() * 6;
        p.color = Color.getHSBColor(rnd.nextFloat(), 0.7f, 1f);
        return p;
    }

    private void step() {
        for (int i = 0; i < confetti.length; i++) {
            Piece p = confetti[i];
            if (p == null) continue;
            p.y += p.vy;
            p.x += p.vx;
            if (p.y > getHeight()) confetti[i] = spawn(false);
        }
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.quality(g2);
        g2.setColor(Theme.alpha(Theme.BG, 215));
        g2.fillRect(0, 0, getWidth(), getHeight());
        for (Piece p : confetti) {
            if (p == null) continue;
            g2.setColor(p.color);
            g2.fillRoundRect((int) p.x, (int) p.y, (int) p.size, (int) (p.size * 1.6), 3, 3);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
