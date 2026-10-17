package com.wordchain.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Compact QWERTY keyboard drawn on a canvas, so letter input never fights the system keyboard. */
final class KeyboardView extends View {
    interface Listener {
        void onLetter(char c);
        void onBackspace();
    }

    private static final String[] ROWS = {"QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"};

    private static final class Key {
        final RectF bounds = new RectF();
        final char letter;      // 0 = backspace

        Key(char letter) { this.letter = letter; }
    }

    private final List<Key> keys = new ArrayList<>();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int gap, keyHeight;
    private Key pressed;
    private Listener listener;

    KeyboardView(Context c) {
        super(c);
        gap = Ui.dp(c, 6);
        keyHeight = Ui.dp(c, 46);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(Ui.dp(c, 18));
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Ui.dp(c, 2));
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        setEnabled(false);
    }

    void setListener(Listener l) { listener = l; }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), 3 * keyHeight + 2 * gap + Ui.dp(getContext(), 8));
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        keys.clear();
        float keyW = (w - 2 * gap - 9 * gap) / 10f;
        for (int row = 0; row < 3; row++) {
            String letters = ROWS[row];
            boolean last = row == 2;
            float backW = keyW * 1.5f;
            float total = letters.length() * keyW + (letters.length() - 1) * gap + (last ? gap + backW : 0);
            float x = (w - total) / 2f;
            float y = row * (keyHeight + gap) + Ui.dp(getContext(), 4);
            for (int i = 0; i < letters.length(); i++) {
                Key k = new Key(letters.charAt(i));
                k.bounds.set(x, y, x + keyW, y + keyHeight);
                keys.add(k);
                x += keyW + gap;
            }
            if (last) {
                Key back = new Key((char) 0);
                back.bounds.set(x, y, x + backW, y + keyHeight);
                keys.add(back);
            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = hit(e.getX(), e.getY());
                if (pressed != null) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (listener != null) {
                        if (pressed.letter == 0) listener.onBackspace();
                        else listener.onLetter(pressed.letter);
                    }
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                pressed = null;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    private Key hit(float x, float y) {
        Key best = null;
        float bestDist = Float.MAX_VALUE;
        for (Key k : keys) {
            float dx = Math.max(Math.max(k.bounds.left - x, x - k.bounds.right), 0);
            float dy = Math.max(Math.max(k.bounds.top - y, y - k.bounds.bottom), 0);
            float d = dx * dx + dy * dy;
            if (d < bestDist) { bestDist = d; best = k; }
        }
        return bestDist <= gap * gap ? best : null;
    }

    @Override protected void onDraw(Canvas canvas) {
        float radius = Ui.dp(getContext(), 9);
        for (Key k : keys) {
            int base = k.letter == 0 ? Ui.mix(Ui.SURFACE_ALT, Ui.DANGER, 0.18f) : Ui.SURFACE_ALT;
            if (k == pressed) base = Ui.ACCENT;
            if (!isEnabled()) base = Ui.mix(base, Ui.BG, 0.5f);
            fill.setColor(base);
            canvas.drawRoundRect(k.bounds, radius, radius, fill);
            int ink = isEnabled() ? Ui.TEXT : Ui.MUTED;
            if (k.letter != 0) {
                text.setColor(ink);
                float baseline = k.bounds.centerY() - (text.ascent() + text.descent()) / 2f;
                canvas.drawText(String.valueOf(k.letter), k.bounds.centerX(), baseline, text);
            } else {
                drawBackspace(canvas, k.bounds, ink);
            }
        }
    }

    private void drawBackspace(Canvas canvas, RectF b, int color) {
        float cx = b.centerX(), cy = b.centerY(), w = b.width() * 0.26f, h = keyHeight * 0.2f;
        Path p = new Path();
        p.moveTo(cx - w, cy);
        p.lineTo(cx - w * 0.4f, cy - h);
        p.lineTo(cx + w, cy - h);
        p.lineTo(cx + w, cy + h);
        p.lineTo(cx - w * 0.4f, cy + h);
        p.close();
        line.setColor(color);
        canvas.drawPath(p, line);
        float s = h * 0.45f, mx = cx + w * 0.3f;
        canvas.drawLine(mx - s, cy - s, mx + s, cy + s, line);
        canvas.drawLine(mx - s, cy + s, mx + s, cy - s, line);
    }
}
