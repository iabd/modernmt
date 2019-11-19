package eu.modernmt.processing.xml;

import eu.modernmt.model.Tag;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.xml.projection.TagProjector;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class XMLTagProjectorSpacingTest {

    private static Translation translation(Word[] tokens, Tag[] tags) {
        return new Translation(tokens, tags, null, null);
    }

    @Test
    public void testTranslationWithTags() {
        Translation translation = translation(new Word[]{
                new Word("Hello", null, " "),
                new Word("world", null, null),
                new Word("!", null, null),
        }, new Tag[]{
                Tag.fromText("<a>", " ", null, 1),
                Tag.fromText("</a>", null, null, 2),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello <a>world</a>!", translation.toString());
        assertEquals("Hello world !", translation.toString(false, false));
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    @Ignore
    public void testTranslationWithDiscordantTagSpacing_FalseTrue() {
        Translation translation = translation(new Word[]{
                new Word("Hello", null, " "),
                new Word("world", " ", null),
                new Word("!", null, null),
        }, new Tag[]{
                Tag.fromText("<a>", " ", null, 1),
                Tag.fromText("</a>", null, null, 2),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world !", translation.toString(false, false));
        assertEquals("Hello <a>world</a>!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithDiscordantTagSpacing_TrueFalse() {
        Translation translation = translation(new Word[]{
                new Word("Hello", null, " "),
                new Word("world", null, null),
                new Word("!", " ", null),
        }, new Tag[]{
                Tag.fromText("<a>", " ", null, 1),
                Tag.fromText("</a>", null, " ", 2),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world !", translation.toString(false, false));
        assertEquals("Hello <a>world</a> !", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedTagList() {
        Translation translation = translation(new Word[]{
                new Word("Hello", null, " "),
                new Word("world", " ", null),
                new Word("!", null, null),
        }, new Tag[]{
                Tag.fromText("<a>", " ", " ", 1),
                Tag.fromText("<b>", " ", " ", 1),
                Tag.fromText("</a>", " ", " ", 1),
                Tag.fromText("</b>", " ", " ", 1),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world!", translation.toString(false, false));
        assertEquals("Hello <a> <b> </a> </b> world!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithTagListSpaceInMiddle() {
        Translation translation = translation(new Word[]{
                new Word("Hello", null, " "),
                new Word("world", " ", null),
                new Word("!", null, null),
        }, new Tag[]{
                Tag.fromText("</a>", " ", " ", 1),
                Tag.fromText("<b>", " ", " ", 1),
                Tag.fromText("</b>", " ", " ", 1),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world!", translation.toString(false, false));
        assertEquals("Hello </a> <b> </b> world!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    @Ignore
    public void testTranslationWithTagInUnbreakableTokenList() {
        Translation translation = translation(new Word[]{
                new Word("That", null, null),
                new Word("'s", null, null),
                new Word("it", " ", null),
                new Word("!", null, null),
        }, new Tag[]{
                Tag.fromText("<b>", null, null, 1),
                Tag.fromText("</b>", null, null, 2),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("That 's it!", translation.toString(false, false));
        assertEquals("That<b>&apos;s</b> it!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedCommentTag() {
        Translation translation = translation(new Word[]{
                new Word("This", null, " "),
                new Word("is", " ", " "),
                new Word("XML", " ", " "),
                new Word("comment", " ", " "),
        }, new Tag[]{
                Tag.fromText("<!--", " ", " ", 2),
                Tag.fromText("-->", " ", null, 4),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("This is XML comment", translation.toString(false, false));
        assertEquals("This is <!-- XML comment -->", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedCommentTag_NoLeadingSpace() {
        Translation translation = translation(new Word[]{
                new Word("This", null, " "),
                new Word("is", " ", null),
                new Word("XML", " ", " "),
                new Word("comment", " ", " "),
        }, new Tag[]{
                Tag.fromText("<!--", null, " ", 2),
                Tag.fromText("-->", " ", null, 4),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("This is XML comment", translation.toString(false, false));
        assertEquals("This is<!-- XML comment -->", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedCommentTag_TrailingSpace() {
        Translation translation = translation(new Word[]{
                new Word("This", null, " "),
                new Word("is", " ", null),
                new Word("XML", " ", " "),
                new Word("comment", " ", " "),
        }, new Tag[]{
                Tag.fromText("<!--", null, " ", 2),
                Tag.fromText("-->", " ", " ", 4),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("This is XML comment", translation.toString(false, false));
        assertEquals("This is<!-- XML comment -->", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

}
