package eu.modernmt.processing.string;

import eu.modernmt.model.*;
import eu.modernmt.processing.xml.XMLCharacterEntity;

import java.util.*;

/**
 * Created by andrearossi on 22/02/17.
 * <p>
 * A SentenceBuilder handles most of the preprocessing activities of a string
 * and generates a Sentence object with the resulting Tokens.
 * <p>
 * The SentenceBuilder stores both
 * - the original version of the String, which is never altered
 * - the current version of the String, that can undergo changes (and is therefore implemented as a StringBuilder)
 * <p>
 * In order to perform String processing the SentenceBuilder employs one Editor,
 * that can update the current string by creating and committing Transformations.
 * The SentenceBuilder also has a Transformation list all Editors add their Transformations to when executing commit.
 * <p>
 * Moreover, the SentenceBuilder has a reference to a IndexMap object
 * that for each position on the local version of the string in the current Editor
 * contains the position on the correspondent character on the original string.
 * <p>
 * During the queue of the build() method the SentenceBuilder
 * uses Transformations to create Tokens, that are employed to generate a Sentence.
 * <p>
 * In order to save memory and time, during all preprocessing activities for all strings
 * one and only one SentenceBuilder object is used.
 * After the generation of the Sentence for the current string it is just cleared and re-initialized.
 */
public class SentenceBuilder {

    private final HashSet<String> annotations = new HashSet<>();
    /*original string to tokenize*/
    private String originalString;
    /*the string as it appears after the changes performed during last commit*/
    private StringBuilder currentString;
    /*ordered list that stores the Transformation committed by the editor so far*/
    private List<Transformation> sentenceBuilderTransformations;
    /*indexMap is an object that for each position in currentString
     * stores the corresponding position in the original string*/
    private IndexMap indexMap;
    /*The Editor is the one object that is allowed to commit Transformations
     * to the sentenceBuilder.
     * It is a singleton and it can never serve by more than one client at a time*/
    private final Editor editor = new Editor();


    /**
     * This constructor generates an empty SentenceBuilder,
     * with no information about the strings it is going to process.
     * <p>
     * This method should only be called by the Preprocessor object
     * once, at the beginning of its lifecycle, to create
     * the single SentenceBuilder instance that it will employ
     */
    public SentenceBuilder() {
        this.originalString = null;
        /*at the beginning no transformations have been performed*/
        this.currentString = new StringBuilder();
        /*list of transformation lists generated by editors*/
        this.sentenceBuilderTransformations = new ArrayList<>();
        /*initialize indexMap array that maps each position of the current string
         * to a position in the original string*/
        this.indexMap = new IndexMap();
    }

    /**
     * This constructor generates an empty SentenceBuilder
     * and performs initialization for a given string.
     *
     * @param string the original string that must be processed and tokenized
     */
    public SentenceBuilder(String string) {
        this();
        this.initialize(string);
    }

    /**
     * This method initializes a SentenceBuilder object
     * with information about one string that must be processed.
     *
     * @param string the original string that must be processed
     */
    public SentenceBuilder initialize(String string) {
        this.originalString = string;

        /*at the beginning no transformations have been performed*/
        this.currentString.setLength(0);
        this.currentString.append(string);

        /*list of transformation lists generated by editors*/
        this.sentenceBuilderTransformations.clear();

        /*initialize indexMap array that maps each position of the current string
         * to a position in the original string*/
        this.indexMap.initialize(originalString.length());

        /*collection of annotations*/
        this.annotations.clear();

        return this;
    }

    /**
     * This method resets the SentenceBuilder instance to
     * its initial state, and makes it ready to
     * be initialized again with another string to process.
     */
    public void clear() {
        this.originalString = null;

        /*at the beginning no transformations have been performed*/
        this.currentString.setLength(0);

        /*list of transformation lists generated by editors*/
        this.sentenceBuilderTransformations.clear();

        /*collection of annotations*/
        this.annotations.clear();
    }

    public void addAnnotation(String annotation) {
        this.annotations.add(annotation);
    }

    /**
     * Method that returns the Editor to process the current string version,
     * if it is not already in use.
     *
     * @return this SentenceBuilder editor
     */
    public Editor edit() {
        return this.editor.init();
    }

