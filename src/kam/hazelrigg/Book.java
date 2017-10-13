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
    private static final HashMap<String, String> posAbbrev =
            TextTools.nonAbbreviate(new File("posAbbreviations.txt"));

    private String title;
    private String author;
    private File path;
    private int wordCount;
    private boolean gutenberg;
    private final FreqMap posFreq;
    private final FreqMap wordFreq;
    private final FreqMap difficultyMap;

    public Book() {
        this.title = "";
        this.author = "";
        this.gutenberg = false;
        this.posFreq = new FreqMap();
        this.wordFreq = new FreqMap();
        this.difficultyMap = new FreqMap();
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

            if (firstLine.contains("Project Gutenberg's")
                    || firstLine.contains("Project Gutenberg’s")) {
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
     * Returns whether or not a file already has a results file.
     *
     * @return True if the file has results already
     */
    boolean resultsFileExists() {
        return getResultsFile().exists();
    }

    private File getResultsFile() {
        return new File("results/txt/" + title + " by " + author + " Results.txt");
    }

    /**
     * Reads a text file and tags each line for parts of speech as well as counts word frequencies.
     */
    public void analyseText() {

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
            long endTime = System.currentTimeMillis();
            System.out.println(
                    "\n☑ - Finished analysis of " + fullTitle + " in " + (endTime - startTime) / 1000 + "s.");

        } catch (IOException e) {
            System.out.println("Couldn't find file at " + path);
        }


    }

    /**
     * Returns the part of speech of a word.
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
     * Writes frequencies of book into a text file.
     */
    public void writeFrequencies() {
        if (posFreq.getSize() > 0) {
            File out;

            // Create results directories
            if (!makeResultDirs()) {
                System.out.println("[Error] Failed to create results directories");
                out = new File(title + " by " + author + " Results.txt");
            } else {
                out = new File("results/txt/" + title + " by " + author + " Results.txt");
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {

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
     * Creates results directories for files to be saved to.
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
            outPath = "results/img/" + title + " by " + author + " " + purpose + " Results.jpeg";
        } else {
            System.out.println("[Error] Failed to create results directories");
            outPath = title + " by " + author + " " + purpose + "Results.jpeg";
        }


        // Load POS data into data set
        HashMap<String, Integer> map = freq.getFrequency();

        for (String type : map.keySet()) {
            dataSet.setValue(type, map.get(type));
        }

        String title = purpose + " of " + this.title + " by " + this.author;

        JFreeChart chart = createPieChart3D(
                title,
                dataSet,
                false,
                true,
                false);

        PiePlot3D plot = (PiePlot3D) chart.getPlot();
        plot = setColors(plot);

        plot.setBaseSectionOutlinePaint(new Color(0, 0, 0));
        plot.setDarkerSides(true);
        plot.setBackgroundPaint(new Color(204, 204, 204));
        plot.setLabelBackgroundPaint(new Color(255, 255, 255));
        plot.setStartAngle(90f);
        plot.setLabelFont(new Font("Ubuntu San Serif", Font.PLAIN, 10));
        plot.setDepthFactor(0.05f);

        // Save the chart to jpeg
        try {
            ChartUtilities.saveChartAsJPEG(new File(outPath), chart, 900, 900);
        } catch (IOException ioe) {
            System.out.println("[Error - makeGraph] Failed to make pie chart " + ioe);
        }
    }

    private PiePlot3D setColors(PiePlot3D chart) {


        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(("posAbbreviations.txt"))));

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

        } catch (IOException e) {
            e.printStackTrace();
        }
        return chart;
    }

    public void writeConclusion() {
        File out;
        // Create results directories
        if (!makeResultDirs()) {
            System.out.println("[Error] Failed to create results directories");
            out = new File("Conclusion of " + title + " by " + author + ".txt");
        } else {
            out = new File("results/txt/Conclusion of " + title + " by " + author + ".txt");
        }


        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(out));
            bw.write("This is an automatically generated conclusion."
                    + " Some information may be incorrect.\n\n");

            bw.write(title + " by " + author + "\n\n");

            bw.write("This piece is considered a " + classifyLength()
                    + " based on Nebula Award classifications.\n");

            bw.write("It is most likely " + classifyDifficulty()
                    + " to read due to its ratio of polysyllabic words to monosyllabic words.\n");

            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return title;
    }

    public File getPath() {
        return path;
    }

    public String getAuthor() {
        return author;
    }

    private String classifyLength() {
        /*
        Classification 	Word count
        Novel 	40,000 words or over
        Novella 	17,500 to 39,999 words
        Novelette 	7,500 to 17,499 words
        Short story 	under 7,500 words
        */

        if (wordCount < 7500) {
            return "short story";
        }

        if (wordCount < 17500) {
            return "novelette";
        }

        if (wordCount < 40000) {
            return "novella";
        }

        return "novel";

    }

    public FreqMap getWordFreq() {
        return wordFreq;
    }

    private String classifyDifficulty() {
        int mono = difficultyMap.get("Monosyllabic");
        int poly = difficultyMap.get("Polysyllabic");

        if (mono < poly) {
            return "easy";
        }

        return "difficult";
    }

    public void setPath(File path) {
        this.path = path;
    }

}
