package eu.modernmt.processing.detokenizer;

import eu.modernmt.lang.Language;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.detokenizer.jflex.JFlexDetokenizer;
import eu.modernmt.processing.detokenizer.jflex.JFlexSpaceAnnotator;
import eu.modernmt.processing.detokenizer.jflex.SpacesAnnotatedString;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by davide on 24/03/17.
 */
public class WhitespaceProjector extends TextProcessor<Translation, Translation> {

    private final JFlexSpaceAnnotator annotator;

    public WhitespaceProjector(Language sourceLanguage, Language targetLanguage) {
        super(sourceLanguage, targetLanguage);
        this.annotator = JFlexDetokenizer.newAnnotator(sourceLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        if (!translation.hasAlignment())
            return translation;

        Sentence source = applyAnnotator(translation.getSource());

        Word[] sourceWords = source.getWords();
        Word[] targetWords = translation.getWords();

        HashSet<AlignmentPoint> alignment = AlignmentPoint.parse(translation.getWordAlignment());

        AlignmentPoint probe = new AlignmentPoint();
        //consider consecutive word pairs translated in monotonic way
        for (AlignmentPoint point : alignment) {
            probe.source = point.source + 1;
            probe.target = point.target + 1;

            if (!alignment.contains(probe))
                continue;
            Word sourceWord = sourceWords[point.source];
            Word targetWord = targetWords[point.target];

            //project the space between under given conditiona
            if ((sourceWord.isRightSpaceRequired() && targetWord.hasRightSpace()) ||
                    (!sourceWord.isRightSpaceRequired() && sourceWord.hasRightSpace()))
                targetWord.setRightSpace(sourceWord.getRightSpace());

            //TODO: do we actually do this separately for right space of the left word
            //      and for the left space of the right word?
            //      It is possible use the same conditions for both the left and the right words

            sourceWord = sourceWords[probe.source];
            targetWord = targetWords[probe.target];

            //project the space between under given conditiona
            if ((sourceWord.isLeftSpaceRequired() && targetWord.hasLeftSpace()) ||
                    (!sourceWord.isLeftSpaceRequired() && sourceWord.hasLeftSpace()))
                targetWord.setLeftSpace(sourceWord.getLeftSpace());
        }

        return translation;
    }

    private Sentence applyAnnotator(Sentence sentence) throws ProcessingException {
        SpacesAnnotatedString text = SpacesAnnotatedString.fromSentence(sentence);

        annotator.reset(text.getReader());

        int type;
        while ((type = next(annotator)) != JFlexSpaceAnnotator.YYEOF) {
            annotator.annotate(text, type);
        }

        //use Word::setRightSpaceRequired for the left word, and use Word::setLeftSpaceRequired for the right word
        text.apply(sentence, Word::setRightSpaceRequired, Word::setLeftSpaceRequired);

        return sentence;
    }

    private static int next(JFlexSpaceAnnotator annotator) throws ProcessingException {
        try {
            return annotator.next();
        } catch (IOException e) {
            throw new ProcessingException(e);
        }
    }

    private static class AlignmentPoint {

        public int source;
        public int target;

        public static HashSet<AlignmentPoint> parse(Alignment alignment) {
            HashSet<AlignmentPoint> result = new HashSet<>(alignment.size());
            for (int[] point : alignment)
                result.add(new AlignmentPoint(point[0], point[1]));
            return result;
        }

        private AlignmentPoint() {
            this(0, 0);
        }

        private AlignmentPoint(int source, int target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AlignmentPoint that = (AlignmentPoint) o;

            if (source != that.source) return false;
            return target == that.target;
        }

        @Override
        public int hashCode() {
            int result = source;
            result = 31 * result + target;
            return result;
        }
    }

}
