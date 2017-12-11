package kam.hazelrigg;


import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import org.apache.commons.cli.Options;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WillowTest {
    final private ExpectedException e = ExpectedException.none();

    private static StanfordCoreNLP pipeline;
    private static Options options = new Options();

    @BeforeClass
    public static void setUp() {

        Properties props = new Properties(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize, ssplit, pos, lemma"
                        , "options", "untokenizable=noneKeep"
                        , "tokenize.language", "en"));
        pipeline = new StanfordCoreNLP(props);

        options.addOption("h", "help", false, "Print help")
                .addOption("v", "verbose", false, "Verbose output")
                //.addOption("w", "words", false, "Only analyse a text for word counts, cannot be used with -i or -j")
                .addOption("e", "economy", false, "Run in economy mode, greatly reduces memory usage at the cost of completion speed. Useful for computers with less memory")
                .addOption("i", "images", false, "Create image outputs")
                .addOption("j", "json", false, "Create JSON output")
                .addOption("c", "csv", false, "Create CSV output")
                .addOption("o", "overwrite", false, "Overwrite any existing results")
                .addOption("t", "threads", true, "Max number of threads to run, 0 = Use number of CPUs available; default = 0");


    }

    @Test
    public void getsCorrectWordCounts() {
        String testString = "I eNjoy writing a lot of unit tests. Unit tests are a loT of fUn to make.";
        String expected = "lot:2|unit:2|tests:2|writing:1|enjoy:1|make:1|fun:1|";

        Book test = new Book();
        test.givePipeline(pipeline);
        test.tagText(testString);
        BookStats stats = test.getStats();

        assertEquals("Failure creating word counts", stats.getWords().toSimpleString(), expected);
    }

    @Test
    public void getsCorrectPOSTags() {
        String testString = "So they were trying to re-invent themselves and their universe."
                + " Science fiction was a big help.";
        String expected = "Noun, singular or mass:3|Verb, past tense:2|Personal pronoun:2|Proper noun, singular:1|Preposition or subordinating conjunction:1|Adjective:1|Coordinating conjunction:1|Determiner:1|Possessive pronoun:1|Verb, gerund or present participle:1|Verb, base form:1|to:1|";

        Book testBook = new Book();
        testBook.givePipeline(pipeline);
        testBook.tagText(testString);
        BookStats stats = testBook.getStats();
        assertEquals(stats.getPartsOfSpeech().toSimpleString(), expected);
    }

    @Test
    public void createsCorrectCSV() {
        String testString = "So they were trying to re-invent themselves and their universe."
                + " Science fiction was a big help. They like science fiction";
        String expected = "\"fiction\", 2\n" +
                "\"science\", 2\n" +
                "\"big\", 1\n" +
                "\"like\", 1\n" +
                "\"re-invent\", 1\n" +
                "\"help\", 1\n" +
                "\"trying\", 1\n" +
                "\"universe\", 1\n";

        Book testBook = new Book();
        testBook.givePipeline(pipeline);
        testBook.tagText(testString);
        String resultCSV = new OutputWriter(testBook).writeCsv();
        assertEquals(resultCSV, expected);
    }

    @Test
    public void ignoresPunctuationInWordCount() {
        Book test = new Book();
        test.givePipeline(pipeline);
        test.tagText("My mother is a fish.");
        BookStats stats = test.getStats();

        assertEquals(5, stats.getWordCount());
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
        test.setTitleFromText(testf);

        assertEquals("2 B R 0 2 B by Kurt Vonnegut", test.getName());
    }

    @Test
    public void nullReadFileTest() {
        e.expect(NullPointerException.class);
        Book test = new Book();
        test.readText(false);
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
        File testFile = null;
        try {
            testFile = new File(this.getClass().getResource("/test2.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Book test = new Book();
        test.setPath(testFile);
        test.setTitleFromText(testFile);
        assertFalse(test.isGutenberg());
    }

    @Test
    public void shouldGetIsGutenbergVariant() {
        File testFile = null;
        try {
            testFile = new File(this.getClass().getResource("/test3.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        Book test = new Book();
        test.setTitleFromText(testFile);

        assertTrue(test.isGutenberg());
    }

    @Test
    public void shouldReadText() {
        File testFile = null;
        try {
            testFile = new File(this.getClass().getResource("/test2.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Book test = new Book();
        test.givePipeline(pipeline);
        test.setPath(testFile);
        assertTrue(test.readText(false));
    }

    @Test
    public void shouldReadGutenbergText() {
        File testFile = null;
        try {
            testFile = new File(this.getClass().getResource("/test.txt").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Book test = new Book();
        test.givePipeline(pipeline);
        test.setTitleFromText(testFile);
        test.setPath(testFile);
        assertTrue(test.isGutenberg());
    }

    @Test
    public void shouldBeMissingFile() {
        e.expect(NullPointerException.class);
        Book test = new Book();
        test.givePipeline(pipeline);
        test.setPath(new File("whoops, nothing here"));
        test.readText(false);
    }

    @Test
    public void createsSubdirectoryBook() {
        Book test = new Book("dir");
        assertEquals(test.getSubdirectory(), "dir");
    }


}
