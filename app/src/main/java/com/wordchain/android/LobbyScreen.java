package com.wordchain.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import wordchain.common.Message;
import wordchain.common.Protocol;

/** Waiting room: share the code, see who's here, host tweaks the rules and starts the game. */
final class LobbyScreen extends ScrollView {
    private final Sender sender;
    private final TextView code, hint;
    private final PlayersView players;
    private final StepperView maxPlayers, seconds, lives;
    private final EditText custom;
    private final Button setWord, start;
    private String appliedWord = "";

    LobbyScreen(final Context c, final Sender sender, final Runnable openChat) {
        super(c);
        this.sender = sender;
        setFillViewport(true);
        setBackgroundColor(Ui.BG);
        LinearLayout outer = new LinearLayout(c);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(Ui.dp(c, 18), Ui.dp(c, 24), Ui.dp(c, 18), Ui.dp(c, 24));

        LinearLayout card = Ui.card(c);
        card.addView(Ui.text(c, "ROOM CODE", 11, Ui.MUTED, true));
        code = Ui.text(c, "----", 48, Ui.ACCENT_2, true);
        code.setLetterSpacing(0.12f);
        card.addView(code);

        LinearLayout shareRow = new LinearLayout(c);
        Button copy = Ui.button(c, "Copy", Ui.SECONDARY);
        Button share = Ui.button(c, "Share", Ui.SECONDARY);
        Button chat = Ui.button(c, "Chat", Ui.SECONDARY);
        copy.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("room", code.getText()));
                final Button b = (Button) v;
                b.setText("Copied!");
                b.postDelayed(new Runnable() { @Override public void run() { b.setText("Copy"); } }, 1400);
            }
        });
        share.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, "Join my WordChain game! Room code: " + code.getText());
                c.startActivity(Intent.createChooser(i, "Invite friends"));
            }
        });
        chat.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { openChat.run(); } });
        shareRow.addView(copy);
        shareRow.addView(share, Ui.lp(c, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 8, 0, 0, 0));
        shareRow.addView(chat, Ui.lp(c, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 8, 0, 0, 0));
        card.addView(shareRow, top(c, 6));
        hint = Ui.text(c, " ", 13, Ui.MUTED, false);
        card.addView(hint, top(c, 10));

        card.addView(Ui.text(c, "PLAYERS", 11, Ui.MUTED, true), top(c, 18));
        players = new PlayersView(c, false);
        card.addView(players, top(c, 8));

        card.addView(Ui.text(c, "RULES", 11, Ui.MUTED, true), top(c, 10));
        maxPlayers = new StepperView(c, 2, 4, 1, "");
        seconds = new StepperView(c, 10, 60, 5, "s");
        lives = new StepperView(c, 1, 5, 1, "");
        card.addView(setting(c, "Max players", maxPlayers), top(c, 6));
        card.addView(setting(c, "Seconds per turn", seconds), top(c, 4));
        card.addView(setting(c, "Lives each", lives), top(c, 4));

        card.addView(Ui.text(c, "Custom start word (optional)", 14, Ui.TEXT, false), top(c, 12));
        LinearLayout wordRow = new LinearLayout(c);
        custom = Ui.field(c, "Random", true);
        custom.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(8), new android.text.InputFilter.AllCaps()});
        setWord = Ui.button(c, "Set", Ui.SECONDARY);
        wordRow.addView(custom, Ui.lp(0, LayoutParams.WRAP_CONTENT, 1f));
        wordRow.addView(setWord, Ui.lp(c, LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 8, 0, 0, 0));
        card.addView(wordRow, top(c, 6));

        start = Ui.button(c, "Start game", Ui.PRIMARY);
        Button leave = Ui.button(c, "Leave room", Ui.GHOST);
        card.addView(start, top(c, 22));
        card.addView(leave, top(c, 8));

        StepperView.OnChange push = new StepperView.OnChange() {
            @Override public void changed(int value) { pushSettings(appliedWord); }
        };
        maxPlayers.setOnChange(push);
        seconds.setOnChange(push);
        lives.setOnChange(push);
        setWord.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { pushSettings(custom.getText().toString()); }
        });
        start.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { sender.send(new Message(Protocol.START)); }
        });
        leave.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { sender.send(new Message(Protocol.LEAVE)); }
        });

        outer.addView(card);
        addView(outer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    void update(RoomState room, String me) {
        boolean host = room.isHost(me);
        code.setText(room.code);
        players.update(room, me, "");
        for (StepperView s : new StepperView[] {maxPlayers, seconds, lives}) s.setEnabled(host);
        maxPlayers.set(room.maxPlayers);
        seconds.set(room.turnSeconds);
        lives.set(room.lives);
        custom.setEnabled(host);
        setWord.setEnabled(host);
        appliedWord = room.customWord;
        if (!custom.hasFocus() || !host) custom.setText(appliedWord.toUpperCase());
        start.setVisibility(host ? VISIBLE : GONE);
        start.setEnabled(room.players.size() >= 2);
        hint.setText(host
                ? (room.players.size() >= 2 ? "You're the host \u2014 start whenever everyone's in."
                        : "Share the code with a friend. You need at least 2 players.")
                : "Waiting for " + room.host + " to start the game\u2026");
    }

    private void pushSettings(String word) {
        sender.send(new Message(Protocol.SETTINGS, maxPlayers.get(), seconds.get(), lives.get(), word.trim()));
    }

    private static LinearLayout setting(Context c, String label, StepperView stepper) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(Ui.text(c, label, 15, Ui.TEXT, false), Ui.lp(0, LayoutParams.WRAP_CONTENT, 1f));
        row.addView(stepper);
        return row;
    }

    private static LinearLayout.LayoutParams top(Context c, int dp) {
        return Ui.lp(c, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0, dp, 0, 0);
    }
}
