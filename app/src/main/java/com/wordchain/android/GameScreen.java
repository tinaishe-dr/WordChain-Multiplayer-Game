package com.wordchain.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import wordchain.common.Message;
import wordchain.common.Protocol;

/** The match screen: scoreboard strip, timer, tiles, on-screen keyboard. */
final class GameScreen extends ScrollView {
    private final Sender sender;
    private final FeedModel feed;
    private final TileBoardView board;
    private final TimerBarView timer;
    private final KeyboardView keyboard;
    private final PlayersView players;
    private final TextView roomLabel, banner, sub, message;
    private final Button submit, hint;
    private RoomState room;
    private String me = "", active = "";

    GameScreen(final Context c, final Sender sender, FeedModel feed, final Runnable openLog) {
        super(c);
        this.sender = sender;
        this.feed = feed;
        setFillViewport(true);
        setBackgroundColor(Ui.BG);

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12));

        LinearLayout top = new LinearLayout(c);
        top.setGravity(Gravity.CENTER_VERTICAL);
        roomLabel = Ui.text(c, "", 13, Ui.MUTED, true);
        top.addView(roomLabel, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button log = Ui.button(c, "Log & chat", Ui.SECONDARY);
        Button resign = Ui.button(c, "Resign", Ui.DANGER_KIND);
        log.setPadding(Ui.dp(c, 12), Ui.dp(c, 7), Ui.dp(c, 12), Ui.dp(c, 7));
        resign.setPadding(Ui.dp(c, 12), Ui.dp(c, 7), Ui.dp(c, 12), Ui.dp(c, 7));
        log.setTextSize(13);
        resign.setTextSize(13);
        log.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { openLog.run(); } });
        resign.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { confirmResign(); } });
        top.addView(log);
        top.addView(resign, Ui.lp(c, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 8, 0, 0, 0));
        root.addView(top);

        players = new PlayersView(c, true);
        root.addView(players, Ui.lp(c, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0, 10, 0, 0));

        banner = Ui.text(c, " ", 28, Ui.TEXT, true);
        banner.setGravity(Gravity.CENTER);
        sub = Ui.text(c, " ", 13, Ui.MUTED, false);
        sub.setGravity(Gravity.CENTER);
        root.addView(banner, Ui.lp(c, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0, 14, 0, 0));
        root.addView(sub, Ui.lp(c, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0, 2, 0, 0));

        timer = new TimerBarView(c);
        root.addView(timer, Ui.lp(c, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 8, 12, 8, 0));

        board = new TileBoardView(c);
        root.addView(board, Ui.lp(c, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0, 10, 0, 0));
        message = Ui.text(c, " ", 14, Ui.MUTED, true);
        message.setGravity(Gravity.CENTER);
        root.addView(message, Ui.lp(c, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0, 4, 0, 0));

        View spacer = new View(c);
        root.addView(spacer, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout buttons = new LinearLayout(c);
        hint = Ui.button(c, "Hint (2)", Ui.SECONDARY);
        submit = Ui.button(c, "Submit", Ui.PRIMARY);
        buttons.addView(hint, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        buttons.addView(submit, Ui.lp(c, 0, LinearLayout.LayoutParams.WRAP_CONTENT, 10, 0, 0, 0));
        ((LinearLayout.LayoutParams) submit.getLayoutParams()).weight = 2f;
        root.addView(buttons, Ui.lp(c, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0, 12, 0, 8));

        keyboard = new KeyboardView(c);
        root.addView(keyboard, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        addView(root, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        keyboard.setListener(new KeyboardView.Listener() {
            @Override public void onLetter(char ch) { board.typeLetter(ch); }
            @Override public void onBackspace() { board.backspace(); }
        });
        board.setListener(new TileBoardView.Listener() {
            @Override public void onSubmit(int position, char letter) {
                sender.send(new Message(Protocol.MOVE, position, String.valueOf(letter)));
            }
            @Override public void onProblem(String text) { flash(text, Ui.WARN); }
        });
        submit.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { board.submit(); } });
        hint.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { sender.send(new Message(Protocol.HINT)); }
        });
        updateButtons();
    }

    void confirmResign() {
        new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Resign?")
                .setMessage("You'll be out of this game.")
                .setNegativeButton("Keep playing", null)
                .setPositiveButton("Resign", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) { sender.send(new Message(Protocol.RESIGN)); }
                })
                .show();
    }

    // ------------------------------------------------------- server events

    void onRoom(RoomState r, String myName) {
        room = r;
        me = myName;
        roomLabel.setText("ROOM " + r.code);
        players.update(r, me, active);
        updateButtons();
    }

    void onGameStart(String word) {
        feed.addNote("\u2014 New game \u2014", Ui.ACCENT_2);
        feed.addNote("Start word: " + word.toUpperCase(), Ui.MUTED);
        board.setWord(word);
        active = "";
        banner.setText("Get ready\u2026");
        banner.setTextColor(Ui.TEXT);
        sub.setText(" ");
        flash(" ", Ui.MUTED);
        fullScroll(View.FOCUS_UP);
    }

    void onTurn(String player, int seconds, String word, int chainLength) {
        active = player;
        boolean mine = player.equals(me);
        board.setWord(word);
        board.setInteractive(mine);
        keyboard.setEnabled(mine);
        keyboard.invalidate();
        timer.start(seconds);
        banner.setText(mine ? "Your turn!" : player + "'s turn");
        banner.setTextColor(mine ? Ui.GOOD : Ui.TEXT);
        sub.setText(mine ? "Tap a letter to erase it, pick a new one, Submit"
                : "Waiting for " + player + " \u2022 chain " + chainLength + " \u2022 " + seconds + "s per turn");
        if (mine) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        if (room != null) players.update(room, me, active);
        updateButtons();
        flash(" ", Ui.MUTED);
    }

    void onAccepted(String player, String oldWord, String newWord, int pos, int points) {
        feed.addMove(player, newWord, points);
        board.setInteractive(false);
        keyboard.setEnabled(false);
        keyboard.invalidate();
        board.setWord(newWord);
        board.popTile(pos);
        timer.stop("");
        flash(player + " made " + newWord.toUpperCase() + "  (+" + points + ")", Ui.GOOD);
    }

    void onRejected(String reason) {
        board.shake();
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        flash(reason, Ui.DANGER);
    }

    void onTimeout(String player, int livesLeft) {
        feed.addNote("\u23F1 " + player + " ran out of time (" + livesLeft + (livesLeft == 1 ? " life" : " lives") + " left)", Ui.DANGER);
        board.setInteractive(false);
        keyboard.setEnabled(false);
        keyboard.invalidate();
        board.shake();
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        flash(player.equals(me) ? "Time's up! You lost a life." : player + " ran out of time!", Ui.DANGER);
    }

    void onEliminated(String player) { feed.addNote("\u2716 " + player + " is out", Ui.DANGER); }

    void onDeadEnd(String stuckWord, String bonusPlayer, int bonus, String newWord) {
        String who = bonusPlayer.isEmpty() ? "" : " \u2014 " + bonusPlayer + " +" + bonus;
        feed.addNote("\uD83D\uDEA7 Dead end at " + stuckWord.toUpperCase() + who + ". New word: " + newWord.toUpperCase(), Ui.WARN);
        board.setWord(newWord);
        flash("Dead end! New word: " + newWord.toUpperCase() + (bonusPlayer.isEmpty() ? "" : "  (" + bonusPlayer + " +" + bonus + ")"), Ui.WARN);
    }

    void onHint(int pos) {
        board.showHint(pos);
        flash("Hint: change the highlighted letter.", Ui.WARN);
    }

    void stopClock(String text) { timer.stop(text); }

    private void updateButtons() {
        int left = 0;
        if (room != null) for (RoomState.Player p : room.players) if (p.name.equals(me)) left = p.hints;
        boolean mine = me.equals(active);
        hint.setText("Hint (" + left + ")");
        hint.setEnabled(mine && left > 0);
        submit.setEnabled(mine);
    }

    private void flash(String text, int color) {
        message.setText(text);
        message.setTextColor(color);
    }
}