    /**
     * Method that scans all transformations committed by the editor;
     * it selects the non-in-conflict transformations with highest priority,
     * uses them to generate tokens (words and tags)
     * that are finally employed to create a Sentence object
     *
     * @return the Sentence resulting from the transformations on the original string
     */
    public Sentence build() {
        try {
            /*List of the highest priority transformation that do not overlap
             * and can therefore be all used to generate tokens*/
            List<Transformation> tokenizableTransformations = this.getTokenizableTransformations();

            /*Lists of words and tags generated by the tokenizable transformations*/
            TokenSet tokenSet = this.tokenize(tokenizableTransformations);
            List<Word> words = tokenSet.words;
            List<Tag> tags = tokenSet.tags;

            /*build the Sentence based on the words and tags lists */
            Sentence sentence = new Sentence(words.toArray(new Word[0]), tags.toArray(new Tag[0]));

            /*set sentence annotations*/
            if (!annotations.isEmpty())
                sentence.addAnnotations(annotations);

            return sentence;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to build sentence for string: " + originalString, e);
        }
    }


    /**
     * Method that scans backwards all the Transformations committed by the editor
     * and selects those that can be employed to generate tokens.
     * In case of conflict between two or more Transformations,
     * the Transformations with lower priority are filtered out.
     * The resulting Transformation list is sorted by increasing start position.
     * <p>
     * Note: replacements are considered as non tokenizable Transformations
     *
     * @return the list of high-priority, non overlapping transformations,
     * sorted by their start position on the original string
     */
    private List<Transformation> getTokenizableTransformations() {
        /*Create a bitset with as many bits as the positions in originalString
         * the bitset is employed to remember, for each position in the original string,
         * whether the corresponding character has been altered by a transformation or not*/
        BitSet bitset = new BitSet(this.originalString.length());

        /*List of priority, non overlapping tokenizable transformations extracted so far
         * from the SentenceBuilder list of transformation lists*/
        List<Transformation> tokenizableTransformations = new ArrayList<>();

        /*Scan the Transformation list backwards, from the last one to the first one*/
        for (int j = sentenceBuilderTransformations.size() - 1; j >= 0; j--) {

            Transformation t = sentenceBuilderTransformations.get(j);

            /*the current transformation is by default tokenizable*/
            boolean tokenizable = true;

            /* if the transformation has no tokenfactory, (e.g. it is a replacement)
             * it is not tokenizable and will be skipped*/
            if (t.tokenFactory == null)
                tokenizable = false;

            /*then, check if the transformation is in conflict with already visited ones.
             * A transformation is in conflict if some of its positions in the bitset
             * have already been set to true by other already visited transformations
             * (that have thus higher priority).
             * If this happens, the transformation is not tokenizable*/
            for (int h = t.start; h < t.end && tokenizable; h++) {
                tokenizable = !bitset.get(h);
            }

            /*finally, if the current transformation is still considered tokenizable,
             * it is added to the ordered list of tokenizable transformations*/
            if (tokenizable) {
                for (int h = t.start; h < t.end; h++) {
                    bitset.set(h, true);
                }
                tokenizableTransformations.add(t);
            }
        }


        /*the tokenizable transformations list is sorted by increasing start position*/
        tokenizableTransformations.sort(Comparator.comparingInt(o -> o.start));
        return tokenizableTransformations;
    }


