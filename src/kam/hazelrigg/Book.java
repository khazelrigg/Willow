package kam.hazelrigg;


import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.text.WordUtils.wrap;
import static org.jfree.chart.ChartFactory.createPieChart;

public class Book {
    // Set up tagger
    private static final MaxentTagger tagger =
            new MaxentTagger("models/english-left3words-distsim.tagger");

    private final FreqMap posFreq;
    private final FreqMap wordFreq;
    private final FreqMap difficultyMap;

    private String title;
    private String author;
    private boolean gutenberg;

    private File path;
    private String subdirectory;

    private long wordCount;
    private long syllableCount;
    private long sentenceCount;
    private List<HasWord> longestSentence;

    public Book() {
        this.title = "";
        this.author = "";
        this.gutenberg = false;
        this.subdirectory = "";

        this.posFreq = new FreqMap();
        this.wordFreq = new FreqMap();
        this.difficultyMap = new FreqMap();
    }

    /**
     * Creates results directories for files to be saved to.
     *
     * @return True if both directories are successfully created
     */
    static boolean makeParentDirs() {
        return makeDir("results/txt") && makeDir("results/img") && makeDir("results/json");
    }

    /**
     * Creates the subdirectories for result files
     *
     * @param subdir Subdirectory to create
     */
    static void makeResultDirs(File subdir) {
        makeParentDirs();
        makeDir("results/txt/" + subdir.getName());
        makeDir("results/img/" + subdir.getName());
        makeDir("results/json/" + subdir.getName());
    }

    private static boolean makeDir(String path) {
        File dir = new File(path);
        if (!(dir.exists() || dir.mkdirs())) {
            System.out.println("[Error] Could not create dir: " + path);
            return false;
        }
        return true;
    }

    /**
     * Get the title of a book.
     *
     * @param text File to find title of
     */
    public void setTitleFromText(File text) {
        String title = "";
        String author = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(text));
            String firstLine;
            firstLine = br.readLine();

            // If the first line is very short skip over it
            // TODO What to do if entire file is empty
            while (firstLine.length() < 1) {
                firstLine = br.readLine();
            }

            // Cases of Gutenberg books to check
            if (firstLine.contains("The Project Gutenberg EBook of")) {
                firstLine = firstLine.substring(31);
                this.gutenberg = true;
            } else if (firstLine.contains("Project Gutenberg's")
                    || firstLine.contains("Project Gutenberg’s")) {
                firstLine = firstLine.substring(20);
                this.gutenberg = true;
            }

            // If the pattern "title by author" appears split at the word 'by' to get author and title
            if (gutenberg && firstLine.contains("by")) {
                title = firstLine.substring(0, firstLine.lastIndexOf("by")).trim();
                author = firstLine.substring(firstLine.lastIndexOf("by") + 2).trim();
            } else {
                title = text.getName();
                author = "";
            }

            // Remove any trailing commas
            if (title.endsWith(",")) {
                title = title.substring(0, title.length() - 1);
            }

