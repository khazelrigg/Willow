package kam.hazelrigg;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WillowTest {

    final private ExpectedException e = ExpectedException.none();

    @BeforeClass
    public static void setUp() {
        Willow.pipeline = createPipeline();
    }

    @Test
    public void getsTitleFromText() {
        Book test = getTestBook();
        test.setTitleFromText();
        assertEquals("2 B R 0 2 B", test.getTitle());
    }

    @Test
    public void getsNameFromText() {
        Book test = getTestBook();
        test.setTitleFromText();
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
        test.setPath(fakePath);
        test.readText(false);
    }

    @Test
    public void shouldGetIsGutenberg() {
        Book test = getTestBook();
        test.setTitleFromText();
        BookStats stats = test.getStats();
        assertTrue(stats.isGutenberg());
    }

    @Test
    public void shouldGetIsNotGutenberg() {
        Book test = getTestBook("test2.txt");
        test.setTitleFromText();
        BookStats stats = test.getStats();
        assertFalse(stats.isGutenberg());
    }

    @Test
    public void shouldGetIsGutenbergVariant() {
        Book test = getTestBook("/test3.txt");
        test.setTitleFromText();
        BookStats stats = test.getStats();
        assertTrue(stats.isGutenberg());
    }

    @Test
    public void shouldReadText() {
        Book test = getTestBook();
        assertTrue(test.readText(false));
    }

    @Test
    public void shouldBeMissingFile() {
        Book test = new Book();
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
    public void getsEconomyTextReader() {
        Book test = getTestBook();
        test.readText(true);
        assertTrue(test.getTextReader() instanceof EconomyTextReader);
    }

    @Test
    public void getsPlainTextReader() {
        Book test = getTestBook();
        test.readText(false);
        assertTrue(test.getTextReader() instanceof PlainTextReader);
    }

    @Test
    public void getsPdfTextReader() {
        Book test = getTestBook("test.pdf");
        test.readText(false);
        assertTrue(test.getTextReader() instanceof PdfTextReader);
    }

    @Test
    public void getsCorrectWordCount() {
        Book test = getTestBook();
        test.readText(false);
        BookStats stats = test.getStats();
        assertEquals(3046, stats.getWordCount());
    }

    @Test
    public void getsCorrectNouns() {
        Book test = getTestBook();
        test.readText(false);
        BookStats stats = test.getStats();
        assertEquals(585, stats.getPartsOfSpeech().get("Noun, singular or mass"));
    }

    private Book getTestBook() {
        Book book = new Book();
        book.setPath(getTestPath());
        return book;
    }

    private Book getTestBook(String path) {
        Book book = new Book();
        book.setPath(getTestPath(path));
        return book;
    }

    private Path getTestPath() {
        return new File("src/test/resources/test.txt").toPath();
        //
    }

    private Path getTestPath(String path) {
        return new File("src/test/resources/" + path).toPath();
    }

    private static StanfordCoreNLP createPipeline() {
        Properties properties = new Properties();
        properties.put("annotators",
                "tokenize, ssplit, pos, lemma");
        properties.put("tokenize.options", "untokenizable=noneDelete");

        return new StanfordCoreNLP(properties);
    }


}
