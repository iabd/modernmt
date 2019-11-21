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
    public static final String TAB_PLACEHOLDER = "tabPlaceholder";
    public static final String NL_PLACEHOLDER = "nlPlaceholder";

    protected final Word[] words;
    protected Tag[] tags;
    protected Set<String> annotations;
    private boolean spaceConsistent = false;

    public Sentence(Word[] words) {
        this(words, null);
    }

    public Sentence(Word[] words, Tag[] tags) {
        this.words = words == null ? new Word[0] : words;
        this.tags = tags == null ? new XMLTag[0] : tags;
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


    public boolean isSpaceConsistent() {
        return spaceConsistent;
    }

    public void setSpaceConsistent(boolean spaceConsistent) {
        this.spaceConsistent = spaceConsistent;
    }

    @Override
    public String toString() {
        return toString(true, false);
    }

    public String toString(boolean printTags, boolean printPlaceholders) {
        return printTags ? toXMLString(printPlaceholders) : toXMLStrippedString(printPlaceholders);
    }


    public static String passSpace(String leftSpace, String rightSpace) {
        /*
        Null-Null: null
        Null-Virt: virt
        Null-Real: real2
        Virt-Null: virt
        Virt-Virt: virt
        Virt-Real: real2
        Real-Null: real1
        Real-Virt: real1
        Real-Real: real1 (with merging)
 */
        String space = leftSpace;
        if (leftSpace == null) {
            space = rightSpace;
        } else if (leftSpace == Token.VIRTUAL_SPACE) {
            if (rightSpace != Token.VIRTUAL_SPACE && rightSpace == Token.VIRTUAL_SPACE) {
                space = rightSpace;
            }
        } else {
            if (rightSpace != Token.VIRTUAL_SPACE && rightSpace == Token.VIRTUAL_SPACE) {
                if (!leftSpace.equals(rightSpace)) { //both real spaces, but different
                    space = space + rightSpace;
                }
            }
        }
        return space;
    }

    public static String getSpace(Token leftToken, Token rightToken) {
        return getSpace(null, leftToken, rightToken);
    }

    public static String getSpace(String prevSpace, Token leftToken, Token rightToken) {
        String leftSpace = leftToken != null ? leftToken.getRightSpace() : null;
        String rightSpace = rightToken != null ? rightToken.getLeftSpace() : null;
        boolean leftIsWord = leftToken instanceof Word;
        boolean rightIsWord = rightToken instanceof Word;

        String space = prevSpace == null ? leftSpace : passSpace(prevSpace, leftSpace);


        if (leftIsWord) {
            if (rightIsWord) { // left:Word, right:Word
/*
                right   right   right
                null    virt    real2
left    null    null    virt    real2
left    virt    virt    virt    real2
left    real1   real1   real1   real1+real2
*/
                if (space == null) {
                    space = rightSpace;
                } else if (space.equals(Token.VIRTUAL_SPACE)) {
                    if (rightSpace != null) {
                        space = rightSpace;
                    }
                } else {
                    if (rightSpace != null && !rightSpace.equals(Token.VIRTUAL_SPACE)) {
                        if (!space.equals(rightSpace)) { //both real space but different
                            space = space + rightSpace;
                        }
                    }
                }
            } else { // left:Word, right:Tag
/*
                right   right   right
                null    virt    real2
left    null    null    null    real2
left    virt    null    null    real2
left    real1   real1   real1   real2
*/
                if (space == null || space == Token.VIRTUAL_SPACE) {
                    if (rightSpace != null && !rightSpace.equals(Token.VIRTUAL_SPACE)) {
                        space = rightSpace;
                    } else {
                        space = null;
                    }
                } else {
                    if (rightSpace != null && !rightSpace.equals(Token.VIRTUAL_SPACE)) {
                        space = rightSpace;
                    }
                }
            }
        } else {
            if (rightIsWord) { // left:Tag, right:Word
/*
                right   right   right
                null    virt    real2
left    null    null    null    real2
left    virt    null    null    real2
left    real1   real1   real1   real1
 */
                if (space == null || space.equals(Token.VIRTUAL_SPACE)) {
                    if (rightSpace != null && !rightSpace.equals(Token.VIRTUAL_SPACE)) {
                        space = rightSpace;
                    } else {
                        space = null;
                    }
                }
            } else { // left:Tag, right:Tag
/*
                right   right   right
                null    virt    real2
left    null    null    null    real2
left    virt    null    null    real2
left    real1   real1   real1   real1+real2
*/
                if (space == null || space.equals(Token.VIRTUAL_SPACE)) {
                    if (rightSpace != null && !rightSpace.equals(Token.VIRTUAL_SPACE)) {
                        space = rightSpace;
                    } else {
                        space = null;
                    }
                } else {
                    if (rightSpace != null && !rightSpace.equals(Token.VIRTUAL_SPACE)) {
                        if (!space.equals(rightSpace)) { //both real spaces, but different
                            space = space + rightSpace;
                        }
                    }

                }
            }
        }

        return space;
    }

    private String toXMLStrippedString(boolean printPlaceholders) {
        StringBuilder builder = new StringBuilder();

        boolean foundFirstWord = false;
        boolean printSpace = false;

        Token previousToken = null;
        String middleSpace;
        String prevSpace = null;

        for (Token token : this) {
            middleSpace = previousToken != null ? getSpace(prevSpace, previousToken, token) : null;

            if (token instanceof Tag) {
                if (foundFirstWord && prevSpace == null) {
                    printSpace = true;
                }
                prevSpace = middleSpace;

                if (token instanceof WhitespaceTag) {
                    if (((WhitespaceTag) token).getName().equals(Sentence.TAB_PLACEHOLDER)) {
                        builder.append('\t');
                    } else if (((WhitespaceTag) token).getName().equals(Sentence.NL_PLACEHOLDER)) {
                        builder.append('\n');
                    } else {
                        builder.append(token.getText());
                    }
                }
            } else {
                if (middleSpace != null) {
                    if (foundFirstWord)
                        builder.append(middleSpace.equals(Token.VIRTUAL_SPACE) ? " " : middleSpace);
                } else {
                    if (printSpace && foundFirstWord)
                        builder.append(" ");
                }
                String text = printPlaceholders || !token.hasText() ? token.getPlaceholder() : token.getText();
                builder.append(text);

                previousToken = token;
                prevSpace = null;
                printSpace = false;
                foundFirstWord = true;
            }
        }

        return builder.toString();
    }

    private String toXMLString(boolean printPlaceholders) {
        StringBuilder builder = new StringBuilder();

        if (this.isSpaceConsistent()) {
            for (Token token : this) {
                String space = token.getLeftSpace();
                if (space != null) {
                    builder.append(space.equals(Token.VIRTUAL_SPACE) ? " " : space);
                }

                if (token instanceof WhitespaceTag) {
                    if (((WhitespaceTag) token).getName().equals(Sentence.TAB_PLACEHOLDER)) {
                        builder.append('\t');
                    } else if (((WhitespaceTag) token).getName().equals(Sentence.NL_PLACEHOLDER)) {
                        builder.append('\n');
                    } else {
                        builder.append(token.getText());
                    }
                } else if (token instanceof XMLTag) {
                    builder.append(token.getText());
                }  else {
                    String text = printPlaceholders || !token.hasText() ? token.getPlaceholder() : token.getText();
                    builder.append(XMLUtils.escapeText(text));
                }
            }
        } else {
            Token previousToken = null;
            for (Token token : this) {
                String space = previousToken != null ? getSpace(previousToken, token) : null;

                if (space != null) {
                    builder.append(space.equals(Token.VIRTUAL_SPACE) ? " " : space);
                }

                if (token instanceof WhitespaceTag) {
                    if (((WhitespaceTag) token).getName().equals(Sentence.TAB_PLACEHOLDER)) {
                        builder.append('\t');
                    } else if (((WhitespaceTag) token).getName().equals(Sentence.NL_PLACEHOLDER)) {
                        builder.append('\n');
                    } else {
                        builder.append(token.getText());
                    }
                } else if (token instanceof XMLTag) {
                    builder.append(token.getText());
                } else {
                    String text = printPlaceholders || !token.hasText() ? token.getPlaceholder() : token.getText();
                    builder.append(XMLUtils.escapeText(text));
                }

                previousToken = token;
            }
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
