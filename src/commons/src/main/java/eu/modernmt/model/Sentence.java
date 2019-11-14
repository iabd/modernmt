package eu.modernmt.model;

import eu.modernmt.xml.XMLUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by davide on 17/02/16.
 */
public class Sentence implements Serializable, Iterable<Token> {

    protected final Word[] words;
    protected Tag[] tags;
    protected Set<String> annotations;

    public Sentence(Word[] words) {
        this(words, null);
    }

    public Sentence(Word[] words, Tag[] tags) {
        this.words = words == null ? new Word[0] : words;
        this.tags = tags == null ? new Tag[0] : tags;
    }

    public Word[] getWords() {
        return words;
    }

    public int length() {
        return words.length + tags.length;
    }

    public Tag[] getTags() {
        return tags;
    }

    public boolean hasTags() {
        return tags.length > 0;
    }

    public boolean hasWords() {
        return words.length > 0;
    }

    /**
     * Sets tags of the sentence
     *
     * @param tags is an array of tags <b>ordered by position field</b>.
     */
    public void setTags(Tag[] tags) {
        this.tags = tags;
    }

    public void addAnnotations(Set<String> annotations) {
        if (this.annotations == null)
            this.annotations = new HashSet<>(annotations);
        else
            this.annotations.addAll(annotations);
    }

    public void addAnnotation(String annotation) {
        if (annotations == null)
            annotations = new HashSet<>(5);
        annotations.add(annotation);
    }

    public boolean hasAnnotation(String annotation) {
        return annotations != null && annotations.contains(annotation);
    }

    @Override
    public String toString() {
        return toString(true, false);
    }

    public String toString(boolean printTags, boolean printPlaceholders) {
        return printTags ? toXMLString(printPlaceholders) : toXMLStrippedString(printPlaceholders);
    }

    private String toXMLStrippedString(boolean printPlaceholders) {
        StringBuilder builder = new StringBuilder();

        boolean foundFirstWord = false;
        boolean printSpace = false;

        String space = null;

        for (Token token : this) {
            String currentLeftSpace = token.getLeftSpace();
            String currentRightSpace = token.getRightSpace();

            if (space != null) {
                if (currentLeftSpace != null && !space.equals(currentLeftSpace)) {
                    space = currentLeftSpace;
                }
            } else {
                space = currentLeftSpace;
            }

            if (token instanceof Tag) {

                if ((space != null && currentRightSpace != null && !space.equals(currentRightSpace)) || space == null) {
                    space = currentRightSpace;
                }

                if (foundFirstWord && space == null) {
                    printSpace = true;
                }

            } else {
                if (space != null) {
                    if (foundFirstWord)
                        builder.append(space);
                } else {
                    if (printSpace && foundFirstWord)
                        builder.append(" ");
                }
                String text = printPlaceholders || !token.hasText() ? token.getPlaceholder() : token.getText();
                builder.append(text);

                space = currentRightSpace;
                printSpace = false;
                foundFirstWord = true;
            }
        }

        return builder.toString();
    }
    private String toXMLString(boolean printPlaceholders) {
        StringBuilder builder = new StringBuilder();

        Token previousToken = null;
        String space = null;
        String currentLeftSpace;
        String currentRightSpace;

        for (Token token : this) {
            currentLeftSpace = token.getLeftSpace();
            currentRightSpace = token.getRightSpace();

            if (space != null) {
                if (currentLeftSpace != null && !space.equals(currentLeftSpace)) {
                    space = currentLeftSpace;
                }
            } else {
                if (previousToken instanceof Tag && ((Tag) previousToken).getType() != Tag.Type.CLOSING_TAG) {
                    space = previousToken.getRightSpace();
                } else {
                    space = currentLeftSpace;
                }
            }

            if (space != null) {
                builder.append(space);
            }

            if (token instanceof Tag) {
                builder.append(token.getText());
            } else {
                String text = printPlaceholders || !token.hasText() ? token.getPlaceholder() : token.getText();
                builder.append(XMLUtils.escapeText(text));
            }

            space = currentRightSpace;
            previousToken = token;
        }

        return builder.toString();
    }

    @Override
    public Iterator<Token> iterator() {
        return new Iterator<Token>() {

            private final Token[] tokens = Sentence.this.words;
            private final Tag[] tags = Sentence.this.tags;

            private int tokenIndex = 0;
            private int tagIndex = 0;

            @Override
            public boolean hasNext() {
                return tokenIndex < tokens.length || tagIndex < tags.length;
            }

            @Override
            public Token next() {
                Token nextToken = tokenIndex < tokens.length ? tokens[tokenIndex] : null;
                Tag nextTag = tagIndex < tags.length ? tags[tagIndex] : null;

                if (nextTag != null && (nextToken == null || tokenIndex == nextTag.getPosition())) {
                    tagIndex++;
                    return nextTag;
                } else {
                    tokenIndex++;
                    return nextToken;
                }
            }
        };
    }
}
