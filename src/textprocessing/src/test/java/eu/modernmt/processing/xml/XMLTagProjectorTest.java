package eu.modernmt.processing.xml;

import eu.modernmt.model.*;
import eu.modernmt.processing.xml.projection.TagProjector;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class XMLTagProjectorTest {

    @Test
    public void testOpeningNotEmptyMonotone() throws Throwable {
        //source:  "hello <b>world</b>!"
        //target:  "ciao <b>mondo</b>!"
        Sentence source = new Sentence(new Word[]{
                new Word("hello", null, " "),
                new Word("world", null, null),
                new Word("!", null, null),
        }, new Tag[]{
                Tag.fromText("<b>", " ", null, 1),
                Tag.fromText("</b>", null, null, 2),
        });

        Translation translation = new Translation(new Word[]{
                new Word("ciao", null, " "),
                new Word("mondo", null, null),
                new Word("!", null, null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
        }));

        new TagProjector().project(translation);

        assertEquals("ciao <b>mondo</b>!", translation.toString());
        assertEquals("ciao mondo !", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<b>", " ", null, 1),
                Tag.fromText("</b>", null, null, 2),
        }, translation.getTags());
    }

    @Test
    public void testOpeningNotEmptyNonMonotone() throws Throwable {
        //source:  "hello <b>world</b>!"
        //target:  "<b>mondo</b> ciao!"
        Sentence source = new Sentence(new Word[]{
                new Word("hello", null , " "),
                new Word("world", null, null),
                new Word("!", null, null),
        }, new Tag[]{
                Tag.fromText("<b>", " ", null, 1),
                Tag.fromText("</b>", null, null, 2),
        });

        Translation translation = new Translation(new Word[]{
                new Word("mondo", null, " "),
                new Word("ciao", " ", null),
                new Word("!", null, null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 1},
                {1, 0},
                {2, 2},
        }));

        new TagProjector().project(translation);

        assertEquals("<b>mondo</b> ciao!", translation.toString());
        assertEquals("mondo ciao!", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<b>", null, null, 0),
                Tag.fromText("</b>", null, " ", 1),
        }, translation.getTags());
    }

    @Test
    @Ignore
    public void testEmptyTag() throws Throwable {
        //source:  "Example with an <empty/>empty tag"
        //target:  "Esempio con un tag <empty/>empty"
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " "),
                new Word("with", " ", " "),
                new Word("an", " ", " "),
                new Word("empty", null, " "),
                new Word("tag", " ", null),
        }, new Tag[]{
                Tag.fromText("<empty/>", " ", null, 3),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " "),
                new Word("con", " ", " "),
                new Word("un", " ", " "),
                new Word("tag", " ", " "),
                new Word("empty", null, " "),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 1},
                {3, 4},
                {4, 3},
        }));

        new TagProjector().project(translation);

        assertEquals("Esempio con un tag <empty/>empty", translation.toString());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<empty/>", " ", null, 4),
        }, translation.getTags());
        assertEquals("Esempio con un tag empty", translation.toString(false, false));
    }

    @Test
    public void testOpeningEmptyMonotone() throws Throwable {
        //source:  "hello <g></g>world!"
        //target:  "ciao <g></g>mondo!"
        Sentence source = new Sentence(new Word[]{
                new Word("hello",null, " "),
                new Word("world", null, null),
                new Word("!", null, null),
        }, new Tag[]{
                Tag.fromText("<g>", " ", null, 1),
                Tag.fromText("</g>", null, null, 1),
        });

        Translation translation = new Translation(new Word[]{
                new Word("ciao", null, " "),
                new Word("mondo", null, null),
                new Word("!", null, null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
        }));

        new TagProjector().project(translation);

        assertEquals("ciao <g></g>mondo!", translation.toString());
        assertEquals("ciao mondo!", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<g>", " ", null, 1),
                Tag.fromText("</g>", null, null, 1),
        }, translation.getTags());
    }

    @Test
    @Ignore
    public void testOpeningEmptyNonMonotone() throws Throwable {
        //source:  "hello <g></g>world!"
        //target:  "<g></g>mondo ciao!"
        Sentence source = new Sentence(new Word[]{
                new Word("hello", null, " "),
                new Word("world", null, null),
                new Word("!", null, null),
        }, new Tag[]{
                Tag.fromText("<g>", " ", null, 1),
                Tag.fromText("</g>", null, null, 1),
        });

        Translation translation = new Translation(new Word[]{
                new Word("mondo", null, " "),
                new Word("ciao", " ", null),
                new Word("!", null, null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 1},
                {1, 0},
                {2, 2},
        }));

        new TagProjector().project(translation);
        //System.out.println(translation.getSource().toString());
        assertEquals("<g></g>mondo ciao!", translation.toString());
        assertEquals("mondo ciao!", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<g>", null, null, 0),
                Tag.fromText("</g>", null, null, 0),
        }, translation.getTags());
    }

    @Test
    public void testOpeningNonClosing() throws Throwable {
        //source:  "Example with <open>a malformed tag"
        //target:  "Esempio con <open>un tag malformato"
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " "),
                new Word("with", " ", " "),
                new Word("a", null, " "),
                new Word("malformed", " ", " "),
                new Word("tag", " ", null),
        }, new Tag[]{
                Tag.fromText("<open>", " ", null, 2),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " "),
                new Word("con", " ", " "),
                new Word("un", null, " "),
                new Word("tag", " ", " "),
                new Word("malformato", " ", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 4},
                {4, 3},
        }));

        new TagProjector().project(translation);

        assertEquals("Esempio con <open>un tag malformato", translation.toString());
        assertEquals("Esempio con un tag malformato", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<open>", " ", null, 2),
        }, translation.getTags());
    }

    @Test
    public void testClosingNonOpening() throws Throwable {
        //source:  "Example with</close> a malformed tag"
        //target:  "Esempio con</close> un tag malformato"
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " "),
                new Word("with", " ", " "),
                new Word("a", null, " "),
                new Word("malformed", " ", " "),
                new Word("tag", " ", null),
        }, new Tag[]{
                Tag.fromText("</close>", null, " ", 2),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " "),
                new Word("con", " ", null),
                new Word("un", " ", " "),
                new Word("tag", " ", " "),
                new Word("malformato", " ", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 4},
                {4, 3},
        }));

        new TagProjector().project(translation);

        assertEquals("Esempio con</close> un tag malformato", translation.toString());
        assertEquals("Esempio con un tag malformato", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("</close>", null, " ", 2),
        }, translation.getTags());
    }
