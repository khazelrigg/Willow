package kam.hazelrigg;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import org.apache.commons.cli.Options;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WillowTest {
    final private ExpectedException e = ExpectedException.none();

    private static StanfordCoreNLP pipeline;
    private static final Options options = new Options();

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
    public void ignoresPunctuationInWordCount() {
        Book test = new Book();
        test.givePipeline(pipeline);
        test.tagText("My mother is a fish.");
        BookStats stats = test.getStats();

        assertEquals(5, stats.getWordCount());
    }

    @Test
    public void getsTitleFromText() {
        Book test = getTestBook();
        assertEquals("2 B R 0 2 B", test.getTitle());
    }

    @Test
    public void getsNameFromText() {
        Book test = getTestBook();

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
        Path fakePath = Paths.get("thiscouldntbearealfileifitwantedtobe.okay");
        test.setTitleFromText(fakePath);
    }

    @Test
    public void shouldGetIsGutenberg() {
        Book test = getTestBook();
        assertTrue(test.isGutenberg());
    }

    @Test
    public void shouldGetIsNotGutenberg() {
        Book test = getTestBook("test2.txt");
        assertFalse(test.isGutenberg());
    }

    @Test
    public void shouldGetIsGutenbergVariant() {
        Book test = getTestBook("/test3.txt");
        assertTrue(test.isGutenberg());
    }

    @Test
    public void shouldReadText() {
        Book test = getTestBook();
        assertTrue(test.readText(false));
    }

    @Test
    public void shouldReadGutenbergText() {
        Book test = getTestBook();
        assertTrue(test.isGutenberg());
    }

    @Test
    public void shouldBeMissingFile() {
        Book test = new Book();
        test.givePipeline(pipeline);
        test.setPath(Paths.get("whoops, nothing here"));
        assertFalse(test.readText(false));
    }

    @Test
    public void createsSubdirectoryBook() {
        Book test = new Book("dir");
        assertEquals(test.getSubdirectory(), "dir");
    }

    @Test
    public void createsTXT() {
        Book test = getTestBook();
        test.readText(false);
        OutputWriter ow = new OutputWriter(test);
        ow.setVerbose(true);
        boolean wroteTxt = ow.writeTxt();
        assertTrue(wroteTxt);
    }

    @Test
    public void createsJSON() {
        Book test = getTestBook();
        test.readText(false);
        OutputWriter ow = new OutputWriter(test);
        assertTrue(ow.writeJson());
    }

    @Test
    public void createsPartsOfSpeechChart() {
        Book test = getTestBook();
        test.readText(false);
        OutputWriter ow = new OutputWriter(test);
        assertTrue(ow.makePartsOfSpeechGraph());
    }

    @Test
    public void createsSyllableDistributionChart() {
        Book test = getTestBook();
        test.readText(false);
        OutputWriter ow = new OutputWriter(test);
        assertTrue(ow.makeSyllableDistributionGraph());
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

    private Book getTestBook() {
        Book book = new Book();
        book.givePipeline(pipeline);
        book.setPath(getTestPath());
        book.setTitleFromText(getTestPath());
        return book;
    }

    private Book getTestBook(String path) {
        Book book = new Book();
        book.givePipeline(pipeline);
        book.setPath(getTestPath(path));
        book.setTitleFromText(getTestPath(path));
        return book;
    }

    private Path getTestPath() {
        return Paths.get("src","main","resources", "test.txt");
    }

    private Path getTestPath(String path) {
        return Paths.get("src", "main", "resources", path);
    }

}
