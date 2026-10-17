package com.wordchain.android;

import android.app.Dialog;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import wordchain.common.Message;
import wordchain.common.Protocol;

/** Full-screen chat & game log. Reads from the shared {@link FeedModel}; sends chat via {@link Sender}. */
final class FeedDialog extends Dialog implements FeedModel.Listener {
    private final FeedModel model;
    private final TextView body;
    private final ScrollView scroll;

    FeedDialog(Context c, FeedModel model, final Sender sender) {
        super(c, android.R.style.Theme_Material_NoActionBar);
        this.model = model;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(c, 16), Ui.dp(c, 16), Ui.dp(c, 16), Ui.dp(c, 16));

        LinearLayout header = new LinearLayout(c);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Ui.text(c, "Chat & log", 20, Ui.TEXT, true);
        header.addView(title, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button close = Ui.button(c, "Close", Ui.SECONDARY);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dismiss(); }
        });
        header.addView(close);
        root.addView(header);

        body = Ui.text(c, "", 15, Ui.TEXT, false);
        body.setLineSpacing(Ui.dp(c, 3), 1f);
        scroll = new ScrollView(c);
        scroll.setBackground(Ui.rounded(c, Ui.SURFACE, Ui.BORDER, 16));
        scroll.setPadding(Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12));
        scroll.addView(body);
        root.addView(scroll, weighted(Ui.lp(c, LinearLayout.LayoutParams.MATCH_PARENT, 0, 0, 12, 0, 12), 1f));

        LinearLayout inputRow = new LinearLayout(c);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        final EditText input = Ui.field(c, "Say something\u2026", false);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        Button send = Ui.button(c, "Send", Ui.PRIMARY);
        final Runnable submit = new Runnable() {
            @Override public void run() {
                String t = input.getText().toString().trim();
                if (t.isEmpty()) return;
                input.setText("");
                sender.send(new Message(Protocol.CHAT, t));
            }
        };
        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { submit.run(); }
        });
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent e) {
                submit.run();
                return true;
            }
        });
        inputRow.addView(input, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        inputRow.addView(send, Ui.lp(c, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 8, 0, 0, 0));
        root.addView(inputRow);
        setContentView(root);
    }

    @Override protected void onStart() {
        super.onStart();
        model.addListener(this);
        onFeedChanged();
    }

    @Override protected void onStop() {
        model.removeListener(this);
        super.onStop();
    }

    @Override public void onFeedChanged() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (CharSequence line : model.lines()) sb.append(line).append('\n');
        body.setText(sb);
        scroll.post(new Runnable() {
            @Override public void run() { scroll.fullScroll(View.FOCUS_DOWN); }
        });
    }

    private static LinearLayout.LayoutParams weighted(LinearLayout.LayoutParams p, float weight) {
        p.weight = weight;
        return p;
    }
}
