package com.wordchain.android;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/** Shared transcript of chat messages, moves and system notices; views just render it. */
final class FeedModel {
    interface Listener { void onFeedChanged(); }

    private static final int MAX_LINES = 300;

    private final List<CharSequence> lines = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();

    void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }

    void removeListener(Listener l) { listeners.remove(l); }

    List<CharSequence> lines() { return lines; }

    void clear() { lines.clear(); changed(); }

    void add(CharSequence line) {
        lines.add(line);
        if (lines.size() > MAX_LINES) lines.remove(0);
        changed();
    }

    void addChat(String name, String text, boolean mine) {
        add(TextUtils.concat(Ui.colored(name + "  ", Ui.avatar(name), true), Ui.colored(text, mine ? Ui.TEXT : 0xFFC9D1EA, false)));
    }

    void addSystem(String text) { add(Ui.colored("\u2022 " + text, Ui.MUTED, false)); }

    void addNote(String text, int color) { add(Ui.colored(text, color, false)); }

    void addMove(String player, String word, int points) {
        add(TextUtils.concat(Ui.colored(word.toUpperCase() + "  ", Ui.TEXT, true),
                Ui.colored(player + "  ", Ui.MUTED, false), Ui.colored("+" + points, Ui.GOOD, true)));
    }

    private void changed() {
        for (Listener l : new ArrayList<>(listeners)) l.onFeedChanged();
    }
}
