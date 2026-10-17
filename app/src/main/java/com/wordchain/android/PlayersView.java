package com.wordchain.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Player cards. In-game ({@code stats = true}) they form a compact horizontal strip with lives and
 * score; in the lobby they are a vertical list of names.
 */
final class PlayersView extends LinearLayout {
    private final boolean stats;

    PlayersView(Context c, boolean stats) {
        super(c);
        this.stats = stats;
        setOrientation(stats ? HORIZONTAL : VERTICAL);
    }

    void update(RoomState room, String me, String active) {
        removeAllViews();
        Context c = getContext();
        int count = room.players.size();
        for (int i = 0; i < count; i++) {
            RoomState.Player p = room.players.get(i);
            Chip chip = new Chip(c, p, p.name.equals(me), p.name.equals(room.host), p.name.equals(active), room.lives);
            LayoutParams lp = stats
                    ? new LayoutParams(0, Ui.dp(c, 74), 1f)
                    : new LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 54));
            if (stats) lp.setMargins(i == 0 ? 0 : Ui.dp(c, 4), 0, i == count - 1 ? 0 : Ui.dp(c, 4), 0);
            else lp.setMargins(0, 0, 0, Ui.dp(c, 8));
            addView(chip, lp);
        }
    }

    private final class Chip extends View {
        private final RoomState.Player p;
        private final boolean me, host, active;
        private final int maxLives;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        Chip(Context c, RoomState.Player p, boolean me, boolean host, boolean active, int maxLives) {
            super(c);
            this.p = p;
            this.me = me;
            this.host = host;
            this.active = active;
            this.maxLives = maxLives;
            text.setTypeface(Typeface.DEFAULT_BOLD);
        }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), d = Ui.dp(getContext(), 1);
            boolean out = stats && !p.alive();
            float radius = Ui.dp(getContext(), 14);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(active ? Ui.mix(Ui.SURFACE_ALT, Ui.ACCENT, 0.3f) : Ui.SURFACE_ALT);
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(active ? 2.5f * d : d);
            paint.setColor(active ? Ui.ACCENT_2 : Ui.BORDER);
            rect.inset(d, d);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            if (stats) drawStats(canvas, w, h, out); else drawRow(canvas, w, h);
        }

        private void drawStats(Canvas canvas, float w, float h, boolean out) {
            float d = Ui.dp(getContext(), 1);
            paint.setColor(out ? Ui.MUTED : Ui.avatar(p.name));
            rect.set(w * 0.3f, 8 * d, w * 0.7f, 11 * d);
            canvas.drawRoundRect(rect, 2 * d, 2 * d, paint);

            text.setTextSize(Ui.dp(getContext(), 13));
            text.setColor(out ? Ui.MUTED : Ui.TEXT);
            text.setTextAlign(Paint.Align.CENTER);
            CharSequence name = TextUtils.ellipsize(me ? "You" : p.name, text, w - 10 * d, TextUtils.TruncateAt.END);
            canvas.drawText(name.toString(), w / 2f, 29 * d, text);

            if (out) {
                text.setTextSize(Ui.dp(getContext(), 11));
                text.setColor(Ui.DANGER);
                canvas.drawText("OUT", w / 2f, 46 * d, text);
            } else {
                float size = 11 * d, gapPx = 3 * d;
                float total = maxLives * size + (maxLives - 1) * gapPx;
                float x = (w - total) / 2f;
                for (int i = 0; i < maxLives; i++) { heart(canvas, x + i * (size + gapPx), 35 * d, size, i < p.lives); }
            }
            text.setTextSize(Ui.dp(getContext(), 12));
            text.setColor(out ? Ui.MUTED : Ui.ACCENT_2);
            canvas.drawText(p.score + " pts", w / 2f, 66 * d, text);
        }

        private void drawRow(Canvas canvas, float w, float h) {
            float d = Ui.dp(getContext(), 1);
            paint.setColor(Ui.avatar(p.name));
            canvas.drawCircle(30 * d, h / 2f, 17 * d, paint);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(Ui.dp(getContext(), 16));
            text.setColor(Ui.BG);
            canvas.drawText(p.name.substring(0, 1).toUpperCase(), 30 * d, h / 2f - (text.ascent() + text.descent()) / 2f, text);
            text.setTextAlign(Paint.Align.LEFT);
            text.setTextSize(Ui.dp(getContext(), 15));
            text.setColor(Ui.TEXT);
            String label = p.name + (me ? " (you)" : "") + (host ? "  \u2605 host" : "");
            canvas.drawText(TextUtils.ellipsize(label, text, w - 64 * d, TextUtils.TruncateAt.END).toString(),
                    56 * d, h / 2f - (text.ascent() + text.descent()) / 2f, text);
        }

        private void heart(Canvas canvas, float x, float y, float s, boolean full) {
            Path h = new Path();
            h.moveTo(x + s / 2, y + s);
            h.cubicTo(x - s * 0.25f, y + s * 0.45f, x + s * 0.1f, y - s * 0.2f, x + s / 2, y + s * 0.3f);
            h.cubicTo(x + s * 0.9f, y - s * 0.2f, x + s * 1.25f, y + s * 0.45f, x + s / 2, y + s);
            paint.setColor(full ? Ui.DANGER : Ui.alpha(Ui.MUTED, 90));
            canvas.drawPath(h, paint);
        }
    }
}
