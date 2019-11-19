package eu.modernmt.processing.normalizers;

import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.model.Sentence;

import java.util.Map;

public class SpacePlaceholderEncoder extends TextProcessor<String, String> {

    @Override
    public String call(String string, Map<String, Object> metadata) throws ProcessingException {

        return compile(string);
    }

    private String compile(String string) {
        StringBuilder result = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (tabPlaceholding(c)) {
                String placeholder = "<" + Sentence.TAB_PLACEHOLDER + ">";
                result.append(placeholder);
            } else if (nlPlaceholding(c)) {

                String placeholder = "<" + Sentence.NL_PLACEHOLDER + ">";
                result.append(placeholder);
            }
            else {
                result.append(string.charAt(i));
            }
        }
        return result.toString();
    }

    public static boolean tabPlaceholding(char c) {
        return c == '\t';
    }

    public static boolean nlPlaceholding(char c) {
        return c == '\n';
    }

}