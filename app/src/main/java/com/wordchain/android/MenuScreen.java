package com.wordchain.android;

import android.content.Context;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Second screen: create a room or join one with a 4-character code, plus a quick rules card. */
final class MenuScreen extends ScrollView {
    interface Handler {
        void create();
        void join(String code);
        void disconnect();
    }

    private final TextView info, serverInfo;
    private final EditText code;

    MenuScreen(Context c, final Handler handler) {
        super(c);
        setFillViewport(true);
        setBackgroundColor(Ui.BG);
        LinearLayout outer = new LinearLayout(c);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);
        outer.setPadding(Ui.dp(c, 18), Ui.dp(c, 24), Ui.dp(c, 18), Ui.dp(c, 24));

        LinearLayout card = Ui.card(c);
        card.addView(Ui.text(c, "Ready to play?", 28, Ui.TEXT, true));
        info = Ui.text(c, " ", 13, Ui.MUTED, false);
        serverInfo = Ui.text(c, " ", 13, Ui.ACCENT_2, false);
        card.addView(info, top(c, 4));
        card.addView(serverInfo, top(c, 2));

        Button create = Ui.button(c, "Create a room", Ui.PRIMARY);
        create.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { handler.create(); } });
        card.addView(create, top(c, 20));

        card.addView(Ui.text(c, "OR JOIN WITH A ROOM CODE", 11, Ui.MUTED, true), top(c, 20));
        LinearLayout row = new LinearLayout(c);
        code = Ui.field(c, "ABCD", true);
        code.setFilters(new InputFilter[] {new InputFilter.LengthFilter(4), new InputFilter.AllCaps()});
        code.setTextSize(22);
        code.setImeOptions(EditorInfo.IME_ACTION_GO);
        Button join = Ui.button(c, "Join", Ui.SECONDARY);
        final Runnable doJoin = new Runnable() { @Override public void run() { handler.join(code.getText().toString()); } };
        join.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { doJoin.run(); } });
        code.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent e) { doJoin.run(); return true; }
        });
        row.addView(code, Ui.lp(0, LayoutParams.WRAP_CONTENT, 1f));
        row.addView(join, Ui.lp(c, LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 10, 0, 0, 0));
        card.addView(row, top(c, 6));

        LinearLayout rules = new LinearLayout(c);
        rules.setOrientation(LinearLayout.VERTICAL);
        rules.setBackground(Ui.rounded(c, Ui.SURFACE_ALT, 0, 16));
        rules.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 14));
        rules.addView(Ui.text(c, "How to play", 15, Ui.TEXT, true));
        String[] lines = {
            "Change exactly one letter to make a new real word (TALK \u2192 WALK).",
            "Tap a tile, tap the new letter, press Submit.",
            "Run out of time and you lose a life. Lose them all and you're out.",
            "Faster answers score more, and the clock speeds up as the chain grows.",
            "Trap the next player in a dead end for a bonus. You get 2 hints.",
        };
        for (String s : lines) rules.addView(Ui.text(c, "\u2022 " + s, 13, Ui.MUTED, false), top(c, 6));
        card.addView(rules, top(c, 22));

        Button out = Ui.button(c, "Disconnect", Ui.GHOST);
        out.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { handler.disconnect(); } });
        card.addView(out, top(c, 14));
        outer.addView(card);
        addView(outer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    void setInfo(String text, String serverText) {
        info.setText(text);
        serverInfo.setText(serverText.isEmpty() ? " " : serverText);
        code.setText("");
    }

    private static LinearLayout.LayoutParams top(Context c, int dp) {
        return Ui.lp(c, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0, dp, 0, 0);
    }
}
