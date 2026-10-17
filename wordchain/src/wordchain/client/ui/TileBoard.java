package wordchain.client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Big animated letter tiles. The active player clicks a tile (erasing that letter),
 * types a replacement and presses Enter. Handles shake/pop/hint animations itself.
 */
public final class TileBoard extends JComponent {
    public interface Listener {
        void onSubmit(int position, char letter);
        void onProblem(String message);
    }

    private static final int GAP = 12, MAX_TILE = 112;

    private String word = "";
    private boolean interactive;
    private int selected = -1, hovered = -1, hintIndex = -1, popIndex = -1;
    private char pending;
    private long popStart, shakeStart, hintStart;
    private Listener listener = new Listener() {
        public void onSubmit(int p, char c) { }
        public void onProblem(String m) { }
    };
    private final Timer ticker = new Timer(16, e -> repaint());

    public TileBoard() {
        setFocusable(true);
        setPreferredSize(new Dimension(520, 160));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int i = indexAt(e.getX(), e.getY());
                if (interactive && i >= 0) { selected = i; pending = 0; hintIndex = -1; repaint(); }
            }
            @Override public void mouseExited(MouseEvent e) { hovered = -1; setCursor(Cursor.getDefaultCursor()); }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                hovered = interactive ? indexAt(e.getX(), e.getY()) : -1;
                setCursor(hovered >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKey(e); }
        });
    }

    public void setListener(Listener l) { listener = l; }

    public void setWord(String w) {
        word = w.toUpperCase();
        clearSelection();
    }

    public void setInteractive(boolean on) {
        interactive = on;
        if (!on) clearSelection();
        else requestFocusInWindow();
        repaint();
    }

    public void clearSelection() { selected = -1; pending = 0; repaint(); }

    public void popTile(int index) { popIndex = index; popStart = System.nanoTime(); }

    public void shake() { shakeStart = System.nanoTime(); }

    public void showHint(int index) { hintIndex = index; hintStart = System.nanoTime(); repaint(); }

    /** Submits the current edit, or reports what is missing. */
    public void submit() {
        if (!interactive) return;
        if (selected < 0) { shake(); listener.onProblem("Click the letter you want to replace first."); return; }
        if (pending == 0) { shake(); listener.onProblem("Type the new letter, then press Enter."); return; }
        if (pending == word.charAt(selected)) { shake(); listener.onProblem("Pick a different letter."); return; }
        listener.onSubmit(selected, pending);
    }

    private void onKey(KeyEvent e) {
        if (!interactive || word.isEmpty()) return;
        int code = e.getKeyCode();
        char ch = e.getKeyChar();
        if (code == KeyEvent.VK_ENTER) submit();
        else if (code == KeyEvent.VK_ESCAPE) clearSelection();
        else if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) { pending = 0; repaint(); }
        else if (code == KeyEvent.VK_LEFT) move(-1);
        else if (code == KeyEvent.VK_RIGHT) move(1);
        else if (Character.isLetter(ch) && ch < 128) {
            if (selected < 0) { listener.onProblem("Click a tile first to choose which letter to erase."); shake(); return; }
            pending = Character.toUpperCase(ch);
            hintIndex = -1;
        }
        repaint();
    }

    private void move(int delta) {
        int start = selected < 0 ? (delta > 0 ? -1 : word.length()) : selected;
        selected = Math.max(0, Math.min(word.length() - 1, start + delta));
        pending = 0;
    }

    // ------------------------------------------------------------ painting

    private int tileSize() {
        int n = Math.max(1, word.length());
        return Math.max(40, Math.min(MAX_TILE, Math.min((getWidth() - 24 - GAP * (n - 1)) / n, getHeight() - 24)));
    }

    private int startX(int size) {
        int n = word.length();
        return (getWidth() - (n * size + (n - 1) * GAP)) / 2;
    }

    private int indexAt(int mx, int my) {
        if (word.isEmpty()) return -1;
        int size = tileSize(), x0 = startX(size), y0 = (getHeight() - size) / 2;
        if (my < y0 || my > y0 + size) return -1;
        for (int i = 0; i < word.length(); i++) {
            int x = x0 + i * (size + GAP);
            if (mx >= x && mx <= x + size) return i;
        }
        return -1;
    }

    @Override public void addNotify() { super.addNotify(); ticker.start(); }

    @Override public void removeNotify() { ticker.stop(); super.removeNotify(); }

    @Override protected void paintComponent(Graphics g) {
        if (word.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.quality(g2);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        int size = tileSize(), x0 = startX(size), y0 = (getHeight() - size) / 2;
        double shake = shakeOffset();
        long now = System.nanoTime();
        boolean blink = (now / 450_000_000L) % 2 == 0;
        double pulse = 0.5 + 0.5 * Math.sin((now - hintStart) / 1.8e8);

        for (int i = 0; i < word.length(); i++) {
            double scale = i == popIndex ? popScale() : 1.0;
            double s = size * scale;
            double x = x0 + i * (size + GAP) + shake - (s - size) / 2;
            double y = y0 - (s - size) / 2 - (i == hovered && i != selected ? 4 : 0);
            boolean sel = i == selected;
            boolean editing = sel && pending != 0;

            Color fill = editing ? Theme.ACCENT : sel ? Theme.SURFACE : Theme.SURFACE_ALT;
            Color edge = sel ? Theme.ACCENT_2 : i == hintIndex ? Theme.mix(Theme.WARN, Theme.SURFACE_ALT, (float) (1 - pulse)) : Theme.BORDER;
            RoundRectangle2D rr = new RoundRectangle2D.Double(x, y, s, s, s * 0.2, s * 0.2);

            g2.setColor(Theme.alpha(Color.BLACK, 70));
            g2.fill(new RoundRectangle2D.Double(x, y + 6, s, s, s * 0.2, s * 0.2));
            g2.setColor(fill);
            g2.fill(rr);
            g2.setStroke(new BasicStroke(sel || i == hintIndex ? 3f : 1.5f));
            g2.setColor(edge);
            g2.draw(rr);

            g2.setFont(Theme.font(Font.BOLD, (float) (s * 0.52)));
            FontMetrics fm = g2.getFontMetrics();
            if (sel && pending == 0) {
                if (blink) {
                    g2.setColor(Theme.ACCENT_2);
                    g2.fill(new RoundRectangle2D.Double(x + s * 0.28, y + s * 0.74, s * 0.44, Math.max(3, s * 0.05), 4, 4));
                }
                g2.setColor(Theme.alpha(Theme.MUTED, 90));
                String ghost = String.valueOf(word.charAt(i));
                g2.drawString(ghost, (float) (x + (s - fm.stringWidth(ghost)) / 2), (float) (y + (s + fm.getAscent() - fm.getDescent()) / 2 - s * 0.08));
            } else {
                String t = String.valueOf(editing ? pending : word.charAt(i));
                g2.setColor(editing ? Color.WHITE : Theme.TEXT);
                g2.drawString(t, (float) (x + (s - fm.stringWidth(t)) / 2), (float) (y + (s + fm.getAscent() - fm.getDescent()) / 2));
            }
        }
        g2.dispose();
    }

    private double shakeOffset() {
        double t = (System.nanoTime() - shakeStart) / 1e9;
        return t > 0.45 ? 0 : Math.sin(t * 50) * 12 * (1 - t / 0.45);
    }

    private double popScale() {
        double t = (System.nanoTime() - popStart) / 1e9;
        if (t > 0.35) { popIndex = -1; return 1.0; }
        return 1.0 + 0.28 * Math.sin(t / 0.35 * Math.PI);
    }
}
