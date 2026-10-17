package wordchain.server;

/** Per-room configuration, edited by the host while in the lobby. */
final class GameSettings {
    static final int MIN_PLAYERS = 2, MAX_PLAYERS = 4;
    static final int MIN_SECONDS = 10, MAX_SECONDS = 60;
    static final int MIN_LIVES = 1, MAX_LIVES = 5;

    int maxPlayers = 4;
    int turnSeconds = 20;
    int lives = 3;
    String customWord = "";

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
