package wordchain.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import wordchain.common.WordRules;

/** English word list used to validate moves and to pick starting words. */
public final class Dictionary {
    private static final int MIN_START_LEN = 4, MAX_START_LEN = 6, MIN_START_NEIGHBOURS = 4;

    private final Set<String> words;
    private final List<String> starters = new ArrayList<>();

    private Dictionary(Set<String> words, List<String> candidates) {
        this.words = words;
        for (String c : candidates) {
            if (c.length() >= MIN_START_LEN && c.length() <= MAX_START_LEN && words.contains(c)
                    && neighbours(c, Collections.<String>emptySet()).size() >= MIN_START_NEIGHBOURS) {
                starters.add(c);
            }
        }
        if (starters.isEmpty()) throw new IllegalStateException("No usable starting words found");
    }

    /** Reads the full word list and the list of common starter words (one word per line). */
    public static Dictionary load(InputStream wordList, InputStream starterList) throws IOException {
        return new Dictionary(read(wordList), new ArrayList<>(read(starterList)));
    }

    private static Set<String> read(InputStream in) throws IOException {
        Set<String> out = new HashSet<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) {
                String w = WordRules.normalise(line);
                if (WordRules.isWellFormed(w)) out.add(w);
            }
        }
        return out;
    }

    public boolean contains(String word) { return words.contains(word); }

    public List<String> neighbours(String word, Set<String> used) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            for (char c = 'a'; c <= 'z'; c++) {
                if (c == word.charAt(i)) continue;
                String n = WordRules.replaceAt(word, i, c);
                if (words.contains(n) && !used.contains(n)) out.add(n);
            }
        }
        return out;
    }

    public List<Integer> playablePositions(String word, Set<String> used) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            for (char c = 'a'; c <= 'z'; c++) {
                if (c == word.charAt(i)) continue;
                String n = WordRules.replaceAt(word, i, c);
                if (words.contains(n) && !used.contains(n)) { out.add(i); break; }
            }
        }
        return out;
    }

    public boolean hasMove(String word, Set<String> used) { return !playablePositions(word, used).isEmpty(); }

    public String randomStarter(Random rnd, Set<String> avoid) {
        for (int i = 0; i < 50; i++) {
            String w = starters.get(rnd.nextInt(starters.size()));
            if (!avoid.contains(w)) return w;
        }
        return starters.get(rnd.nextInt(starters.size()));
    }
}
