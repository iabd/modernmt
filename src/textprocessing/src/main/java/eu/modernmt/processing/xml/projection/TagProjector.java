package eu.modernmt.processing.xml.projection;

import eu.modernmt.model.*;

import java.util.Arrays;


public class TagProjector {

    public Translation project(Translation translation) {
        Sentence source = translation.getSource();

        if (source.hasTags()) {
            TagCollection sourceTags = new TagCollection(source.getTags());

            if (source.hasWords()) {
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
                    simpleSpaceAnalysis(translation);
                }
            } else { //there are no source words; just copy the source tags in the target tags
                Tag[] copy = Arrays.copyOf(sourceTags.getTags(), sourceTags.size());
                translation.setTags(copy);
            }
        } else {
            simpleSpaceAnalysis(translation);
        }

        return translation;
    }

    public static void simpleSpaceAnalysis(Translation translation) {
        //Add whitespace between the last word and the next tag if the latter has left space
        Tag[] tags = translation.getTags();
        Word[] words = translation.getWords();
        int firstTagAfterLastWord = tags.length - 1;
        boolean found = false;
        while (firstTagAfterLastWord >= 0 && tags[firstTagAfterLastWord].getPosition() == words.length) {
            firstTagAfterLastWord--;
            found = true;
        }
        if (found) {
            firstTagAfterLastWord++;
            if (tags[firstTagAfterLastWord].hasLeftSpace() && !words[words.length - 1].hasRightSpace()) {
                words[words.length - 1].setRightSpace(" ");
            }
        }

        //TODO: revise this part according to the new meaning of rightSpace and leftSpace
        //Remove whitespace between word and the next tag, if the first has no right space.
        //Left trim first token if it is a tag
        Token previousToken = null;
        for (Token token : translation) {

            //Remove first whitespace
            if (previousToken == null) {
                token.setLeftSpace(null);
            }
            if (token instanceof Tag) {
                Tag tag = (Tag) token;
                if (previousToken != null && previousToken.hasRightSpace() && !tag.hasLeftSpace()) {
                    if (!tag.hasRightSpace()) {
                        tag.setRightSpace(previousToken.getRightSpace());
                    }
                    previousToken.setRightSpace(null);
                }
            }
            previousToken = token;

        }
        //Remove the last whitespace
        if (previousToken != null) {
            previousToken.setRightSpace(null);
        }

    }

}
