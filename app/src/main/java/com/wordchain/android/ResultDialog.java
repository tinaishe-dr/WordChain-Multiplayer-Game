package com.wordchain.android;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Random;

/** End-of-game standings with a little confetti. */
final class ResultDialog extends Dialog {
    static final class Standing {
        final String name;
        final int score;
        final boolean survived;

        Standing(String name, int score, boolean survived) {
            this.name = name;
            this.score = score;
            this.survived = survived;
        }
    }

    ResultDialog(Context c, String winner, String me, List<Standing> standings, final Runnable onBack) {
        super(c, android.R.style.Theme_Material_NoActionBar);
        setCancelable(false);
        FrameLayout root = new FrameLayout(c);
        root.setBackgroundColor(Ui.BG);
        root.addView(new Confetti(c), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout card = Ui.card(c);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        boolean won = winner.equals(me);
        String headline = winner.isEmpty() ? "Game over" : won ? "You win! \uD83C\uDFC6" : winner + " wins!";
        TextView title = Ui.text(c, headline, 28, won ? Ui.GOOD : Ui.TEXT, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title);
        card.addView(Ui.text(c, "Final standings", 14, Ui.MUTED, false));
        int rank = 1;
        for (Standing s : standings) {
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = Ui.text(c, rank++ + ".  " + s.name + (s.name.equals(me) ? " (you)" : "") + (s.survived ? "  \u2605" : ""),
                    17, s.survived ? Ui.WARN : Ui.TEXT, true);
            TextView pts = Ui.text(c, s.score + " pts", 17, Ui.ACCENT_2, true);
            row.addView(name, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(pts);
            card.addView(row, Ui.lp(c, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0, 14, 0, 0));
        }
        Button back = Ui.button(c, "Back to lobby", Ui.PRIMARY);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dismiss(); onBack.run(); }
        });
        card.addView(back, Ui.lp(c, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0, 26, 0, 0));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        lp.setMargins(Ui.dp(c, 20), 0, Ui.dp(c, 20), 0);
        root.addView(card, lp);
        setContentView(root);
    }

    private static final class Confetti extends View {
        private final Random rnd = new Random();
        private final float[] x = new float[70], y = new float[70], vy = new float[70], vx = new float[70], size = new float[70];
        private final int[] color = new int[70];
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private boolean ready;

        Confetti(Context c) { super(c); }

        private void spawn(int i, boolean anywhere) {
            x[i] = rnd.nextFloat() * getWidth();
            y[i] = anywhere ? -rnd.nextFloat() * getHeight() : -20;
            vy[i] = 3 + rnd.nextFloat() * 5;
            vx[i] = rnd.nextFloat() * 3 - 1.5f;
            size[i] = Ui.dp(getContext(), 5) + rnd.nextFloat() * Ui.dp(getContext(), 6);
            color[i] = android.graphics.Color.HSVToColor(new float[] {rnd.nextFloat() * 360, 0.7f, 1f});
        }

        @Override protected void onDraw(Canvas canvas) {
            if (!ready) { for (int i = 0; i < x.length; i++) spawn(i, true); ready = true; }
            for (int i = 0; i < x.length; i++) {
                y[i] += vy[i];
                x[i] += vx[i];
                if (y[i] > getHeight()) spawn(i, false);
                paint.setColor(color[i]);
                rect.set(x[i], y[i], x[i] + size[i], y[i] + size[i] * 1.6f);
                canvas.drawRoundRect(rect, 3, 3, paint);
            }
            postInvalidateOnAnimation();
        }
    }
}
