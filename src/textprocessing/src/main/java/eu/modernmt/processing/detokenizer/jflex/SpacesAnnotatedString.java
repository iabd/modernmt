package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;

import java.io.CharArrayReader;
import java.io.Reader;
import java.util.BitSet;

/**
 * Created by davide on 01/02/16.
 */
public class SpacesAnnotatedString {

    private char[] text;
    private BitSet bits;

    public static SpacesAnnotatedString fromSentence(Sentence sentence) {
        StringBuilder builder = new StringBuilder();
        builder.append(' ');

        for (Word word : sentence.getWords()) {
            builder.append(word.getPlaceholder());
            builder.append(' ');
        }

        char[] buffer = new char[builder.length()];
        builder.getChars(0, builder.length(), buffer, 0);

        return new SpacesAnnotatedString(buffer);
    }

    private SpacesAnnotatedString(char[] text) {
        this.text = text;
        this.bits = new BitSet(text.length);
    }

    public void removeSpaceRight(int position) {
        while (0 < position && position < text.length) {
            if (text[position] == ' ') {
                bits.set(position);
                break;
            }
            position++;
        }
    }

    public void removeSpaceLeft(int position) {
        while (0 < position && position < text.length) {
            if (text[position] == ' ') {
                bits.set(position);
                break;
            }
            position--;
        }
    }

    public void removeAllSpaces(int start, int end) {
        start = start < 0 ? 0 : start;
        end = end > text.length ? text.length : end;

        for (int i = start; i < end; i++) {
            if (text[i] == ' ')
                bits.set(i);
        }
    }

    public Reader getReader() {
        return new CharArrayReader(text);
    }

    public <S extends Sentence> S apply(S sentence, ApplyFunction leftFunction, ApplyFunction rightFunction) {
        int index = 1; // Skip first whitespace

        Word[] words = sentence.getWords();

        if (words.length == 0)
            return sentence;

        rightFunction.apply(words[0], false);
        for (int i = 1; i < words.length; i++) {
            Word leftWord = words[i-1];
            Word rightWord = words[i];
            String placeholder = leftWord.getPlaceholder();
            index += placeholder.length();

            System.out.println("leftWord:" + leftWord + " rightWord:" + rightWord + " index:" + index  + " bits.get(index):" + bits.get(index));
            leftFunction.apply(leftWord, !bits.get(index));
            rightFunction.apply(rightWord, !bits.get(index));
            index++;
        }
        
        leftFunction.apply(words[words.length-1], false);

        return sentence;
    }

    @Override
    public String toString() {
        return new String(text);
    }

    public interface ApplyFunction {

        void apply(Word word, boolean hasSpace);

    }
}
