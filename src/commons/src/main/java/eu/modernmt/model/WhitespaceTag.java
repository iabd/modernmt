package eu.modernmt.model;

import java.util.regex.Pattern;

public class WhitespaceTag extends Tag {

    public static final Pattern TagRegex = Pattern.compile("\t|\n");

    private static final String NL_NAME = "NL";
    private static final String TAB_NAME = "TAB";
    private static final String WS_NAME = "WS";

    public static WhitespaceTag fromText(String text) {
        return fromText(text, null, null, -1);
    }

    public static WhitespaceTag fromText(String text, String leftSpace, String rightSpace, int position) {
        int length = text.length();

        if (length != 1)
            throw new IllegalArgumentException("Invalid tag: " + text);

        String name;
        Tag.Type type;

        if (text.charAt(0) == '\t') {
            type = Type.EMPTY_TAG;
            name = TAB_NAME;
        } else if (text.charAt(0) == '\n') {
            type = Type.EMPTY_TAG;
            name = NL_NAME;
        } else {
            type = Type.EMPTY_TAG;
            name = WS_NAME;
        }

        return new WhitespaceTag(name, text, leftSpace, rightSpace, position, type, false);
    }

    public static WhitespaceTag fromTag(Tag other) {
        return new WhitespaceTag(other.name, other.text, other.leftSpace, other.rightSpace, other.position, other.type, other.dtd);
    }

    protected WhitespaceTag(String name, String text, String leftSpace, String rightSpace, int position, WhitespaceTag.Type type, boolean dtd) {
        super(name, text, leftSpace, rightSpace, position, type, dtd);
    }

    public int compareTo(WhitespaceTag other) {
        return Integer.compare(this.position, other.getPosition());
    }

    @Override
    public String toString() {
        return text == null ? placeholder : text;
    }
}