            br.close();
        } catch (IOException e) {
            System.out.println("IOException : " + path.getName());
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("OSHIT - STUFF BROKE WITH " + path.getName());
            e.printStackTrace();
        }

        this.title = title;
        this.author = author;
    }

    /**
     * Tag a text for parts of speech
     *
     * @param text Text to be tagged
     */
    private void tagFile(String text) {
        // Tag the entire file
        PTBTokenizer.PTBTokenizerFactory<CoreLabel> tokenizerFactory =
                PTBTokenizer.PTBTokenizerFactory.newPTBTokenizerFactory(new CoreLabelTokenFactory(), "untokenizable=noneKeep");

        try {
            // Get POS abbreviation values
            HashMap<String, String> posAbbrev = TextTools.nonAbbreviate();

            BufferedReader br = new BufferedReader(new FileReader(path));
            DocumentPreprocessor dp = new DocumentPreprocessor(br);
            dp.setTokenizerFactory(tokenizerFactory);

            // Loop through every sentence
            for (List<HasWord> sentence : dp) {
                List<TaggedWord> tSent = tagger.tagSentence(sentence);

                if (longestSentence == null) {
                    longestSentence = sentence;
                }

                if (sentence.size() > longestSentence.size()) {
                    longestSentence = sentence;
                }

                for (TaggedWord word : tSent) {
                    String tag = posAbbrev.get(word.tag().toLowerCase());

                    if (tag != null) {
                        posFreq.increaseFreq(tag);
                    }
                }
                sentenceCount++;
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        syllableCount = TextTools.getSyllableCount(text);
    }

    /**
     * Reads a text file and tags each line for parts of speech as well as counts word frequencies.
     */
    public void analyseText() {

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            System.out.println("☐ - Starting analysis of " + getName());
            long startTime = System.currentTimeMillis();

            Boolean atBook = false;
            StringBuilder text = new StringBuilder();

            for (String line; (line = br.readLine()) != null; ) {

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Skip over the Gutenberg headers
                if (gutenberg && !atBook) {
                    if (line.contains("START OF THIS PROJECT GUTENBERG EBOOK")
                            || line.contains("START OF THE PROJECT GUTENBERG EBOOK")) {
                        atBook = true;
                        line = br.readLine();
                    } else {
                        continue;
                    }
                }

                // Stop at the Gutenberg footer
                if (gutenberg) {
                    if (line.contains("End of the Project Gutenberg EBook")
                            || line.contains("End of Project Gutenberg’s")) {
                        break;
                    }
                }

                text.append(line);

                // Word counts
                for (String word : line.split("\\s")) {
                    wordCount++;
                    // Make word lowercase and strip punctuation
                    word = word.toLowerCase().replaceAll("\\W", "");

                    // Skip punctuation and stop words
                    if (word.isEmpty() || TextTools.isStopWord(word)) {
                        continue;
                    }

                    // Add difficulty information
                    if (TextTools.getSyllableCount(word) == 1) {
                        difficultyMap.increaseFreq("Monosyllabic");
                    } else {
                        difficultyMap.increaseFreq("Polysyllabic");
                    }

                    // Increase word frequency
                    wordFreq.increaseFreq(word);

                }
            }
            br.close();

            tagFile(text.toString());

            long endTime = System.currentTimeMillis();
            System.out.println(
                    "\n☑ - Finished analysis of " + getName() + " in " + (endTime - startTime) / 1000 + "s.");

        } catch (IOException e) {
            System.out.println("Couldn't find file at " + path);
        }

    }

    /**
     * Writes frequencies of book into a text file.
     */
    public void writeText() {
        //TODO Look into template for generating these files

        if (posFreq.getSize() > 0) {
            File out;

            // Create results directories
            if (!makeParentDirs()) {
                System.out.println("[Error] Failed to create results directories");
                out = new File(getName() + " Results.txt");
            } else {
                if (subdirectory.equals("")) {
                    out = new File("results/txt/" + getName() + " Results.txt");
                } else {
                    out = new File("results/txt/" + subdirectory + "/" + getName()
                            + " Results.txt");
                }
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
                bw.write("Auto generated results for ");
                if (author.isEmpty()) {
                    bw.write(title + "\n");
                } else {
                    bw.write(title + " by " + author + "\n");
                }

                // Write stats
                bw.write("\n==================[ Stats ]==================\n");
                bw.write("Total Words: " + wordCount);
                bw.write("\nUnique Words: " + wordFreq.getSize());
                bw.write("\nPolysyllabic Words: " + difficultyMap.get("Polysyllabic"));
                bw.write("\nMonosyllabic Words: " + difficultyMap.get("Monosyllabic"));
                bw.write("\nTotal Syllables: " + syllableCount);
                bw.write("\nTotal Sentences: " + sentenceCount);
                bw.write("\nFlesch-Kincaid Grade: " + TextTools.getReadingEaseLevel(wordCount, sentenceCount, syllableCount));
                bw.write("\nClassified Length: " + TextTools.classifyLength(wordCount));
                bw.write("\nTop 3 words: " + wordFreq.getTopThree());
                bw.write(wrap("\nLongest Sentence: " + getLongestSentence(), 100));

                bw.write("\n");

                // Write conclusion
                bw.write("\n================[ Conclusion ]===============\n");
                bw.write(writeResultsText() + "\n");

                // Write pos frequencies
                bw.write("\n=================[ POS Tags ]================\n");
                bw.write(posFreq.toString());

                // Write concordance
                bw.write("\n===============[ Concordance ]===============\n");
                bw.write(createConcordance() + "\n");

                // Write word frequencies
                bw.write("\n===================[ Word ]==================\n");
                bw.write(wordFreq.toString());

                bw.close();
            } catch (IOException e) {
                System.out.println("Error writing frequencies");
                System.exit(3);
            }
            JsonCreator.writeJson(getName(), subdirectory, posFreq);
        }
    }

    /**
     * Creates a parts of speech distribution pie graph.
     */
    public void makePosGraph() {
        makeGraph("POS Distribution", posFreq);
    }

    /**
     * Creates a difficulty pie graph that uses syllable.
     */
    public void makeDifficultyGraph() {
        makeGraph("Difficulty", difficultyMap);
    }

    /**
     * Creates a graph using JFreeChart that is saved to a jpg.
     *
     * @param purpose Purpose of the graph, used in title of graph
     * @param freq    FreqMap to use values off
     */
    private void makeGraph(String purpose, FreqMap freq) {

        DefaultPieDataset dataSet = new DefaultPieDataset();
        String outPath;

        // Create results directories
        if (new File("results/img/").isDirectory()) {
            outPath = "results/img/" + subdirectory + "/" + getName() + " " + purpose + " Results.jpeg";
        } else {
            System.out.println("[Error] Failed to create results directories");
            outPath = title + "  " + author + " " + purpose + "Results.jpeg";
        }


        // Load POS data into data set
        HashMap<String, Integer> map = freq.getFrequency();

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            dataSet.setValue(type, count);
        }

        JFreeChart chart = createPieChart(
                purpose + " of " + getName(),
                dataSet,
                false,
                true,
                false);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot = setColors(plot);

        plot.setBaseSectionOutlinePaint(new Color(0, 0, 0));
        plot.setShadowPaint(null);
        plot.setBackgroundPaint(new Color(204, 204, 204));
        plot.setLabelBackgroundPaint(new Color(255, 255, 255));
        plot.setStartAngle(90f);
        plot.setLabelFont(new Font("Ubuntu San Serif", Font.PLAIN, 10));

        // Save the chart to jpeg
        try {
            ChartUtilities.saveChartAsJPEG(new File(outPath), chart, 1000, 1000);
        } catch (IOException ioe) {
            System.out.println("[Error - makeGraph] Failed to make pie chart " + ioe);
        }
    }

    /**
     * Sets the colors of a pie chart based on labels in order to keep charts consistent
     *
     * @param chart Chart to modify label colors of
     * @return PieChart with color modifications
     */
    private PiePlot setColors(PiePlot chart) {


        try {
            InputStreamReader inputStreamReader =
                    new InputStreamReader(TextTools.class.getResourceAsStream("posAbbreviations.txt"));

            BufferedReader br = new BufferedReader(inputStreamReader);

            String line = br.readLine();
            chart.setSectionPaint("Monosyllabic", new Color(77, 77, 77));
            chart.setSectionPaint("Polysyllabic", new Color(241, 88, 84));

            while (line != null) {
                String label = line.substring(line.indexOf(":") + 1, line.indexOf(">")).trim();

                String hexColor = line.substring(line.indexOf(">") + 1).trim();
                Color color = Color.decode(hexColor);

                chart.setSectionPaint(label, color);

                line = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chart;
    }

    private String writeResultsText() {
        StringBuilder conclusion = new StringBuilder();

        String classifiedLength = TextTools.classifyLength(wordCount);
        String gradeLevel = TextTools.getReadingEaseLevel(wordCount, sentenceCount, syllableCount);
        String difficulty = TextTools.classifyDifficulty(difficultyMap.get("Monosyllabic"),
                difficultyMap.get("Polysyllabic"));

        //TODO format times/
        conclusion.append("This piece is classified as a ")
                .append(classifiedLength).append(" based on the Nebula Award categories. It has ")
                .append("a total of ").append(wordCount).append(" words with ").append(wordFreq.getSize())
                .append(" of those being unique. Using the Flesh-Kincaid reading ease test, this")
                .append(" text is rated at the ").append(gradeLevel).append(" level. ")
                .append("Comparing the ratio of (").append(difficultyMap.get("Polysyllabic"))
                .append(") polysyllabic words to (").append(difficultyMap.get("Monosyllabic"))
                .append(") monosyllabic words it can be speculated that this text is ").append(difficulty)
                .append(" to read. To read this text at a rate of 275wpm it would take ").append(TextTools.getReadingTime(wordCount))
                .append(" minute(s) to finish, ").append(TextTools.getSpeakingTime(wordCount)).append(" minute(s) to speak")
                .append(" at 180wpm, ").append(wordCount / 13).append(" minute(s) to write")
                .append(" at 13wpm and ").append(wordCount / 40).append(" minute(s) to type")
                .append(" at 40wpm.");

        String out = conclusion.toString();
        out = out.replaceAll("\\. ", ".\n");

        return out;
    }

    public String getName() {
        // If there is no author don't add the " by XXX"
        if (author.equals("")) {
            return title;
        }
        return title + " by " + author;
    }

    /**
     * Creates a concordance of all unique words in the text
     *
     * @return String wrapped at 100 characters
     */
    private String createConcordance() {
        StringBuilder concordance = new StringBuilder();

        for (String word : wordFreq.getSortedByKey()) {
            concordance.append(word).append(" ");
        }

        return wrap(concordance.toString(), 100);
    }

    /**
     * Takes a sentence of HasWords and makes them presentable as a human readable sentence
     *
     * @return Sentence as a string
     */
    private String getLongestSentence() {
        StringBuilder sentence = new StringBuilder();
        for (HasWord hasWord : longestSentence) {
            String word = hasWord.word();

            // Remove spaces that come before punctuation
            if (TextTools.isPunctuation(word) && sentence.length() > 1) {
                sentence.deleteCharAt(sentence.length() - 1);
            }

            sentence.append(word).append(" ");
        }
        return sentence.toString();

    }

    public String getTitle() {
        return title;
    }

    /**
     * Returns whether or not a file already has a results file.
     *
     * @return True if the file has results already
     */
    public boolean resultsFileExists() {
        return new File("results/txt/" + subdirectory + "/" + getName() + " Results.txt").exists();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
    }

    void setSubdirectory(String dir) {
        this.subdirectory = dir;
    }
}