    /**
     * Method that scans over all the tokenizable Transformations and
     * generates a Token object for each of them.
     *
     * @param transformations the list of all tokenizable Transformations,
     *                        sorted by increasing start position
     * @return a TokenSet containing a separate list of Tokens
     * for each Token type generated (e.g. words, tags, etc).
     */
    private TokenSet tokenize(List<Transformation> transformations) {

        /*get the original String as a char array*/
        char[] originalChars = this.originalString.toCharArray();

        /*list of words obtained by the tokenization process*/
        List<Word> words = new ArrayList<>();
        /*list of tags obtained by the tokenization process*/
        List<Tag> tags = new ArrayList<>();

        /*for each tokenizable transformation in list*/
        for (int i = 0; i < transformations.size(); i++) {

            Transformation transformation = transformations.get(i);

            /*extract the necessary information from the transformation*/
            String placeholderText = transformation.text;
            TokenFactory tokenFactory = transformation.tokenFactory;

            /*compute additional information about the way that
             the current transformation is linked to the previous and next ones in the list*/

            /*there is a space between previous transformation text and current one*/
            boolean hasLeftSpace;
            /*there is a space between current transformation text and next one*/
            boolean hasRightSpace;
            /*string with the space between current transformation text and previous one*/
            String leftSpace = null;
            /*string with the space between current transformation text and next one*/
            String rightSpace = null;
            /*amount of WORDS that occur before the current transformation*/
            int tagPosition;

            /*compute leftSpace*/
            /*if the current transformation is the first one in the list,
             * it has a leftSpace if it doesn't start at position 0 */
            if (i == 0) {
                hasLeftSpace = (transformation.start > 0);
                /*else, if the current transformation is not the first one in the list
                 * it has a leftSpace if it doesn't start at the end of its predecessor*/

                if (hasLeftSpace) {
                    /*unescape the leftSpace (see comment above to understand why)*/
                    leftSpace = XMLCharacterEntity.unescapeAll(new String(originalChars, transformation.end, transformation.start));
                }
            } else {
                Transformation previousTransformation = transformations.get(i - 1);
                hasLeftSpace = (transformation.start - previousTransformation.end > 0);

                if (hasLeftSpace) {
                    /*unescape the leftSpace (see comment above to understand why)*/
                    leftSpace = XMLCharacterEntity.unescapeAll(new String(originalChars, previousTransformation.end, transformation.start - previousTransformation.end));
                } else{
                    if ((transformation.tokenFactory == TokenFactory.WORD_FACTORY && previousTransformation.tokenFactory == TokenFactory.TAG_FACTORY) ||
                            (transformation.tokenFactory == TokenFactory.TAG_FACTORY && previousTransformation.tokenFactory == TokenFactory.WORD_FACTORY)) {
                        leftSpace = Token.VIRTUAL_SPACE;
                    }
                }
            }

            /*compute rightSpace*/
            /*if the current transformation is the last one in the list*/
            if (i == transformations.size() - 1) {
                hasRightSpace = transformation.end < originalChars.length;

                /*RightSpace can only be extracted by the original string,
                as the transformation indexes refer to positions in the original string.
                However the original string still has
                    - xml tags
                    - xml escaping sequences (e.g: &lt;, &gr;, &nbsp;, etc).
                    - rare chars
                    - whitespaces
                    - etc
                XML tags lead to the creation of new XMLTag Transformations that are now in the
                tokenizable transformations list, so an XML tag can't be in a rightSpace.

                Xml escaping sequences, rare chars and whitespaces on the contrary
                generate Replacement Transformations, that are not tokenizable
                so are not in the tokenizable list.
                While we are ok with having rare chars and whitespaces in the rightSpace,
                we still don't want XML escaping sequences.
                Therefore we unescape the rightSpace.*/
                if (hasRightSpace) {
                    /*unescape the rightSpace (see comment above to understand why)*/
                    rightSpace = XMLCharacterEntity.unescapeAll(new String(originalChars, transformation.end, originalChars.length - transformation.end));
                }
                /*if the current transformation is not the last one in the list*/
            } else {
                Transformation nextTransformation = transformations.get(i + 1);
                hasRightSpace = transformation.end < nextTransformation.start;

                if (hasRightSpace) {
                    /*unescape the rightSpace (see comment above to understand why)*/
                    rightSpace = XMLCharacterEntity.unescapeAll(new String(originalChars, transformation.end, nextTransformation.start - transformation.end));
                } else {
                    if ((transformation.tokenFactory == TokenFactory.WORD_FACTORY && nextTransformation.tokenFactory == TokenFactory.TAG_FACTORY) ||
                            (transformation.tokenFactory == TokenFactory.TAG_FACTORY && nextTransformation.tokenFactory == TokenFactory.WORD_FACTORY)) {
                        rightSpace = Token.VIRTUAL_SPACE;
                    }
                }
            }


            /*compute tagPosition*/
            /*the current tag position is the amount of words in the words list*/
            tagPosition = words.size();

            /*the original text is necessary to create the Token.
             However
                - If the token is a XMLTag, it surely does not require XML escaping
                - If it is a word, the original text may still contain
                       xml tags, xml escape sequences, rare chars and whitespaces
                       However it is impossible to have XML tags (they would lead to a XMLTag Token)
                       We are ok with whitespaces and rare chars
                       We still need xml escaping
                  Therefore, if we are creating a Word Token, unescape the originalText.
                 */
            String originalText;
            if (tokenFactory == TokenFactory.TAG_FACTORY || tokenFactory == TokenFactory.WHITESPACE_TAG_FACTORY ) {
                originalText = new String(originalChars, transformation.start, transformation.end - transformation.start);
            } else {
                //originalText = XMLCharacterEntity.unescapeAll(originalChars, transformation.start, transformation.end - transformation.start);
                originalText = XMLCharacterEntity.unescapeAll(new String(originalChars, transformation.start, transformation.end - transformation.start));
            }

            /*generate the Token*/
            Token token = tokenFactory.build(originalText, placeholderText, leftSpace, rightSpace, tagPosition);

            /*put the token in the separate list corresponding to its class*/
            if (token instanceof XMLTag || token instanceof WhitespaceTag) {
                tags.add((Tag) token);
            } else if (token instanceof Word) {
                words.add((Word) token);
            }
        }

        if (!tags.isEmpty()) {
            int currentTagIdx = 0;

            for (int wordPos = 0; wordPos < words.size(); wordPos++) {
                while (currentTagIdx < tags.size()) {
                    Tag currentTag = tags.get(currentTagIdx);
                    if (currentTag.getPosition() > wordPos+1) {
                        break;
                    }

                    if (currentTag.hasRightSpace()) {
                        Word word = words.get(wordPos);
                        word.setTagRightSpaceRequired(true);
                    }

                    currentTagIdx++;
                }
            }
        }

        /*create and return a new tokenset with the various token lists obtained*/
        return new TokenSet(words, tags);
    }



