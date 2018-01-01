package kam.hazelrigg;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import kam.hazelrigg.readers.*;
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

import static org.junit.Assert.*;

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
    public void stringDoesNotEqualFreqmap() {
        FreqMap one;
        one = new FreqMap();
        one.increaseFreq("test");

        assertFalse(one.equals("test"));
    }

    @Test
    public void freqmapsAreEqual() {
        FreqMap one;
        one = new FreqMap();
        one.increaseFreq("test");
        one.increaseFreq("best");

        FreqMap two = new FreqMap();
        two.increaseFreq("test");
        two.increaseFreq("best");

        assertTrue(one.equals(two));
    }

    @Test
    public void freqmapsAreNotEqual() {
        FreqMap one;
        one = new FreqMap();
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
        Book test = getTestBook("test3.txt");
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
    public void createsCSV() {
        Book test = getTestBook();
        test.readText(false);
        OutputWriter ow = new OutputWriter(test);
        assertTrue(ow.writeCsv());
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
    public void getsCorrectWordCount() throws ParseException {
        Runner testEconomyRunner = new Runner(getTestPath(), getTestPath());
        testEconomyRunner.setCommandLine(Willow.getCommandLine(new String[]{"-eo"}));
        testEconomyRunner.run();

        Book test = testEconomyRunner.getBook();
        BookStats stats = test.getStats();
        assertEquals(2672, stats.getWordCount());
    }

    @Test
    public void getsCorrectNouns() {
        Book test = getTestBook();
        test.readText(false);
        BookStats stats = test.getStats();
        assertEquals(363, stats.getPartsOfSpeech().get("Noun, singular or mass"));
    }

    @Test
    public void getsNovelLength() {
        Book test = getTestBook("test4.txt");
        test.readText(false);
        BookStats stats = test.getStats();
        assertEquals("novel", stats.getClassifiedLength());
    }

    @Test
    public void getsSentenceCount() {
        Book test = getTestBook();
        test.readText(false);
        BookStats stats = test.getStats();
        assertEquals(261, stats.getSentenceCount());
    }

    @Test
    public void getsMonosyllabic() {
        Book test = getTestBook();
        test.readText(false);
        BookStats stats = test.getStats();
        assertEquals(1769, stats.getMonosyllablic());
    }

    @Test
    public void getsPolysyllabic() {
        Book test = getTestBook();
        test.readText(false);
        BookStats stats = test.getStats();
        assertEquals(905, stats.getPolysyllablic());
    }

    @Test
    public void isDifficult() {
        Book test = getTestBook("difficult.txt");
        test.readText(false);
        BookStats stats = test.getStats();
        assertEquals("difficult", stats.getEasyDifficult());
    }


    @Test
    public void getsGradeLevel() {
        Book test = getTestBook("Obama.txt");
        test.readText(false);
        BookStats stats = test.getStats();
        assertEquals("10th to 12th grade", stats.classifyKincaidScore(stats.getFleschKincaidScore()));
    }

    @Test
    public void runBatchSetOfFiles() throws IOException {
        BatchRunner.clear();
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

    @Test(expected = IOException.class)
    public void runNullFolder() throws IOException {
        BatchRunner.startRunners(getTestPath(null), 1);
    }

    @Test
    public void batchRunnerSingleFile() throws IOException {
        BatchRunner.clear();
        BatchRunner.startRunners(getTestPath(), 1);
        assertEquals(1, BatchRunner.getRunners().size());
    }

    @Test
    public void runnerCreatesResults() throws ParseException {
        Runner runner = new Runner(getTestPath(), getTestPath());
        runner.setCommandLine(Willow.getCommandLine(new String[]{"-o"}));
        runner.run();
        assertTrue(runner.getBook().hasResults(false, false));
    }

    @Test(expected = IOException.class)
    public void batchRunnerNullFile() throws IOException {
        BatchRunner.clear();
        BatchRunner.startRunners(getTestPath("notarealfile"), 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void batchRunnerNoConstructor() {
        new BatchRunner();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void willowNoConstructor() {
        new Willow();
    }

    // Text Readers
    @Test
    public void economyModeGetsCorrectWordCounts() throws ParseException {
        Runner testEconomyRunner = new Runner(getTestPath(), getTestPath());
        testEconomyRunner.setCommandLine(Willow.getCommandLine(new String[]{"-oe"}));
        testEconomyRunner.run();

        Book test = testEconomyRunner.getBook();
        BookStats stats = test.getStats();
        long wordCount = stats.getWordCount();
        assertEquals(2672, wordCount);
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
    public void getsDocTextReader() {
        Book test = getTestBook("TestWordDoc.doc");
        test.readText(false);
        assertTrue(test.getTextReader() instanceof DocTextReader);
    }

    @Test
    public void getsDocxTextReader() {
        Book test = getTestBook("demo.docx");
        test.readText(false);
        assertTrue(test.getTextReader() instanceof DocxTextReader);
    }

    @Test
    public void plainTextIOException() {
        e.expect(IOException.class);
        Book test = new Book();
        PlainTextReader reader = new PlainTextReader();
        reader.setBook(test);
        reader.setPath(Paths.get(""));
        reader.readText();
    }

    @Test
    public void pdfReaderIOException() {
        e.expect(IOException.class);
        Book test = new Book();
        PdfTextReader reader = new PdfTextReader();
        reader.setBook(test);
        reader.setPath(Paths.get(""));
        reader.readText();
    }

    @Test
    public void economyIOException() {
        e.expect(IOException.class);
        Book test = new Book();
        EconomyTextReader reader = new EconomyTextReader();
        reader.setBook(test);
        reader.setPath(Paths.get(""));
        reader.readText();
    }

    @Test
    public void economyScratchException() {
        e.expect(IOException.class);
        Book test = new Book();
        EconomyTextReader reader = new EconomyTextReader();
        reader.setBook(test);
        reader.setPath(Paths.get(""));
        reader.readText();
    }

    @Test
    public void docxIOException() {
        e.expect(IOException.class);
        Book test = new Book();
        DocxTextReader reader = new DocxTextReader();
        reader.setBook(test);
        reader.setPath(Paths.get(""));
        reader.readText();
    }

    @Test
    public void docIOException() {
        e.expect(IOException.class);
        Book test = new Book();
        DocTextReader reader = new DocTextReader();
        reader.setBook(test);
        reader.setPath(Paths.get(""));
        reader.readText();
    }

    @Test
    public void runnerRunsFlags() throws ParseException {
        Runner runner = new Runner(getTestPath(), getTestPath());
        runner.setCommandLine(Willow.getCommandLine(new String[]{"-jicoe"}));
        runner.run();
        Book tester = runner.getBook();
        assertTrue(tester.hasResults(true, true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void willowPrintsHelp() {
        Willow.main(new String[]{"-o"});
    }

    @Test
    public void willowNotRealPath() {
        e.expect(IOException.class);
        Willow.main(new String[]{"areallylongnamethatisnotanactualdirectory"});
    }

    @Test
    public void willowParseException() {
        e.expect(ParseException.class);
        Willow.main(new String[]{"-xyz", ""});
    }

    @Test
    public void willowRuns() {
        BatchRunner.clear();
        Willow.main(new String[]{"-ot", "3", "src/test/resources/dir"});
        assertEquals(3, BatchRunner.getRunners().size());
    }

    private static StanfordCoreNLP createPipeline() {
        Properties properties = new Properties();
        properties.put("annotators",
                "tokenize, ssplit, pos, lemma");
        properties.put("tokenize.options", "untokenizable=noneDelete");

        return new StanfordCoreNLP(properties);
    }

    private static Book getTestBook() {
        Book book = new Book();
        book.setPath(getTestPath());
        book.setTitleFromText();
        return book;
    }

    private static Book getTestBook(String path) {
        Book book = new Book();
        book.setPath(getTestPath(path));
        book.setTitleFromText();
        return book;
    }

    private static Path getTestPath() {
        return new File("src/test/resources/test.txt").toPath();
    }

    private static Path getTestPath(String path) {
        return new File("src/test/resources/" + path).toPath();
    }


}
