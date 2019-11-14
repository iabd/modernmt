package eu.modernmt.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SentenceTest {

    @Test
    public void testCommonSentence() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", null, " "),
                new Word("world", " ", null),
                new Word("!", null, null),
        });

        assertEquals("Hello world!", sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
    }

    @Test
    public void testInitialTagWithSpace() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", " ", " "),
                new Word("world", " ", null),
                new Word("!", null, null),
        }, new Tag[]{
                Tag.fromText("<a>", null, " ", 0)
        });

        assertEquals("<a> Hello world!", sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
    }

    @Test
    public void testStrippedSentenceWithSpaceAfterTag() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", null, null),
                new Word("world", " ", null),
        }, new Tag[]{
                Tag.fromText("<a>", null, " ", 1)
        });

        assertEquals("Hello<a> world", sentence.toString(true, false));
        assertEquals("Hello world", sentence.toString(false, false));
    }

    @Test
    public void testStrippedSentenceWithSpacesBetweenTags() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", null, null),
                new Word("world", null, null),
        }, new Tag[]{
                Tag.fromText("<a>", null, " ", 1),
                Tag.fromText("<b>", " ", null, 1)
        });

        assertEquals("Hello<a> <b>world", sentence.toString(true, false));
        assertEquals("Hello world", sentence.toString(false, false));
    }

    @Test
    public void testStrippedSentenceWithoutSpacesBetweenTags() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", null, null),
                new Word("world", null, null),
        }, new Tag[]{
                Tag.fromText("<a>", null, null, 1),
                Tag.fromText("<b>", null, null, 1)
        });

        assertEquals("Hello<a><b>world", sentence.toString(true, false));
        assertEquals("Hello world", sentence.toString(false, false));
    }

}
