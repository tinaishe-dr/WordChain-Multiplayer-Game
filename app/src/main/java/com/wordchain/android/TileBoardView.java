package com.wordchain.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * Big letter tiles. The active player taps a tile (erasing it), picks a replacement on the
 * on-screen keyboard and submits. Owns its own shake / pop / hint animations.
 */
final class TileBoardView extends View {
    interface Listener {
        void onSubmit(int position, char letter);
        void onProblem(String message);
    }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint letters = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int gap, maxTile, pad;

    private String word = "";
    private boolean interactive;
    private int selected = -1, hintIndex = -1, popIndex = -1;
    private char pending;
    private long popStart, shakeStart, hintStart;
    private Listener listener;

    TileBoardView(Context c) {
        super(c);
        gap = Ui.dp(c, 8);
        maxTile = Ui.dp(c, 84);
        pad = Ui.dp(c, 12);
        stroke.setStyle(Paint.Style.STROKE);
        shadow.setColor(0x46000000);
        letters.setTypeface(Typeface.DEFAULT_BOLD);
        letters.setTextAlign(Paint.Align.CENTER);
    }

    void setListener(Listener l) { listener = l; }

    void setWord(String w) {
        word = w.toUpperCase();
        selected = -1;
        pending = 0;
        hintIndex = -1;
        requestLayout();
        invalidate();
    }

    void setInteractive(boolean on) {
        interactive = on;
        if (!on) { selected = -1; pending = 0; }
        invalidate();
    }

    void popTile(int index) { popIndex = index; popStart = System.nanoTime(); invalidate(); }

    void shake() { shakeStart = System.nanoTime(); invalidate(); }

    void showHint(int index) { hintIndex = index; hintStart = System.nanoTime(); invalidate(); }

    void typeLetter(char c) {
        if (!interactive || word.isEmpty()) return;
        if (selected < 0) {
            shake();
            problem("Tap the letter you want to replace first.");
            return;
        }
        pending = Character.toUpperCase(c);
        hintIndex = -1;
        invalidate();
    }

    void backspace() { pending = 0; invalidate(); }

    void submit() {
        if (!interactive) return;
        if (selected < 0) { shake(); problem("Tap the letter you want to replace first."); return; }
        if (pending == 0) { shake(); problem("Pick the new letter on the keyboard."); return; }
        if (pending == word.charAt(selected)) { shake(); problem("Pick a different letter."); return; }
        if (listener != null) listener.onSubmit(selected, pending);
    }

    private void problem(String text) { if (listener != null) listener.onProblem(text); }

    private int tileSize(int width) {
        int n = Math.max(1, word.length());
        return Math.min(maxTile, (width - 2 * pad - gap * (n - 1)) / n);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, tileSize(w) + Ui.dp(getContext(), 30));
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (!interactive || e.getActionMasked() != MotionEvent.ACTION_DOWN) return interactive;
        int size = tileSize(getWidth());
        int n = word.length();
        float x0 = (getWidth() - (n * size + (n - 1) * gap)) / 2f;
        for (int i = 0; i < n; i++) {
            float x = x0 + i * (size + gap);
            if (e.getX() >= x - gap / 2f && e.getX() <= x + size + gap / 2f) {
                selected = i;
                pending = 0;
                hintIndex = -1;
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                invalidate();
                return true;
            }
        }
        return true;
    }

    @Override protected void onDraw(Canvas canvas) {
        if (word.isEmpty()) return;
        int size = tileSize(getWidth());
        int n = word.length();
        float x0 = (getWidth() - (n * size + (n - 1) * gap)) / 2f;
        float y0 = (getHeight() - size) / 2f;
        long now = System.nanoTime();
        float shakeX = shakeOffset(now);
        boolean blink = (now / 450_000_000L) % 2 == 0;
        float pulse = (float) (0.5 + 0.5 * Math.sin((now - hintStart) / 1.8e8));
        boolean animating = shakeX != 0 || popIndex >= 0 || (interactive && selected >= 0) || hintIndex >= 0;

        for (int i = 0; i < n; i++) {
            float scale = i == popIndex ? popScale(now) : 1f;
            float s = size * scale;
            float x = x0 + i * (size + gap) + shakeX - (s - size) / 2f;
            float y = y0 - (s - size) / 2f;
            boolean sel = i == selected;
            boolean editing = sel && pending != 0;
            float r = s * 0.2f;

            rect.set(x, y + Ui.dp(getContext(), 3), x + s, y + s + Ui.dp(getContext(), 3));
            canvas.drawRoundRect(rect, r, r, shadow);
            rect.set(x, y, x + s, y + s);
            fill.setColor(editing ? Ui.ACCENT : sel ? Ui.SURFACE : Ui.SURFACE_ALT);
            canvas.drawRoundRect(rect, r, r, fill);
            stroke.setStrokeWidth(Ui.dp(getContext(), sel || i == hintIndex ? 2.5f : 1.2f));
            stroke.setColor(sel ? Ui.ACCENT_2 : i == hintIndex ? Ui.mix(Ui.WARN, Ui.SURFACE_ALT, 1 - pulse) : Ui.BORDER);
            canvas.drawRoundRect(rect, r, r, stroke);

            letters.setTextSize(s * 0.52f);
            float baseline = y + s / 2f - (letters.ascent() + letters.descent()) / 2f;
            if (sel && pending == 0) {
                letters.setColor(Ui.alpha(Ui.MUTED, 90));
                canvas.drawText(String.valueOf(word.charAt(i)), x + s / 2f, baseline - s * 0.08f, letters);
                if (blink) {
                    fill.setColor(Ui.ACCENT_2);
                    rect.set(x + s * 0.28f, y + s * 0.76f, x + s * 0.72f, y + s * 0.76f + Math.max(3, s * 0.05f));
                    canvas.drawRoundRect(rect, 4, 4, fill);
                }
            } else {
                letters.setColor(editing ? 0xFFFFFFFF : Ui.TEXT);
                canvas.drawText(String.valueOf(editing ? pending : word.charAt(i)), x + s / 2f, baseline, letters);
            }
        }
        if (animating) postInvalidateOnAnimation();
    }

    private float shakeOffset(long now) {
        double t = (now - shakeStart) / 1e9;
        return t > 0.45 || t < 0 ? 0f : (float) (Math.sin(t * 50) * Ui.dp(getContext(), 8) * (1 - t / 0.45));
    }

    private float popScale(long now) {
        double t = (now - popStart) / 1e9;
        if (t > 0.35) { popIndex = -1; return 1f; }
        return (float) (1.0 + 0.28 * Math.sin(t / 0.35 * Math.PI));
    }
}
