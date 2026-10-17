package com.wordchain.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import wordchain.common.Message;
import wordchain.common.Protocol;

/** Immutable snapshot of a room as last reported by the server. */
final class RoomState {
    static final class Player {
        final String name;
        final int lives, score, hints;

        Player(String name, int lives, int score, int hints) {
            this.name = name;
            this.lives = lives;
            this.score = score;
            this.hints = hints;
        }

        boolean alive() { return lives > 0; }
    }

    final String code, host, phase, customWord;
    final int maxPlayers, turnSeconds, lives;
    final List<Player> players;

    private RoomState(Message m, List<Player> players) {
        code = m.arg(0);
        host = m.arg(1);
        phase = m.arg(2);
        maxPlayers = m.intArg(3, 4);
        turnSeconds = m.intArg(4, 20);
        lives = m.intArg(5, 3);
        customWord = m.arg(6);
        this.players = Collections.unmodifiableList(players);
    }

    static RoomState parse(Message m) {
        List<Player> list = new ArrayList<>();
        String raw = m.arg(7);
        if (!raw.isEmpty()) {
            for (String entry : raw.split(",")) {
                String[] f = entry.split(":");
                if (f.length >= 4) list.add(new Player(f[0], toInt(f[1]), toInt(f[2]), toInt(f[3])));
            }
        }
        return new RoomState(m, list);
    }

    boolean isHost(String me) { return me != null && me.equals(host); }

    boolean playing() { return Protocol.PHASE_PLAYING.equals(phase); }

    private static int toInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}
