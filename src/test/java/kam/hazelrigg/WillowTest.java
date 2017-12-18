package kam.hazelrigg;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import kam.hazelrigg.readers.EconomyTextReader;
import kam.hazelrigg.readers.PdfTextReader;
import kam.hazelrigg.readers.PlainTextReader;
import org.apache.commons.cli.ParseException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WillowTest {

    final private ExpectedException e = ExpectedException.none();

    @BeforeClass
    public static void setUp() {
        Willow.pipeline = createPipeline();
        try {
            BatchRunner.passCommandLine(Willow.getCommandLine(new String[]{""}));
        } catch (ParseException e1) {
            e1.printStackTrace();
        }
    }

    @Test
    public void copiedFreqmapsAreEqual() {
        FreqMap one = new FreqMap();
        one.increaseFreq("test");

        FreqMap two = one;

        assertTrue(one.equals(two));
    }


    @Test
    public void freqmapsAreEqual() {
        FreqMap one = new FreqMap();
        one.increaseFreq("test");
        one.increaseFreq("best");

        FreqMap two = new FreqMap();
        two.increaseFreq("test");
        two.increaseFreq("best");

        assertTrue(one.equals(two));
    }

    @Test
    public void freqmapsAreNotEqual() {
        FreqMap one = new FreqMap();
        one.increaseFreq("test");
        FreqMap two = new FreqMap();
        one.increaseFreq("toast");
        assertFalse(one.equals(two));
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

    @Test(expected = NullPointerException.class)
    public void nullReadFileTest() {
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
        assertEquals("dir", test.getSubdirectory());
    }

    @Test
    public void createsTXT() {
        Book test = getTestBook();
        test.readText(false);
        OutputWriter ow = new OutputWriter(test);
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
        assertEquals(2662, stats.getWordCount());
    }

    @Test
    public void getsCorrectNouns() {
        Book test = getTestBook();
        test.readText(false);
        BookStats stats = test.getStats();
        assertEquals(360, stats.getPartsOfSpeech().get("Noun, singular or mass"));
    }

    @Test
    public void runBatchSetOfFiles() throws IOException {
        BatchRunner.startRunners(getTestPath("dir"), 3);
        ArrayList<Runner> runners = BatchRunner.getRunners();

        ArrayList<String> realPaths = new ArrayList<>();
        realPaths.add("test.txt");
        realPaths.add("test2.txt");
        realPaths.add("test3.txt");

        ArrayList<String> paths = new ArrayList<>();
        for (Runner runner : runners) {
            paths.add(runner.getBook().getPath().getFileName().toString());
        }

        Collections.sort(paths);
        assertEquals(realPaths, paths);
    }

    @Test
    public void batchRunnerSingleFile() throws IOException {
        BatchRunner.clear();
        BatchRunner.startRunners(getTestPath(), 1);
        assertEquals(1, BatchRunner.getRunners().size());
    }

    @Test
    public void runnerCreatesResults() throws IOException, ParseException {
        Runner.setCmd(Willow.getCommandLine(new String[]{"-o"}));
        Runner runner = new Runner(getTestPath(), getTestPath());
        runner.run();
        assertTrue(runner.getBook().hasResults(false, false));
    }

    @Test(expected = IOException.class)
    public void batchRunnerNullFile() throws IOException {
        BatchRunner.clear();
        BatchRunner.startRunners(getTestPath("notarealfile"), 0);
    }

    @Test
    public void runWillow() {
        Willow.main(new String[]{"-o", "src/test/resources/dir"});
        assertTrue(BatchRunner.getRunners().get(0).getBook().hasResults(false, false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void willowPrintsHelp() {
        Willow.main(new String[]{});
    }

    private Book getTestBook() {
        Book book = new Book();
        book.setPath(getTestPath());
        book.setTitleFromText();
        return book;
    }

    private Book getTestBook(String path) {
        Book book = new Book();
        book.setPath(getTestPath(path));
        book.setTitleFromText();
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
