package wordchain.common;

import java.util.Locale;

/** Pure helpers describing what a legal word and a legal move look like. */
public final class WordRules {
    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 8;

    private WordRules() {}

    public static String normalise(String w) {
        return w == null ? "" : w.trim().toLowerCase(Locale.ROOT);
    }

    /** True for 3-8 lowercase ASCII letters. */
    public static boolean isWellFormed(String w) {
        if (w == null || w.length() < MIN_LENGTH || w.length() > MAX_LENGTH) return false;
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (c < 'a' || c > 'z') return false;
        }
        return true;
    }

    public static String replaceAt(String word, int pos, char letter) {
        return word.substring(0, pos) + letter + word.substring(pos + 1);
    }
}
