package com.wordchain.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** Countdown bar that shifts green -> amber -> red and flashes in the last seconds. */
final class TimerBarView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private long startMillis, endMillis;
    private boolean running;
    private String idleText = "";

    TimerBarView(Context c) {
        super(c);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(Ui.dp(c, 13));
    }

    void start(int seconds) {
        startMillis = System.currentTimeMillis();
        endMillis = startMillis + seconds * 1000L;
        running = true;
        invalidate();
    }

    void stop(String text) { running = false; idleText = text; invalidate(); }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), Ui.dp(getContext(), 24));
    }

    @Override protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        paint.setColor(Ui.SURFACE_ALT);
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, h / 2f, h / 2f, paint);
        String text = idleText;
        if (running) {
            long now = System.currentTimeMillis();
            double total = Math.max(1, endMillis - startMillis);
            double left = Math.max(0, endMillis - now);
            double frac = left / total;
            int color = frac > 0.5 ? Ui.mix(Ui.WARN, Ui.GOOD, (float) ((frac - 0.5) * 2))
                    : Ui.mix(Ui.DANGER, Ui.WARN, (float) (frac * 2));
            if (left < 3000 && (now / 250) % 2 == 0) color = Ui.mix(color, 0xFFFFFFFF, 0.25f);
            paint.setColor(color);
            rect.set(0, 0, (float) Math.max(h, w * frac), h);
            canvas.drawRoundRect(rect, h / 2f, h / 2f, paint);
            text = (int) Math.ceil(left / 1000.0) + "s";
            label.setColor(Ui.BG);
            postInvalidateOnAnimation();
        } else {
            label.setColor(Ui.MUTED);
        }
        canvas.drawText(text, w / 2f, h / 2f - (label.ascent() + label.descent()) / 2f, label);
    }
}
