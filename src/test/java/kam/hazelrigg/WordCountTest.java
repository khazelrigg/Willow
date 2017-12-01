package kam.hazelrigg;


import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WordCountTest {
    private ExpectedException e = ExpectedException.none();

    private Properties props = new Properties(
            PropertiesUtils.asProperties(
                    "annotators", "tokenize, ssplit, pos, lemma"
                    , "options", "untokenizable=noneKeep"
                    , "tokenize.language", "en"));
    private StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

    @Test
    public void getsCorrectWordCounts() {
        String testString = "I eNjoy writing a lot of unit tests. Unit tests are a loT of fUn to make.";
        String expected = "lot:2|unit:2|tests:2|writing:1|enjoy:1|make:1|fun:1|";

        Book test = new Book();
        test.givePipeline(pipeline);
        test.tagText(testString);

        assertEquals("Failure creating word counts", test.wordFreq.getSimpleString(), expected);
    }

    @Test
    public void getsCorrectPOSTags() {
        String testString = "So they were trying to re-invent themselves and their universe."
                + " Science fiction was a big help.";
        String expected = "Noun, singular or mass:3|Verb, past tense:2|Personal pronoun:2|Proper noun, singular:1|Preposition or subordinating conjunction:1|Adjective:1|Coordinating conjunction:1|Determiner:1|Possessive pronoun:1|Verb, gerund or present participle:1|Verb, base form:1|to:1|";

        Book testBook = new Book();
        testBook.givePipeline(pipeline);
        testBook.tagText(testString);
        assertEquals(testBook.posFreq.getSimpleString(), expected);
    }

    @Test
    public void createsCorrectJSON() {
        File testf = null;
        try {
            testf = new File(this.getClass().getResource("/test.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        Book test = new Book();
        test.givePipeline(pipeline);
        test.setPath(testf);
        test.setTitleFromText(testf);
        test.readText();

        OutputWriter ow = new OutputWriter(test);
        String outputJson = "";
        try {
            outputJson = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/test_expectedJson.json"))).readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(outputJson, ow.writeJson());
    }

    @Test
    public void ignoresPunctuationInWordCount() {
        Book test = new Book();
        test.givePipeline(pipeline);
        test.tagText("My mother is a fish.");

        assertEquals(5, test.getWordCount());
    }

    @Test
    public void getsTitleFromText() {
        File testf = null;
        try {
            testf = new File(this.getClass().getResource("/test.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        Book test = new Book();
        test.setTitleFromText(testf);

        assertEquals("2 B R 0 2 B", test.getTitle());
    }

    @Test
    public void getsNameFromText() {
        File testf = null;
        try {
            testf = new File(this.getClass().getResource("/test.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        Book test = new Book();
        //If author and title are somehow set beforehand
        test.setAuthor("null");
        test.setTitle("null");
        test.setTitleFromText(testf);

        assertEquals("2 B R 0 2 B by Kurt Vonnegut", test.getName());
    }

    @Test
    public void nullReadFileTest() {
        e.expect(NullPointerException.class);
        Book test = new Book();
        test.readText();
    }

    @Test
    public void setTitleFakeFileTest() {
        e.expect(FileNotFoundException.class);
        Book test = new Book();
        File fake = new File("akljajdflj.oops");
        test.setTitleFromText(fake);
    }

    @Test
    public void shouldGetIsGutenberg() {
        File testf = null;
        try {
            testf = new File(this.getClass().getResource("/test.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        Book test = new Book();
        test.setTitleFromText(testf);

        assertTrue(test.isGutenberg());
    }

    @Test
    public void shouldGetIsNotGutenberg() {
        File testf = null;
        try {
            testf = new File(this.getClass().getResource("/test2.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Book test = new Book();
        test.setTitleFromText(testf);
        assertFalse(test.isGutenberg());
    }

    @Test
    public void shouldGetIsGutenbergVariant() {
        File testf = null;
        try {
            testf = new File(this.getClass().getResource("/test3.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        Book test = new Book();
        test.setTitleFromText(testf);

        assertTrue(test.isGutenberg());
    }

    @Test
    public void shouldReadText() {
        File testf = null;
        try {
            testf = new File(this.getClass().getResource("/test2.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Book test = new Book();
        test.setTitleFromText(testf);
        test.givePipeline(pipeline);
        test.setPath(testf);
        assertTrue(test.readText());
    }

    @Test
    public void shouldReadGutenbergText() {
        File testf = null;
        try {
            testf = new File(this.getClass().getResource("/test.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Book test = new Book();
        test.givePipeline(pipeline);
        test.setTitleFromText(testf);
        test.setPath(testf);
        assertTrue(test.readText() && test.isGutenberg());
    }


    @Test
    public void shouldGetResultsExist() {
        File testf = null;
        try {
            testf = new File(this.getClass().getResource("/test.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Book test = new Book();
        test.givePipeline(pipeline);
        test.setPath(testf);
        test.setTitleFromText(testf);
        test.readText();

        OutputWriter ow = new OutputWriter(test);
        ow.writeTxt();
        ow.makeDiffGraph();
        ow.makePosGraph();
        ow.writeJson();
        assertTrue(test.resultsFileExists());
    }

    @Test
    public void shouldBeMissingFile() {
        e.expect(NullPointerException.class);
        Book test = new Book();
        test.givePipeline(pipeline);
        test.setPath(new File("whoops, nothing here"));
        test.getPath();
        test.readText();
    }

    @Test
    public void subdirectoryBook() {
        Book test = new Book("dir");
        assertEquals(test.subdirectory, "dir");
    }

    @Test
    public void testUserInputDir() {
        String input = "Books/tests";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);

        WordCount.main(new String[0]);
    }

    @Test
    public void batchRunFakeDir() {
        e.expect(IOException.class);
        BatchRunner.startRunners(new File("thereisnofolderonacomputerthatwouldhavethisname"));
    }

    @Test
    public void runnerOverwriteTest() {
        Runner testRunner = new Runner(new File("Books/tests/test_speech.txt")
                , new File("Books/tests/test_speech.txt"), "Books/tests");
        testRunner.setOverwrite(true);
        testRunner.book.givePipeline(pipeline);
        testRunner.runBook();
    }


    @Test
    public void testOpenDirectory() {
        // Clear any old runners
        BatchRunner.runners = new ArrayList<>();
        BatchRunner.startRunners(new File("Books/tests"));
        assertEquals(BatchRunner.runners.size(), 2);
    }

    @Test
    public void testCommandlineDirArg() {
        BatchRunner.runners = new ArrayList<>();
        WordCount.main(new String[]{"Books/tests"});
        assertEquals(BatchRunner.runners.size(), 2);
    }

    @Test
    public void testCommandlineFileArg() {
        BatchRunner.runners = new ArrayList<>();
        WordCount.main(new String[]{"Books/tests/test_speech.txt"});
        assertEquals(WordCount.path.getName(), "test_speech.txt");
    }


}
