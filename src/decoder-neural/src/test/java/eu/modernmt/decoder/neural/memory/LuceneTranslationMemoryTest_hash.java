package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.neural.memory.lucene.DocumentBuilder;
import eu.modernmt.decoder.neural.memory.lucene.query.DefaultQueryBuilder;
import eu.modernmt.memory.ScoreEntry;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static eu.modernmt.decoder.neural.memory.TestData.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by davide on 03/08/17.
 */
public class LuceneTranslationMemoryTest_hash {

    private TLuceneTranslationMemory memory;

    @Before
    public void setup() throws Throwable {
        this.memory = new TLuceneTranslationMemory();
    }

    @After
    public void teardown() throws Throwable {
        this.memory.close();
        this.memory = null;
    }

    @Test
    public void queryWithMisleadingHashes() throws Throwable {
        IndexWriter indexWriter = memory.getIndexWriter();
        indexWriter.addDocument(DocumentBuilder.newInstance(EN__IT, 2, "2-1", "2-1", "A B C D"));
        indexWriter.addDocument(DocumentBuilder.newInstance(EN__IT, 1, "1-1", "1-1", "A B C D"));
        indexWriter.addDocument(DocumentBuilder.newInstance(EN__FR, 1, "1-1F", "1-1F", "A B C D"));
        indexWriter.addDocument(DocumentBuilder.newInstance(EN__IT, 1, "1-2", "1-2", "D C B A"));
        indexWriter.addDocument(DocumentBuilder.newInstance(EN__IT, 1, "1-3", "1-3", "D C B Z"));
        indexWriter.commit();

        Query query = new DefaultQueryBuilder().getByHash(1, "A B C D");

        IndexSearcher searcher = memory.getIndexSearcher();
        ScoreDoc[] result = searcher.search(query, 10).scoreDocs;

        assertEquals(2, result.length);

        ScoreEntry e1 = DocumentBuilder.asEntry(searcher.doc(result[0].doc));
        ScoreEntry e2 = DocumentBuilder.asEntry(searcher.doc(result[1].doc));

        if ("fr".equals(e1.language.target.getLanguage())) {
            assertArrayEquals(new String[]{"1-1F"}, e1.sentence);
            assertArrayEquals(new String[]{"1-1"}, e2.sentence);
        } else {
            assertArrayEquals(new String[]{"1-1F"}, e2.sentence);
            assertArrayEquals(new String[]{"1-1"}, e1.sentence);
        }
    }

    @Test
    public void overwriteNotExisting() throws Throwable {
        TranslationUnit original = tu(0, 0L, 1L, EN__IT, "hello world", "ciao mondo", null);
        memory.onDataReceived(Collections.singletonList(original));

        TranslationUnit overwrite = tu(0, 1L, 1L, EN__IT, "test sentence", "frase di prova",
                "hello world __", "ciao mondo __", null);
        memory.onDataReceived(Collections.singletonList(overwrite));

        Set<ScoreEntry> expectedEntries = TLuceneTranslationMemory.asEntrySet(Arrays.asList(original, overwrite));

        assertEquals(expectedEntries, memory.entrySet());
    }

    @Test
    public void overwriteExisting() throws Throwable {
        TranslationUnit original = tu(0, 0L, 1L, EN__IT, "hello world", "ciao mondo", null);
        memory.onDataReceived(Collections.singletonList(original));

        TranslationUnit overwrite = tu(0, 1L, 1L, EN__IT, "test sentence", "frase di prova",
                "hello world", "ciao mondo", null);
        memory.onDataReceived(Collections.singletonList(overwrite));

        Set<ScoreEntry> expectedEntries = TLuceneTranslationMemory.asEntrySet(Collections.singletonList(overwrite));

        assertEquals(expectedEntries, memory.entrySet());
    }
}
