package kam.hazelrigg;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import static org.jfree.chart.ChartFactory.createPieChart3D;

public class Book {
    // Set up tagger
    private static final MaxentTagger tagger =
            new MaxentTagger("models/english-left3words-distsim.tagger");

    // Get POS abbreviation values
    private static HashMap<String, String> posAbbrev =
            TextTools.nonAbbreviate(new File("posAbbreviations.txt"));

    private String title;
    private String author;
    private File path;
    private boolean gutenberg;
    private FreqMap posFreq;
    private FreqMap wordFreq;
    private FreqMap difficultyMap;

    public Book() {
        this.title = "";
        this.author = "";
        this.gutenberg = false;
        this.posFreq = new FreqMap();
        this.wordFreq = new FreqMap();
        this.difficultyMap = new FreqMap();
    }

    /**
     * Creates results directories for files to be saved to
     *
     * @return True if both directories are successfully created
     */
    private static boolean makeResultDirs() {

        File txt = new File("results/txt");
        File img = new File("results/img");

        if (!(txt.exists() || txt.mkdirs())) {
            System.out.println("[Error] Could not create results directory 'txt'");
            return false;
        }
        if (!(img.exists() || img.mkdirs())) {
            System.out.println("[Error] Could not create results directory 'img'");
            return false;
        }

        return true;
    }

    /**
     * Returns the part of speech of a word
     *
     * @param line The word to tag
     * @return Tag of word
     */
    private static String[] getSentenceTags(String line) {
        String tagLine = tagger.tagString(line);

        StringBuilder tags = new StringBuilder();

        for (String word : tagLine.split("\\s")) {
            // Split line into words with tags and then ignore short words

            if (word.replaceAll("\\W", "").length() > 2) {
                String tag = word.substring(word.indexOf("_") + 1).toLowerCase();
                tag = posAbbrev.get(tag);

                // What to do if we have no tag
                if (tag == null) {
                    tag = "Unknown";
                }

                // Add the tag and | so we can split the string later
                tags.append(tag).append("|");

            }
        }

        return tags.toString().split("\\|");
    }

    /**
     * Set the path for the text file
     *
     * @param path path of text
     */
    void setPath(File path) {
        this.path = path;
    }

