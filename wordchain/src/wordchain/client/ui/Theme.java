package wordchain.client.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.UIManager;

/** Central palette, typography and rendering helpers for the dark UI. */
public final class Theme {
    public static final Color BG = new Color(0x0B1020);
    public static final Color SURFACE = new Color(0x151B30);
    public static final Color SURFACE_ALT = new Color(0x1E2642);
    public static final Color BORDER = new Color(0x2C3560);
    public static final Color TEXT = new Color(0xE8ECF8);
    public static final Color MUTED = new Color(0x8F9ABB);
    public static final Color ACCENT = new Color(0x7C5CFF);
    public static final Color ACCENT_2 = new Color(0x22D3EE);
    public static final Color GOOD = new Color(0x34D399);
    public static final Color WARN = new Color(0xFBBF24);
    public static final Color DANGER = new Color(0xF87171);

    private static final String FAMILY = pickFamily();

    private Theme() {}

    public static Font font(int style, float size) { return new Font(FAMILY, style, 1).deriveFont(style, size); }

    public static void install() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) { }
        UIManager.put("Button.disabledText", MUTED);
        UIManager.put("ToolTip.background", SURFACE_ALT);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", javax.swing.BorderFactory.createLineBorder(BORDER));
    }

    public static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    public static Color mix(Color a, Color b, float t) {
        float u = Math.max(0f, Math.min(1f, t));
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * u),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * u),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * u));
    }

    /** Stable, pleasant colour per player name. */
    public static Color avatar(String name) {
        float hue = (Math.abs(name.hashCode()) % 360) / 360f;
        return Color.getHSBColor(hue, 0.55f, 0.95f);
    }

    private static String pickFamily() {
        Set<String> have = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[] {"Inter", "SF Pro Text", "Segoe UI", "Helvetica Neue", "Roboto", "Noto Sans", "DejaVu Sans"}) {
            if (have.contains(f)) return f;
        }
        return Font.SANS_SERIF;
    }
}