/*
//TODO: revise this test
    @Test
    public void testEmbeddedTags() throws Throwable {
        //source:  "Example <a>with nested <b>tag</b></a>"
        //target:  "Esempio <a>con <b>tag</b> innestati</a>"
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " "),
                new Word("with", null," "),
                new Word("nested", " ", " "),
                new Word("tag", null, null),
        }, new Tag[]{
                Tag.fromText("<a>", " ", null, 1),
                Tag.fromText("<b>", " ", null, 3),
                Tag.fromText("</b>", null, null, 4),
                Tag.fromText("</a>", null, null, 4),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " "),
                new Word("con", null, " "),
                new Word("tag", null, null),
                new Word("innestati", " ",null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        }));

        new TagProjector().project(translation);

        assertEquals("Esempio <a>con <b>tag</b> innestati</a>", translation.toString());
        assertEquals("Esempio con tag innestati", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<a>", " ", null, 1),
                Tag.fromText("<b>", " ", null, 2),
                Tag.fromText("</b>", null, " ", 3),
                Tag.fromText("</a>", null, null, 4),
        }, translation.getTags());
    }
*/

    @Test
    public void testSpacedXMLCommentTags() throws Throwable {
        //source:  "Example with <!-- XML Comment -->"
        //target:  "Esempio con <!-- commenti XML -->"
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " "),
                new Word("with", " ", " "),
                new Word("XML", " ", " "),
                new Word("comment", " ", " "),
        }, new Tag[]{
                Tag.fromText("<!--", " ", " ", 2),
                Tag.fromText("-->", " ", null, 4),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " "),
                new Word("con", " ", " "),
                new Word("commenti", " ", " "),
                new Word("XML", " ", " "),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        }));

        new TagProjector().project(translation);

        assertEquals("Esempio con <!-- commenti XML -->", translation.toString());
        assertEquals("Esempio con commenti XML", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<!--", " ", " ", 2),
                Tag.fromText("-->", " ", null, 4),
        }, translation.getTags());
    }

    @Test
    public void testNotSpacedXMLCommentTags() throws Throwable {
        //source:  "Esempio con <!--commenti XML-->"
        //target:  "Esempio con commenti XML"
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " "),
                new Word("with", " ", " "),
                new Word("XML", null, " "),
                new Word("comment", " ", null),
        }, new Tag[]{
                Tag.fromText("<!--", " ", null, 2),
                Tag.fromText("-->", null, null, 4),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " "),
                new Word("con", " ", " "),
                new Word("commenti",null, " "),
                new Word("XML", " ", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        }));

        new TagProjector().project(translation);

        assertEquals("Esempio con <!--commenti XML-->", translation.toString());
        assertEquals("Esempio con commenti XML", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<!--", " ", null, 2),
                Tag.fromText("-->", null, null, 4),
        }, translation.getTags());
    }

    @Test
    public void testSingleXMLComment() throws Throwable {
        //source:  "<!--This ie a test-->"
        //target:  "<!--Questo è un esempio-->"
        Sentence source = new Sentence(new Word[]{
                new Word("This", null, " "),
                new Word("is", " ", " "),
                new Word("a", " ", " "),
                new Word("test", " " ,null),
        }, new Tag[]{
                Tag.fromText("<!--", null, null, 0),
                Tag.fromText("-->", null, null, 4),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Questo", null, " "),
                new Word("è", " ", " "),
                new Word("un", " ", " "),
                new Word("esempio", " ",null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 3},
        }));

        new TagProjector().project(translation);

        assertEquals("<!--Questo è un esempio-->", translation.toString());
        assertEquals("Questo è un esempio", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<!--", null, null, 0),
                Tag.fromText("-->", null, null, 4),
        }, translation.getTags());
    }

    @Test
    public void testDTDTags() throws Throwable {
        //source:  "<!ENTITY key="value"> Test"
        //target:  "<!ENTITY key="value"> Prova"
        Sentence source = new Sentence(new Word[]{
                new Word("Test", " ", null),
        }, new Tag[]{
                Tag.fromText("<!ENTITY key=\"value\">", null, " ", 0),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Prova", " ",null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0}
        }));

        new TagProjector().project(translation);

        assertEquals("Prova", translation.toString(false, false));
        assertEquals("<!ENTITY key=\"value\"> Prova", translation.toString());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<!ENTITY key=\"value\">", null, " ", 0),
        }, translation.getTags());
    }

    @Test
    public void testOnlyTags() throws Throwable {
        Sentence source = new Sentence(null, new Tag[]{
                Tag.fromText("<a>", null, null, 0),
                Tag.fromText("</a>", null, null, 0),
        });

        Translation translation = new Translation(null, source, null);

        new TagProjector().project(translation);

        assertEquals("<a></a>", translation.toString());
        assertTrue(translation.toString(false, false).isEmpty());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<a>", null, null, 0),
                Tag.fromText("</a>", null, null, 0),
        }, translation.getTags());
    }

}
