package kam.hazelrigg;

import edu.stanford.nlp.util.CoreMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import static org.apache.commons.lang3.text.WordUtils.wrap;

public class OutputWriter {
    static final String ANSI_RESET = "\u001B[0m";
    static final String ANSI_GREEN = "\u001B[32m";
    private final String subdirectory;
    private final FreqMap<String, Integer> words;
    private final FreqMap<String, Integer> partsOfSpeech;
    private final FreqMap<String, Integer> lemmas;
    private final FreqMap<String, Integer> syllables;
    private final long wordCount;
    private final long syllableCount;
    private final long sentenceCount;
    private final CoreMap longestSentence;

    private Book book;

    public OutputWriter(Book book) {
        this.book = book;
        this.subdirectory = book.getSubdirectory();
        this.words = book.getWords();
        this.partsOfSpeech = book.getPartsOfSpeech();
        this.lemmas = book.getLemmas();
        this.syllables = book.getSyllables();
        this.wordCount = book.getWordCount();
        this.syllableCount = book.getSyllableCount();
        this.sentenceCount = book.getSentenceCount();
        this.longestSentence = book.getLongestSentence();
    }

    /**
     * Create folders for result subdirectories
     */
    private void makeResultDirs() {
        makeParentDirs();
        try {
            makeDir("results/txt/" + subdirectory);
            makeDir("results/img/" + subdirectory);
            makeDir("results/json/" + subdirectory);
        } catch (NullPointerException ignore) {
        }
    }

    /**
     * Creates root results directories for files to be saved to.
     */
    private void makeParentDirs() {
        if (!(makeDir("results/txt") || makeDir("results/img")
                || makeDir("results/json"))) {
            System.out.println("[x] Failed to create result directories!");
        }
    }

    /**
     * Create a directory
     *
     * @param path Path of directory to be created
     * @return true if successful
     */
    private boolean makeDir(String path) {
        File dir = new File(path);
        return dir.exists() || dir.mkdirs();
    }

    public void writeTxt() {
        if (partsOfSpeech.size() > 0) {
            makeResultDirs();
            File outFile;

            try {
                outFile = new File("results/txt/" + subdirectory + "/" + book.getName()
                        + " Results.txt");
            } catch (NullPointerException e) {
                outFile = new File("results/txt/" + book.getName() + " Results.txt");
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
                String header = "Auto generated results for " + book.title;
                if (!book.author.isEmpty()) {
                    header += " by " + book.getAuthor();
                }

                bw.write(wrapInBox(header));
                bw.write("\n" + wrapInBox("Stats") + getStats());
                bw.write("\n" + wrapInBox("Conclusion") + getConclusionString());
                bw.write("\n" + wrapInBox("Parts of Speech") + partsOfSpeech.toString());
                bw.write("\n" + wrapInBox("Word Counts") + words.toString());
                bw.write("\n" + wrapInBox("Lemma Counts") + lemmas.toString());
                bw.write("\n" + wrapInBox("Concordance") + createConcordance(words));
                bw.write("\n" + wrapInBox("Lemma Concordance") + createConcordance(lemmas));

                bw.close();
                System.out.println(ANSI_GREEN + "☑ - Finished writing TXT information for " + book.getName() + ANSI_RESET);

            } catch (IOException e) {
                System.out.println("[Error - writeTxt] Error opening " + outFile.getName() + "for writing");
                e.printStackTrace();
            }

        }
    }

    /**
     * Formats a string into a 100 character wide box
     *
     * @param text Text to be placed into box
     * @return Formatted String
     */
    private String wrapInBox(String text) {
        //                   TL   TC   TR   LL   LC   LR   COL
        String[] boxParts = {"╒", "═", "╕", "└", "─", "┘", "│"};
        StringBuilder wrapped = new StringBuilder();

        // Start with TL corner
        wrapped.append(boxParts[0]);
        for (int i = 0; i <= 98; i++) {
            if (i == 98) {
                // Add TR corner
                wrapped.append(boxParts[2]).append("\n");
            } else {
                wrapped.append(boxParts[1]);
            }
        }

        // Add Column and text
        wrapped.append(boxParts[6]).append(" ").append(text);
        for (int i = 0; i <= 97 - text.length(); i++) {
            if (i == 97 - text.length()) {
                wrapped.append(boxParts[6]).append("\n");
            } else {
                wrapped.append(" ");
            }
        }

        // Draw bottom row
        wrapped.append(boxParts[3]);
        for (int i = 0; i <= 98; i++) {
            if (i == 98) {
                wrapped.append(boxParts[5]).append("\n");
            } else {
                wrapped.append(boxParts[4]);
            }
        }
        wrapped.append("\n");
        return wrapped.toString();
    }

