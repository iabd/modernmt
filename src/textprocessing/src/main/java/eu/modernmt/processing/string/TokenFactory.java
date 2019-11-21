package eu.modernmt.processing.string;

import eu.modernmt.model.XMLTag;
import eu.modernmt.model.Token;
import eu.modernmt.model.WhitespaceTag;
import eu.modernmt.model.Word;

/**
 * Created by andrea on 22/02/17.
 * <p>
 * A TokenFactory is an object that is able to build Tokens.
 * Each type of Token requires a specific implementation of TokenFactory.
 */
public interface TokenFactory {

    /**
     * A WORD_FACTORY is an implementation of Token Factory that creates Words
     */
    TokenFactory WORD_FACTORY = new TokenFactory() {
        @Override
        public Token build(String text, String placeholder, String leftSpace, String rightSpace, int position) {
            return new Word(text, placeholder, leftSpace, rightSpace);
        }

        @Override
        public String toString() {
            return "Word Factory";
        }
    };

    /**
     * A TAG_FACTORY is an implementation of Token Factory that creates Tags
     */
    TokenFactory TAG_FACTORY = new TokenFactory() {
        @Override
        public XMLTag build(String text, String placeholder, String leftSpace, String rightSpace, int position) {
            return XMLTag.fromText(text, leftSpace, rightSpace, position);
        }

        @Override
        public String toString() {
            return "XMLTag Factory";
        }
    };

    /**
     * A WHITESPACE TAG_FACTORY is an implementation of Token Factory that creates WhitespaceTags
     */
    TokenFactory WHITESPACE_TAG_FACTORY = new TokenFactory() {
        @Override
        public WhitespaceTag build(String text, String placeholder, String leftSpace, String rightSpace, int position) {
            return WhitespaceTag.fromText(text, leftSpace, rightSpace, position);
        }

        @Override
        public String toString() {
            return "Whitespace XMLTag Factory";
        }
    };

    /**
     * Method that builds a Token object
     *
     * @param leftSpace    String conveying the space between the new Token and the previous one
     * @param rightSpace   String conveying the space between the new Token and the next one
     * @param text         String with the text target of the Transformation for this Token
     * @param position     int conveying the amount of WORD tokens already created (it is meaningful only if the token to create is a TAG
     * @return the newly created Token
     */
    Token build(String text, String placeholder, String leftSpace, String rightSpace, int position);

}
