package kam.hazelrigg;

import org.apache.commons.cli.Options;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WillowTest {

    final private ExpectedException e = ExpectedException.none();

    private static final Options options = new Options();

    @BeforeClass
    public static void setUp() {
        options.addOption("h", "help", false, "Print help")
                .addOption("v", "verbose", false, "Verbose output")
                .addOption("w", "words", false, "Only analyse a text for word counts, cannot be used with -i or -j")
                .addOption("e", "economy", false, "Run in economy mode, greatly reduces memory usage at the cost of completion speed. Useful for computers with less memory")
                .addOption("i", "images", false, "Create image outputs")
                .addOption("j", "json", false, "Create JSON output")
                .addOption("c", "csv", false, "Create CSV output")
                .addOption("o", "overwrite", false, "Overwrite any existing results")
                .addOption("t", "threads", true, "Max number of threads to run, 0 = Use number of CPUs available; default = 0");
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
        BookStats stats = test.getStats();
        assertTrue(stats.isGutenberg());
    }

    @Test
    public void shouldGetIsNotGutenberg() {
        Book test = getTestBook("test2.txt");
        BookStats stats = test.getStats();
        assertFalse(stats.isGutenberg());
    }

    @Test
    public void shouldGetIsGutenbergVariant() {
        Book test = getTestBook("/test3.txt");
        BookStats stats = test.getStats();
        assertTrue(stats.isGutenberg());
    }

    @Test
    public void shouldReadText() {
        Book test = getTestBook();
        assertTrue(test.readText(false));
    }

    @Test
    public void shouldReadGutenbergText() {
        Book test = getTestBook();
        BookStats stats = test.getStats();
        assertTrue(stats.isGutenberg());
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

    private Book getTestBook() {
        Book book = new Book();
        book.setPath(getTestPath());
        book.setTitleFromText(getTestPath());
        return book;
    }

    private Book getTestBook(String path) {
        Book book = new Book();
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