    /**
     * Format a Book's data into a list style string
     *
     * @return Formatted string
     */
    private String getStats() {
        return "Total Words: " + wordCount
                + "\nUnique Words: " + words.size()
                + "\nPolysyllabic Words: " + syllables.get("Polysyllabic")
                + "\nMonosyllabic Words: " + syllables.get("Monosyllabic")
                + "\nTotal Syllables: " + syllableCount
                + "\nTotal Sentences: " + sentenceCount
                + "\nFlesch-Kincaid Grade: "
                + TextTools.getReadingEaseLevel(book)
                + "\nClassified Length: " + TextTools.classifyLength(wordCount)
                + "\nTop 3 words: " + words.getTopThree()
                + wrap("\nLongest Sentence; " + longestSentence, 100) + "\n";
    }

    /**
     * Format a Book's data into a paragraph style String wrapped at 100 characters
     *
     * @return Formatted String
     */
    private String getConclusionString() {
        int monoSyllable = syllables.get("Monosyllabic");
        int polySyllable = syllables.get("Polysyllabic");

        String classifiedLength = TextTools.classifyLength(wordCount);
        String gradeLevel = TextTools.getReadingEaseLevel(book);
        String difficulty = TextTools.classifyDifficulty(monoSyllable, polySyllable);

        //TODO format times/
        String conclusion = String.format("This piece is classified as a %s based on the Nebula "
                        + "Award categories. It has a total of %d words with %d of those being "
                        + "unique. Using the Flesch-Kincaid reading ease test, this text is rated "
                        + "at the %s level. Comparing the ratio of (%d) polysyllabic words to (%d) "
                        + "monosyllabic words it can be speculated that this text is %s to read. To"
                        + " read this text at a rate of 275wpm it would take %d minute(s) to finish"
                        + ",to speak at 180wpm, %d minute(s), to type at 40wpm, %d minutes and "
                        + " to write at 13wpm it would take %d minute(s)."
                , classifiedLength, wordCount, words.size(), gradeLevel,
                polySyllable, monoSyllable, difficulty, TextTools.getReadingTimeInMinutes(wordCount),
                TextTools.getSpeakingTimeInMinutes(wordCount), wordCount / 40, wordCount / 13);

        return wrap(conclusion + "\n", 100);
    }

    /**
     * Creates a concordance of all unique words in the text
     *
     * @param words FreqMap to load keys of
     * @return String wrapped at 100 characters
     */
    private String createConcordance(FreqMap words) {
        StringBuilder concordance = new StringBuilder();

        for (String word : words.getSortedKeys()) {
            concordance.append(word).append(" ");
        }

        return wrap(concordance.toString() + "\n", 100);
    }

    public void makeSyllableDistributionGraph() {
        makeGraph("Difficulty", syllables);
    }

    public void makePartsOfSpeechGraph() {
        makeGraph("POS Distribution", partsOfSpeech);
    }

    /**
     * Creates a graph using JFreeChart that is saved to a jpg.
     *
     * @param purpose Purpose of the graph, used in title of graph
     * @param data FreqMap to use values off
     */
    private void makeGraph(String purpose, FreqMap<String, Integer> data) {
        DefaultPieDataset dataSet = new DefaultPieDataset();
        String outPath;
        int resolution = 1000;

        // Create results directories
        if (makeDir("results/img/")) {
            outPath = "results/img/" + subdirectory + "/" + book.getName() + " " + purpose + " Results.jpeg";
        } else {
            System.out.println("[Error] Failed to create image results directories");
            outPath = book.title + "  " + book.author + " " + purpose + "Results.jpeg";
        }

        // Load POS data into data set
        data.toHashMap().entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .forEach(entry -> dataSet.setValue(entry.getKey(), entry.getValue()));

        JFreeChart chart = ChartFactory.createPieChart(
                purpose + " of " + book.getName(), dataSet, false, true, false);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot = setColors(plot, purpose);

        plot.setBaseSectionOutlinePaint(new Color(0, 0, 0));
        plot.setShadowPaint(null);
        plot.setBackgroundPaint(new Color(204, 204, 204));
        plot.setLabelBackgroundPaint(new Color(255, 255, 255));
        plot.setStartAngle(90f);
        plot.setLabelFont(new Font("Ubuntu San Serif", Font.PLAIN, 10));

        // Save the chart to jpeg
        try {
            ChartUtilities.saveChartAsJPEG(new File(outPath), chart, resolution, resolution);
            System.out.println(ANSI_GREEN + "☑ - Finished writing " + purpose
                    + " chart for " + book.getName() + ANSI_RESET);

        } catch (IOException ioe) {
            System.out.println("[Error - makeGraph] Failed to make pie chart " + ioe);
            ioe.printStackTrace();
        }
    }

