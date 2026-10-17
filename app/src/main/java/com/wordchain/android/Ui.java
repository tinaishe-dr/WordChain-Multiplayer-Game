package com.wordchain.android;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Palette and small view factories shared by every screen (all UI is built in code). */
final class Ui {
    static final int BG = 0xFF0B1020, SURFACE = 0xFF151B30, SURFACE_ALT = 0xFF1E2642, BORDER = 0xFF2C3560;
    static final int TEXT = 0xFFE8ECF8, MUTED = 0xFF8F9ABB, ACCENT = 0xFF7C5CFF, ACCENT_2 = 0xFF22D3EE;
    static final int GOOD = 0xFF34D399, WARN = 0xFFFBBF24, DANGER = 0xFFF87171;

    static final int PRIMARY = 0, SECONDARY = 1, DANGER_KIND = 2, GHOST = 3;

    private Ui() {}

    static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    static int alpha(int color, int a) { return (color & 0x00FFFFFF) | (a << 24); }

    static int mix(int a, int b, float t) {
        float u = Math.max(0f, Math.min(1f, t));
        return Color.argb(255,
                Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * u),
                Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * u),
                Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u));
    }

    static int avatar(String name) {
        return Color.HSVToColor(new float[] {Math.abs(name.hashCode()) % 360, 0.55f, 0.95f});
    }

    static GradientDrawable rounded(Context c, int fill, int stroke, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) d.setStroke(dp(c, 1), stroke);
        return d;
    }

    static CharSequence colored(String s, int color, boolean bold) {
        SpannableString sp = new SpannableString(s);
        sp.setSpan(new ForegroundColorSpan(color), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (bold) sp.setSpan(new StyleSpan(Typeface.BOLD), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sp;
    }

    static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static Button button(Context c, String label, int kind) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setStateListAnimator(null);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(c, 18), dp(c, 12), dp(c, 18), dp(c, 12));
        b.setGravity(Gravity.CENTER);
        int base;
        switch (kind) {
            case PRIMARY: base = ACCENT; break;
            case DANGER_KIND: base = mix(DANGER, BG, 0.45f); break;
            case GHOST: base = Color.TRANSPARENT; break;
            default: base = SURFACE_ALT;
        }
        int stroke = (kind == SECONDARY || kind == GHOST) ? BORDER : 0;
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[] {-android.R.attr.state_enabled},
                rounded(c, kind == GHOST ? Color.TRANSPARENT : mix(base, BG, 0.55f), stroke, 14));
        sld.addState(new int[] {android.R.attr.state_pressed},
                rounded(c, kind == GHOST ? SURFACE_ALT : mix(base, Color.BLACK, 0.25f), stroke, 14));
        sld.addState(new int[0], rounded(c, base, stroke, 14));
        b.setBackground(sld);
        b.setTextColor(new ColorStateList(new int[][] {{-android.R.attr.state_enabled}, {}}, new int[] {MUTED, TEXT}));
        return b;
    }

    static EditText field(Context c, String hint, boolean caps) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setInputType(caps ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                : InputType.TYPE_CLASS_TEXT);
        e.setBackground(rounded(c, BG, BORDER, 12));
        e.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        return e;
    }

    static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    static LinearLayout.LayoutParams lp(Context c, int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return p;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(rounded(c, SURFACE, BORDER, 22));
        l.setPadding(dp(c, 20), dp(c, 20), dp(c, 20), dp(c, 20));
        return l;
    }
}
