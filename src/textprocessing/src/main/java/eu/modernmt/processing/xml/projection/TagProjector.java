package eu.modernmt.processing.xml.projection;

import eu.modernmt.model.*;

import java.util.Arrays;


public class TagProjector {

    public Translation project(Translation translation) {
        Sentence source = translation.getSource();

        if (source.hasWords()) {
            if (source.hasTags()) {
                TagCollection sourceTags = new TagCollection(source.getTags());
                if (translation.hasAlignment()) {
                    sourceTags.fixXmlCompliance();

                    Word[] sourceWords = source.getWords();
                    Word[] translationWords = translation.getWords();
                    TagCollection translationTags = new TagCollection();
                    SpanCollection sourceSpans = new SpanCollection(sourceTags.getTags(), sourceWords.length);

                    SpanTree sourceTree = new SpanTree(sourceSpans);
                    sourceTree.create();

                    Alignment alignment = new Alignment(translation.getWordAlignment(), sourceWords.length, translationWords.length);

                    SpanCollection translationSpans = new SpanCollection();
                    translationSpans.project(sourceSpans, alignment, translationWords.length);

                    SpanTree translationTree = new SpanTree(translationSpans);
                    translationTree.project(sourceTree, alignment, translationWords.length);

                    translationTags.populate(translationTree);

                    translation.setTags(translationTags.getTags());
                }
            }
            simpleSpaceAnalysis(translation);
        } else {
            if (source.hasTags()) {
                TagCollection sourceTags = new TagCollection(source.getTags());
                Tag[] copy = Arrays.copyOf(sourceTags.getTags(), sourceTags.size());
                translation.setTags(copy);
            }
        }

        return translation;
    }

    public static void simpleSpaceAnalysis(Translation translation) {
        //make left and right spaces identical and coherent with the priority policy established
        Token previousToken = null;
        for (Token token : translation) {
            //Remove whitespace on the left of the first token
            if (previousToken == null) {
                token.setLeftSpace(null);
            } else {
                String leftSpace = previousToken.getRightSpace();
                String rightSpace = token.getLeftSpace();
                String space = Sentence.getSpace(leftSpace, rightSpace, previousToken instanceof Word, token instanceof Word);

                previousToken.setRightSpace(space);
                token.setLeftSpace(space);
            }
            previousToken = token;
        }
        //Remove whitespace on the right side of the last token
        if (previousToken != null) {
            previousToken.setRightSpace(null);
        }
        translation.setSpaceConsistent(true);

    }

}