    /**
     * Creates the frequencies of the book
     */
    void analyseText() {

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String fullTitle = title + " by " + author;

            System.out.println("☐ - Starting analysis of " + fullTitle);
            long startTime = System.currentTimeMillis();
            Boolean atBook = false;

            for (String line; (line = br.readLine()) != null; ) {
                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Skip over the Gutenberg headers
                if (gutenberg && !atBook) {
                    if (line.contains("START OF THIS PROJECT GUTENBERG EBOOK") ||
                            line.contains("START OF THE PROJECT GUTENBERG EBOOK")) {
                        atBook = true;
                        line = br.readLine();
                    } else {
                        continue;
                    }
                }

                // Stop at the Gutenberg footer
                if (gutenberg) {
                    if (line.contains("End of the Project Gutenberg EBook") ||
                            line.contains("End of Project Gutenberg’s")) {
                        break;
                    }
                }

                // Tag each line
                String[] tagLine = getSentenceTags(line);
                for (String tag : tagLine) {
                    if (tag.isEmpty()) {
                        continue;
                    }
                    posFreq.increaseFreq(tag);
                }

                // Word counts
                for (String word : line.split("\\s")) {
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
            long endTime = System.currentTimeMillis();
            System.out.println("\n☑ - Finished analysis of " + fullTitle + " in " + (endTime - startTime) / 1000 + "s.");

        } catch (IOException e) {
            System.out.println("Couldn't find file at " + path);
        }


    }

    /**
     * Writes frequencies of book into a text file
     */
    void writeFrequencies() {
        if (posFreq.getSize() > 0) {
            String outPath;

            // Create results directories
            if (!makeResultDirs()) {
                System.out.println("[Error] Failed to create results directories");
                outPath = title + " by " + author + " Results.txt";
            } else {
                outPath = "results/txt/" + title + " by " + author + " Results.txt";
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outPath)))) {

                // Write word frequencies
                bw.write("==================[ Word ]==================\n");
                bw.write(wordFreq.toString());

                // Write pos frequencies
                bw.write("\n================[ POS Tags ]================\n");
                bw.write(posFreq.toString());

                bw.close();
            } catch (IOException e) {
                System.out.println("Error writing frequencies");
                System.exit(3);
            }

        }
    }

    /**
     * Creates a parts of speech distribution pie graph
     */
    void makePOSGraph() {
        makeGraph("POS Distribution", posFreq);
    }

    /**
     * Creates a difficulty pie graph that uses syllable 
     */
    void makeDifficultyMap() {
        makeGraph("Difficulty", difficultyMap);
    }

    /**
     * Creates a graph using JFreeChart that is saved to a jpg
     * @param purpose Purpose of the graph, used in title of graph
     * @param freq FreqMap to use values off
     */
    private void makeGraph(String purpose, FreqMap freq) {

        DefaultPieDataset dataSet = new DefaultPieDataset();
        String outPath;

        // Create results directories
        if (new File("results/img/").isDirectory()) {
            outPath = "results/img/" + title + " by " + author + " " + purpose + " Results.jpeg";
        } else {
            System.out.println("[Error] Failed to create results directories");
            outPath = title + " by " + author + " " + purpose + "Results.jpeg";
        }

        // Load POS data into data set
        for (String type : freq.getFrequency().keySet()) {
            dataSet.setValue(type, freq.get(type));
        }

        String title = purpose + " of " + this.title + " by " + this.author;

        JFreeChart chart = createPieChart3D(
                title,
                dataSet,
                false,
                true,
                false);

        PiePlot3D plot = (PiePlot3D) chart.getPlot();

        plot.setBaseSectionOutlinePaint(new Color(0, 0, 0));
        plot.setDarkerSides(true);
        plot.setBackgroundPaint(new Color(204, 204, 204));
        plot.setLabelBackgroundPaint(new Color(255, 255, 255));
        plot.setStartAngle(90f);
        plot.setLabelFont(new Font("Ubuntu San Serif", Font.PLAIN, 10));
        plot.setDepthFactor(0.05f);

        try {
            ChartUtilities.saveChartAsJPEG(new File(outPath), chart, 900, 900);
        } catch (IOException ioe) {
            System.out.println("[Error - makeGraph] Failed to make pie chart " + ioe);
        }
    }

    /**
     * Returns whether or not a file already has a results file
     *
     * @return True if the file has results already
     */
    boolean resultsFileExists() {
        File results = new File("results/txt/" + title + " by " + author + " Results.txt");
        return results.exists();
    }

    /**
     * Get the title of a book
     *
     * @param text File to find title of
     */
    void setTitleFromText(File text) {
        String title = "";
        String author = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(text));
            String firstLine = br.readLine();

            // If the first line is very short skip over it
            while (firstLine.length() < 3) {
                firstLine = br.readLine();
            }

            // Cases of Gutenberg books to check
            if (firstLine.contains("The Project Gutenberg EBook of")) {
                firstLine = firstLine.substring(31);
                this.gutenberg = true;
            }

            if (firstLine.contains("Project Gutenberg's") ||
                    firstLine.contains("Project Gutenberg’s")) {
                firstLine = firstLine.substring(20);
                this.gutenberg = true;
            }

            // If the pattern "title by author" appears split at the word 'by' to get author and title
            if (firstLine.contains("by")) {
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
            e.printStackTrace();
        }

        this.title = title;
        this.author = author;
    }

    /**
     * Gets the title of a Book
     * @return title of the book
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Set the title of a book
     *
     * @param title Title to use
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get the author of a book
     *
     * @return The author's name
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Set the author of a book
     *
     * @param author Author to use
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Return if book is a Gutenberg text
     *
     * @return True if it is a Gutenberg text, false otherwise
     */
    public boolean isGutenberg() {
        return gutenberg;
    }

    /**
     * Set whether or not a text is a Gutenberg Book
     *
     * @param gutenberg Boolean set to true if it is a Gutenberg book
     */
    public void setGutenberg(boolean gutenberg) {
        this.gutenberg = gutenberg;
    }
}