    /*getters and setters*/

    public String getOriginalString() {
        return this.originalString;
    }

    @Override
    public String toString() {
        return this.currentString.toString();
    }

    public char[] toCharArray() {
        int l = currentString.length();
        char[] buffer = new char[l];
        currentString.getChars(0, l, buffer, 0);

        return buffer;
    }


    /**
     * An Editor is an object that scans the SentenceBuilder current version of the string
     * in order to perform processing activities
     * and create corresponding Transformations to keep track of them.
     * <p>
     * Given one SentenceBuilder, its editor is always one and the same,
     * and it can't accept requests from more clients at a time.
     * The editor is "freed" from its client when it is asked to perform commit,
     * and it submits all the Transformations it has created to the SentenceBuilder.
     * It is then ready to accept another client.
     * <p>
     * An Editor can create Transformations to
     * - generate Tokens like WORDS and TAGS;
     * - to perform string replacements on the SentenceBuilder current String.
     * If a Transformation involves Token generation it may or may not involve replacements;
     * however, a Transformation may also be a simple replacement without Tokens to generate.
     * <p>
     * Token generation is executed by the SentenceBuilder when it runs its build() method.
     * On the contrary, replacements should be performed directly by the Editor
     * before the next client gets to see the currentString.
     * Therefore, during commit(), the Editor scans its newly generated Transformations
     * and if they involve replacements it applies them to the current String.
     * <p>
     * During the queue of the commit method, the Editor
     * - scans all the transformations in the local list
     * - applies replacements to the current String
     * - updates the start and end indexes of the transformation,
     * to make them match the right position in the original string, not the current one
     * - adds all the transformations to the SenteceBuilder transformations list
     * - gets ready for serving a new client.
     */
    public class Editor {
        private final List<Transformation> localTransformations = new ArrayList<>();
        private boolean inUse = false;

        /**
         * Constructor for the Editor for this SenteceBuilder;
         * Since the Editor is a singleton, this method is only used once.
         */
        public Editor() {
        }

        /**
         * Method to initialize the Editor:
         * if the Editor is not already in use,
         * it gets ready to serve a new client
         * and updates its state to "in use".
         * <p>
         * Otherwhise, it throws an IllegalStateException.
         *
         * @return a reference to the Editor itself, now marked as in use.
         */
        public Editor init() {

            if (this.inUse) {
                throw new IllegalStateException("this Editor is already in use");
            }

            this.localTransformations.clear();
            this.inUse = true;

            return this;
        }

