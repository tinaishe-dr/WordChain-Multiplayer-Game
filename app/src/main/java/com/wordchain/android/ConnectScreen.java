package com.wordchain.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import wordchain.common.Protocol;

/** First screen: pick a name, then join a server or host one on this phone. */
final class ConnectScreen extends ScrollView {
    interface Handler { void connect(String name, String host, int port, boolean hostHere); }

    private final SharedPreferences prefs;
    private final EditText name, host, port;
    private final CheckBox hostHere;
    private final Button go;
    private final TextView status;

    ConnectScreen(Context c, final Handler handler) {
        super(c);
        prefs = c.getSharedPreferences("wordchain", Context.MODE_PRIVATE);
        setFillViewport(true);
        setBackgroundColor(Ui.BG);

        LinearLayout outer = new LinearLayout(c);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);
        outer.setPadding(Ui.dp(c, 18), Ui.dp(c, 24), Ui.dp(c, 18), Ui.dp(c, 24));

        LinearLayout card = Ui.card(c);
        card.addView(Ui.text(c, "WordChain", 36, Ui.TEXT, true));
        card.addView(Ui.text(c, "Swap one letter. Make a new word. Beat the clock.", 14, Ui.MUTED, false));

        name = Ui.field(c, "Your name", false);
        name.setText(prefs.getString("name", ""));
        host = Ui.field(c, "e.g. 192.168.1.20", false);
        host.setText(prefs.getString("host", ""));
        host.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI
                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        port = Ui.field(c, "Port", false);
        port.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        port.setText(String.valueOf(prefs.getInt("port", Protocol.DEFAULT_PORT)));

        card.addView(label(c, "YOUR NAME"), top(c, 24));
        card.addView(name);
        LinearLayout serverRow = new LinearLayout(c);
        LinearLayout hostCol = new LinearLayout(c);
        hostCol.setOrientation(LinearLayout.VERTICAL);
        hostCol.addView(label(c, "SERVER ADDRESS"));
        hostCol.addView(host, Ui.lp(c, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0, 6, 0, 0));
        LinearLayout portCol = new LinearLayout(c);
        portCol.setOrientation(LinearLayout.VERTICAL);
        portCol.addView(label(c, "PORT"));
        portCol.addView(port, Ui.lp(c, Ui.dp(c, 96), LayoutParams.WRAP_CONTENT, 0, 6, 0, 0));
        serverRow.addView(hostCol, Ui.lp(0, LayoutParams.WRAP_CONTENT, 1f));
        serverRow.addView(portCol, Ui.lp(c, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 10, 0, 0, 0));
        card.addView(serverRow, top(c, 16));

        hostHere = new CheckBox(c);
        hostHere.setText("Host the game on this phone");
        hostHere.setTextColor(Ui.TEXT);
        hostHere.setTextSize(15);
        hostHere.setChecked(false);
        hostHere.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                host.setHint(hostHere.isChecked() ? "Not needed \u2014 hosting here" : "e.g. 192.168.1.20");
            }
        });
        card.addView(hostHere, top(c, 14));
        card.addView(Ui.text(c, "Friends on the same Wi-Fi join using the address shown on the next screen.", 12, Ui.MUTED, false));

        go = Ui.button(c, "Play", Ui.PRIMARY);
        go.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { submit(handler); }
        });
        card.addView(go, top(c, 22));
        status = Ui.text(c, " ", 13, Ui.MUTED, false);
        card.addView(status, top(c, 12));

        outer.addView(card);
        addView(outer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    private void submit(Handler handler) {
        int p;
        try {
            p = Integer.parseInt(port.getText().toString().trim());
            if (p < 1 || p > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            fail("Port must be a number from 1 to 65535.");
            return;
        }
        String n = name.getText().toString().trim();
        String h = host.getText().toString().trim();
        if (n.isEmpty()) { fail("Please enter a name."); return; }
        if (!hostHere.isChecked() && h.isEmpty()) { fail("Enter the server address."); return; }
        prefs.edit().putString("name", n).putString("host", h).putInt("port", p).apply();
        setBusy(hostHere.isChecked() ? "Starting server\u2026" : "Connecting\u2026");
        handler.connect(n, hostHere.isChecked() ? "127.0.0.1" : h, p, hostHere.isChecked());
    }

    void setBusy(String text) {
        go.setEnabled(false);
        status.setText(text);
        status.setTextColor(Ui.MUTED);
    }

    void fail(String text) {
        go.setEnabled(true);
        status.setText(text);
        status.setTextColor(Ui.DANGER);
    }

    private static TextView label(Context c, String s) { return Ui.text(c, s, 11, Ui.MUTED, true); }

    private static LinearLayout.LayoutParams top(Context c, int dp) {
        return Ui.lp(c, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0, dp, 0, 0);
    }
}