    /**
     * Sets the colors of a pie chart based on labels in order to keep charts consistent
     *
     * @param chart Chart to modify label colors of
     * @return PieChart with color modifications
     */
    private PiePlot setColors(PiePlot chart, String purpose) {

        if (purpose.equals("Difficulty")) {
            chart.setSectionPaint("Monosyllabic", new Color(77, 77, 77));
            chart.setSectionPaint("Polysyllabic", new Color(241, 88, 84));
        } else if (purpose.equals("POS Distribution")) {
            try {
                InputStreamReader inputStreamReader =
                        new InputStreamReader(TextTools.class.getResourceAsStream("/posAbbreviations.txt"));

                BufferedReader br = new BufferedReader(inputStreamReader);
                String line = br.readLine();

                while (line != null) {
                    String label = line.substring(line.indexOf(":") + 1, line.indexOf(">")).trim();
                    String hexColor = line.substring(line.indexOf(">") + 1).trim();
                    Color color = Color.decode(hexColor);

                    chart.setSectionPaint(label, color);
                    line = br.readLine();
                }
                br.close();
            } catch (IOException e) {
                System.out.println("[Error - setColors] Error reading posAbbreviations file");
                e.printStackTrace();
            }
        }
        return chart;
    }


    @SuppressWarnings("unchecked")
    public String writeJson() {
        String outPath;

        if (makeDir("results/json/")) {
            if (subdirectory != null) {
                outPath = "results/json/" + subdirectory + "/" + book.getName() + " Results.json";
            } else {
                outPath = "results/json/" + "/" + book.getName() + " Results.json";
            }
        } else {
            System.out.println("[Error] Failed to create json results directory");
            outPath = book.getName() + " Results.json";
        }

        File out = new File(outPath);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {

            JSONObject bookJson = new JSONObject();
            bookJson.put("name", book.getName());
            bookJson.put("description", "Parts of speech for " + book.getName());

            String[] posTypes = {"Nouns", "Verbs", "Adverbs", "Adjectives", "Pronouns", "Other"};
            HashMap<String, JSONObject> jsonObjects = new HashMap<>();
            HashMap<String, JSONArray> jsonTypes = new HashMap<>();

            for (String type : posTypes) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", type);
                jsonObject.put("description", type);
                jsonObjects.put(type, jsonObject);

                jsonTypes.put(type, new JSONArray());
            }

            String[] types = partsOfSpeech.getSortedKeys();

            Arrays.stream(types).forEach(type -> {
                // Create temporary parent for each type
                JSONObject typeParent = new JSONObject();
                typeParent.put("name", type);
                typeParent.put("description", type);

                // Basic setup
                JSONArray typeArray = new JSONArray();
                JSONObject typeObject = new JSONObject();

                typeObject.put("name", type);
                typeObject.put("description", type);
                typeObject.put("size", partsOfSpeech.get(type));
                typeArray.add(typeObject);

                //Categorise each type
                typeParent.put("children", typeArray);
                switch (TextTools.getParentType(type)) {
                    case "Noun":
                        jsonTypes.get("Nouns").add(typeParent);
                    case "Verb":
                        jsonTypes.get("Verbs").add(typeParent);
                    case "Adverb":
                        jsonTypes.get("Adverbs").add(typeParent);
                    case "Adjective":
                        jsonTypes.get("Adjectives").add(typeParent);
                    case "Pronoun":
                        jsonTypes.get("Pronouns").add(typeParent);
                    default:
                        jsonTypes.get("Other").add(typeParent);
                }
            });

            JSONArray jsonParent = new JSONArray();

            Arrays.stream(posTypes).forEach(type -> {
                jsonObjects.get(type).put("children", jsonTypes.get(type));
                jsonParent.add(jsonObjects.get(type));
            });

            bookJson.put("children", jsonParent);

            JSONObject rootObject = new JSONObject();
            rootObject.put(book.title, bookJson);


            bw.write(rootObject.toJSONString());
            bw.close();
            System.out.println(ANSI_GREEN + "☑ - Finished writing JSON information for " + book.getName() + ANSI_RESET);
            return rootObject.toJSONString();

        } catch (IOException e) {
            System.out.println("[Error - writeJSON] Error opening " + out.getName() + " for writing");
            makeResultDirs();
            e.printStackTrace();
            writeJson();
        }
        return null;
    }

    String writeCSV() {
        Path outPath;
        if (makeDir("results/csv/")) {
            try {
                outPath = Paths.get("results", "csv", subdirectory, book.getName() + " Results.csv");
            } catch (NullPointerException e) {
                outPath = Paths.get("results", "csv", book.getName() + " Results.csv");
            }
        } else {
            outPath = Paths.get(book.getName() + "Results.csv");
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outPath.toFile()))) {
            bw.write("Word, Count\n");
            bw.write(words.toCsvString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return words.toCsvString();
    }
}