        /**
         * This method handles a string processing requested to the Editor.
         * It includes the start and end indexes of the target text on the current String,
         * a replacement string (null is no replacement is involved in the Transformation),
         * and a reference to the token factory to use during build (null for simple replacements).
         * <p>
         * The Editor now proceeds to create a Transformation object
         * with indexes referring to the currentString in the SentenceBuilder.
         * and stores it in the Transformation in a local list.
         * <p>
         * Potential replacements are not executed contextually to the SetTransformation method;
         * they are handled during the commit method instead.
         * <p>
         *
         * @param currentStart first position of the text to edit in the current String
         * @param length       length of the text to edit
         * @param replacement  string that must substitute the text to edit.
         *                     If no replacement is needed, this parameter is null.
         * @param factory:     object that can generate tokens; depending on the kind of transformation
         *                     it can be specialized for words, tags, etc.
         *                     If no token must be generated by this transformation, this parameter is null.
         * @throws UnsupportedOperationException if the requested processing involves a replacement with ''
         *                                       in the middle of the string
         */
        private void setTransformation(int currentStart, int length, String replacement, TokenFactory factory) {

            /*the end of the text that is target to this Transformation*/
            int currentEnd = currentStart + length;

            /*check if the transformation involves an empty replacement in the middle of the string*/
            if (replacement != null && replacement.length() == 0) {
                if (!(currentStart == 0 || currentEnd == currentString.length()))
                    throw new UnsupportedOperationException("Empty replacements not yet supported in the middle of the sencence");
            }

            /*the text that is target to this transformation*/
            String text = currentString.substring(currentStart, currentEnd);

            /*create the transformation using the positions in the original string*/
            Transformation transformation = new Transformation(
                    currentStart,
                    currentEnd,
                    text,
                    replacement,
                    factory);

            this.localTransformations.add(transformation);
        }

        /**
         * This method handles the specific request of a simple replacement Transformation.
         * Therefore it just invokes the setting of a new Transformation object
         * sending it a TokenFactory field equal to null.
         * <p>
         * Since we are explicitly generating a replacement, the replacement field cannot be null.
         *
         * @param curStartIndex first position of the text to edit in the current string.
         * @param textLength    length of the text to process.
         * @param replacement   string that must substitute the text to edit. It can not be null.
         */
        public void replace(int curStartIndex, int textLength, String replacement) {
            if (replacement == null)
                throw new IllegalArgumentException("when invoking replace, the replacement must not be null");

            /*create the Transformation, put it in the Editor Transformations list*/
            setTransformation(curStartIndex, textLength, replacement, null);
        }


        public void delete(int curStartIndex, int textLength) {
            this.setTransformation(curStartIndex, textLength, "", null);
        }

        /**
         * This method handles the generic request of a Token generation.
         * Therefore it just invokes the setting of a new Transformation object
         * Since we explicitly want to generate a token, the tokenFactory field cannot be null.
         *
         * @param startIndex  first position of the text to edit in the current string
         * @param textLength  length of the text to edit
         * @param replacement string that must substitute the text to edit.
         * @param factory     object that can be employed to create Tokens. It can not be null.
         */
        public void setToken(int startIndex, int textLength, String replacement, TokenFactory factory) {
            if (factory == null)
                throw new IllegalArgumentException("when invoking setToken, the tokenFactory must not be null");

            /*create the Transformation, put it in the Editor Transformations list*/
            setTransformation(startIndex, textLength, replacement, factory);
        }

        /**
         * This method handles the specific request of a WORD Token.
         * It thus requests the setting of a new Token,
         * passing the specific TokenFactory WORD_FACTORY that is used
         * to generate WORD Tokens.
         *
         * @param startIndex  first position of the text to edit in the current string
         * @param length      length of the text to edit
         * @param replacement string that must substitute the text to edit.
         */
        public void setWord(int startIndex, int length, String replacement) {
            /*create the Transformation, put it in the Editor Transformations list;
             * as a TokenFactory use a WORD_FACTORY*/
            this.setToken(startIndex, length, replacement, TokenFactory.WORD_FACTORY);
        }

