package wordchain.client.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.Timer;

/** Countdown bar that shifts green -> amber -> red and pulses in the final seconds. */
public final class TimerBar extends JComponent {
    private long startMillis, endMillis;
    private boolean running;
    private String idleText = "";
    private final Timer ticker = new Timer(30, e -> repaint());

    public TimerBar() { setPreferredSize(new Dimension(400, 26)); }

    public void start(int seconds) {
        startMillis = System.currentTimeMillis();
        endMillis = startMillis + seconds * 1000L;
        running = true;
    }

    public void stop(String text) { running = false; idleText = text; repaint(); }

    private double remainingMillis() { return Math.max(0, endMillis - System.currentTimeMillis()); }

    @Override public void addNotify() { super.addNotify(); ticker.start(); }

    @Override public void removeNotify() { ticker.stop(); super.removeNotify(); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.quality(g2);
        int w = getWidth(), h = getHeight();
        g2.setColor(Theme.SURFACE_ALT);
        g2.fillRoundRect(0, 0, w, h, h, h);

        String text = idleText;
        if (running) {
            double total = Math.max(1, endMillis - startMillis), left = remainingMillis(), frac = left / total;
            var color = frac > 0.5 ? Theme.mix(Theme.WARN, Theme.GOOD, (float) ((frac - 0.5) * 2))
                    : Theme.mix(Theme.DANGER, Theme.WARN, (float) (frac * 2));
            if (left < 3000 && (System.currentTimeMillis() / 250) % 2 == 0) color = Theme.mix(color, java.awt.Color.WHITE, 0.25f);
            int fw = (int) Math.max(h, (w) * frac);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, fw, h, h, h);
            text = (int) Math.ceil(left / 1000.0) + "s";
        }
        g2.setFont(Theme.font(Font.BOLD, 13f));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(running ? Theme.BG : Theme.MUTED);
        g2.drawString(text, (w - fm.stringWidth(text)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
        g2.dispose();
    }
}
