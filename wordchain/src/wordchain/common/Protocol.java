package wordchain.common;

/** Message types and shared constants for the client/server protocol. */
public final class Protocol {
    private Protocol() {}

    public static final int DEFAULT_PORT = 5050;

    // Client -> server
    public static final String HELLO = "HELLO";        // name
    public static final String CREATE = "CREATE";      //
    public static final String JOIN = "JOIN";          // code
    public static final String SETTINGS = "SETTINGS";  // maxPlayers, seconds, lives, customWord
    public static final String START = "START";        //
    public static final String MOVE = "MOVE";          // position, letter
    public static final String HINT = "HINT";          //
    public static final String CHAT = "CHAT";          // text
    public static final String RESIGN = "RESIGN";      //
    public static final String LEAVE = "LEAVE";        //

    // Server -> client
    public static final String WELCOME = "WELCOME";        // name
    public static final String JOINED = "JOINED";          // code, name
    public static final String LEFT = "LEFT";              //
    public static final String ROOM = "ROOM";              // code, host, phase, max, seconds, lives, custom, players
    public static final String GAME_START = "GAME_START";  // word
    public static final String TURN = "TURN";              // player, seconds, word, chain
    public static final String ACCEPTED = "ACCEPTED";      // player, oldWord, newWord, position, points
    public static final String REJECTED = "REJECTED";      // reason, position
    public static final String TIMEOUT = "TIMEOUT";        // player, livesLeft
    public static final String ELIMINATED = "ELIMINATED";  // player
    public static final String DEAD_END = "DEAD_END";      // stuckWord, bonusPlayer, bonus, newWord
    public static final String HINT_POS = "HINT_POS";      // position
    public static final String GAME_OVER = "GAME_OVER";    // winner, standings... (name:score:survived)
    public static final String INFO = "INFO";              // text
    public static final String ERROR = "ERROR";            // text
    public static final String CHAT_MSG = "CHAT_MSG";      // name, text

    public static final String PHASE_LOBBY = "LOBBY";
    public static final String PHASE_PLAYING = "PLAYING";
}