        /**
         * This method handles the specific request of a TAG Token.
         * It thus requests the setting of a new Token,
         * passing the specific TokenFactory TAG_FACTORY that is used
         * to generate TAG Tokens.
         *
         * @param startIndex  first position of the text to edit in the current string
         * @param length      length of the text to edit
         * @param replacement string that must substitute the text to edit.
         */
        public void setTag(int startIndex, int length, String replacement) {
            /*create the Transformation, put it in the Editor Transformations list;
             * as a TokenFactory use a TAG_FACTORY*/

            this.setToken(startIndex, length, replacement, TokenFactory.TAG_FACTORY);
        }

        /**
         * This method handles the specific request of a TAG Token.
         * It thus requests the setting of a new Token,
         * passing the specific TokenFactory TAG_FACTORY that is used
         * to generate TAG Tokens.
         *
         * @param startIndex  first position of the text to edit in the current string
         * @param length      length of the text to edit
         * @param replacement string that must substitute the text to edit.
         */
        public void setWhitespaceTag(int startIndex, int length, String replacement) {
            /*create the Transformation, put it in the Editor Transformations list;
             * as a TokenFactory use a WHITESPACE_TAG_FACTORY*/

            this.setToken(startIndex, length, replacement, TokenFactory.WHITESPACE_TAG_FACTORY);
        }

        /**
         * This method ends this Editor's scan on the current version of the String.
         * <p>
         * The Editor iterates over all the Transformations it has created
         * during the current scan over the current version of the String.
         * For each Transformation it executes replacements on the currentString, if required;
         * moreover, it updates the Transformation indexes
         * in order to make them refer to the original (immutable) string,
         * instead of the continuously modified currentString.
         * <p>
         * More in detail, for each Transformation
         * replacements lead to alter the currentString, so the next transformation indexes
         * won't refer to the same positions and characters anymore.
         * Therefore, after each replacement the Editor updates an offset field
         * that, added to the not updated start and end of the next transformation,
         * will lead to the start and end on the just updated currentString.
         * <p>
         * Moreover, the Editor also uses and, after each replacement, updates,
         * the SentenceBuilder IndexMap mapping each position on the currentString
         * to the corresponding position on the originalString.
         * <p>
         * Therefore the Editor can easily set the start and end fields of the transformation
         * so that they refer to the right positions on the original String
         * instead of on the current one.
         * <p>
         * The Editor finally sends all its transformations to the SentenceBuilder,
         * and prepares for new scan.
         */
        public SentenceBuilder commit() {

            /*distance between the current String and the original String transformation*/
            int offset = 0;

            /*executes replacements*/
            for (Transformation t : this.localTransformations) {

                /*computes the transformation start on the currentString*/
                int currentStart = t.start + offset;
                /*computes the transformation end on the currentString*/
                int currentEnd = t.end + offset;
                /*computes the transformation start on the originalString*/
                t.start = indexMap.get(currentStart);
                /*computes the transformation end on the originalString*/
                t.end = indexMap.get(currentEnd);

                /*process replacement, if necessary*/
                if (t.replacement != null) {
                    /*replace the symbols on the currentString*/
                    currentString.replace(currentStart, currentEnd, t.replacement);
                    /*update the mapping information in the indexMap:
                     * the portion from currentStart to currentEnd
                     * has become t.replacement.length() long*/
                    indexMap.update(currentStart, currentEnd, t.replacement.length());
                    /*since the replacement was now completed,
                     * the following transformation must now use a different offset*/
                    offset = offset - t.text.length() + t.replacement.length();
                }
            }
            /*add this Editor's transformation list to the SentenceBuilder Transformation lists*/
            sentenceBuilderTransformations.addAll(this.localTransformations);

            /*make the editor ready to start over with a new client*/
            localTransformations.clear();
            this.inUse = false;

            return SentenceBuilder.this;
        }

        /**
         * Since before commit() no replacements are executed
         * and no Transformations are employed,
         * there is no operation to rollback.
         * All that this method must do is prepare the editor
         * for the next client, by clearing the transformation list
         * and by marking the editor as free from clients.
         */
        public void abort() {
            /*make the editor ready to start over*/
            localTransformations.clear();
            this.inUse = false;
        }
    }
}