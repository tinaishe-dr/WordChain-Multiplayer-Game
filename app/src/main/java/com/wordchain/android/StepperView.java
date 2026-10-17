package com.wordchain.android;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** "[-] value [+]" control for a bounded integer. {@link #set} never fires the change callback. */
final class StepperView extends LinearLayout {
    interface OnChange { void changed(int value); }

    private final int min, max, step;
    private final String suffix;
    private final TextView value;
    private final Button minus, plus;
    private int current;
    private OnChange callback;

    StepperView(Context c, int min, int max, int step, String suffix) {
        super(c);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        setGravity(Gravity.CENTER_VERTICAL);
        minus = small(c, "\u2212");
        plus = small(c, "+");
        value = Ui.text(c, "", 16, Ui.TEXT, true);
        value.setGravity(Gravity.CENTER);
        addView(minus, new LayoutParams(Ui.dp(c, 42), Ui.dp(c, 42)));
        addView(value, new LayoutParams(Ui.dp(c, 58), LayoutParams.WRAP_CONTENT));
        addView(plus, new LayoutParams(Ui.dp(c, 42), Ui.dp(c, 42)));
        minus.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { change(-StepperView.this.step); } });
        plus.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { change(StepperView.this.step); } });
        set(min);
    }

    private static Button small(Context c, String label) {
        Button b = Ui.button(c, label, Ui.SECONDARY);
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    void setOnChange(OnChange cb) { callback = cb; }

    int get() { return current; }

    void set(int v) {
        current = Math.max(min, Math.min(max, v));
        value.setText(current + suffix);
        minus.setEnabled(isEnabled() && current > min);
        plus.setEnabled(isEnabled() && current < max);
    }

    private void change(int delta) {
        set(current + delta);
        if (callback != null) callback.changed(current);
    }

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        set(current);
    }
}
