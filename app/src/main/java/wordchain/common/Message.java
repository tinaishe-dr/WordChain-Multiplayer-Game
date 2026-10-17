package wordchain.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** One protocol message: a type plus tab-separated arguments, sent as a single text line. */
public final class Message {
    private static final String SEP = "\t";

    private final String type;
    private final List<String> args;

    public Message(String type, Object... args) {
        this.type = type;
        List<String> list = new ArrayList<>(args.length);
        for (Object a : args) list.add(String.valueOf(a).replaceAll("[\\x00-\\x1F\\x7F]", " "));
        this.args = Collections.unmodifiableList(list);
    }

    public String type() { return type; }

    public int size() { return args.size(); }

    public String arg(int i) { return i < args.size() ? args.get(i) : ""; }

    public int intArg(int i, int fallback) {
        try {
            return Integer.parseInt(arg(i).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public String encode() {
        StringBuilder sb = new StringBuilder(type);
        for (String a : args) sb.append(SEP).append(a);
        return sb.toString();
    }

    public static Message decode(String line) {
        String[] parts = line.split(SEP, -1);
        return new Message(parts[0], (Object[]) Arrays.copyOfRange(parts, 1, parts.length));
    }

    @Override
    public String toString() { return encode().replace('\t', ' '); }
}
