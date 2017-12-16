package kam.hazelrigg;

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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import static org.apache.commons.lang3.text.WordUtils.wrap;

public class OutputWriter {
    static final String ANSI_RESET = "\u001B[0m";
    static final String ANSI_GREEN = "\u001B[32m";
    private final String subdirectory;
    private final Book book;
    private final BookStats bookStats;
    private final String title;
    private boolean verbose = false;

    public OutputWriter(Book book) {
        this.book = book;
        this.subdirectory = book.getSubdirectory();
        this.bookStats = book.getStats();
        this.title = book.getTitle();
    }

    public boolean writeTxt() {
        FreqMap partsOfSpeech = bookStats.getPartsOfSpeech();
        FreqMap words = bookStats.getWords();
        FreqMap lemmas = bookStats.getLemmas();

        if (partsOfSpeech.size() > 0) {
            Path outPath = getOutPath("txt", "Text", "txt");


            try (BufferedWriter bw = Files.newBufferedWriter(outPath)) {
                String header = "Auto generated results for " + book.getName();

                bw.write(wrapInBox(header));
                bw.write("\n" + wrapInBox("Stats") + bookStats.toFormattedString());
                bw.write("\n" + wrapInBox("Conclusion") + getConclusionString());
                bw.write("\n" + wrapInBox("Parts of Speech") + partsOfSpeech.toString());
                bw.write("\n" + wrapInBox("Word Counts") + words.toString());
                bw.write("\n" + wrapInBox("Lemma Counts") + lemmas.toString());
                bw.write("\n" + wrapInBox("Concordance") + createConcordance(words));
                bw.write("\n" + wrapInBox("Lemma Concordance") + createConcordance(lemmas));

                bw.close();

                printFinishedStatement("TXT");
                return true;
            } catch (IOException e) {
                System.out.println("[Error - writeTxt] Error opening " + outPath.toString()
                        + "for writing");
                e.printStackTrace();
            }
        }
        return false;
    }

    private String getConclusionString() {
        String classifiedLength = bookStats.getClassifiedLength();
        long wordCount = bookStats.getWordCount();
        long uniqueWords = bookStats.getUniqueWords();
        String gradeLevel = bookStats.getGradeLevel();
        long polySyllable = bookStats.getPolysyllablic();
        long monoSyllable = bookStats.getMonosyllablic();
        String easyDifficult = bookStats.getEasyDifficult();


        //TODO format times/
        String conclusion = String.format("This piece is classified as a %s based on the Nebula "
                        + "Award categories. It has a total of %d words with %d of those being "
                        + "unique. Using the Flesch-Kincaid reading ease test, this text is rated "
                        + "at the %s level. Comparing the ratio of (%d) polysyllabic words to (%d) "
                        + "monosyllabic words it can be speculated that this text is %s to read. To"
                        + " read this text at a rate of 275wpm it would take %d minute(s) to finish"
                        + ",to speak at 180wpm, %d minute(s), to type at 40wpm, %d minutes and "
                        + " to write at 13wpm it would take %d minute(s).",
                classifiedLength, wordCount, uniqueWords, gradeLevel,
                polySyllable, monoSyllable, easyDifficult,
                getReadingTimeInMinutes(wordCount), getSpeakingTimeInMinutes(wordCount),
                wordCount / 40, wordCount / 13);

        return wrap(conclusion + "\n", 100);
    }

    private String createConcordance(FreqMap words) {
        StringBuilder concordance = new StringBuilder();

        for (String word : words.getSortedKeys()) {
            concordance.append(word).append(" ");
        }

        return wrap(concordance.toString() + "\n", 100);
    }

    public boolean makeSyllableDistributionGraph() {
        return makeGraph("Difficulty", bookStats.getSyllables());
    }

    public boolean makePartsOfSpeechGraph() {
        return makeGraph("POS Distribution", bookStats.getPartsOfSpeech());
    }

    private boolean makeGraph(String purpose, FreqMap<String, Integer> data) {
        DefaultPieDataset dataSet = new DefaultPieDataset();
        int resolution = 1000;

        Path outPath = getOutPath("img", purpose, "jpeg");

        // Load POS data into data set
        data.toHashMap().entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .forEach(entry -> dataSet.setValue(entry.getKey(), entry.getValue()));

        JFreeChart chart = ChartFactory.createPieChart(
                purpose + " of " + book.getName(), dataSet,
                false, true, false);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot = setColors(plot, purpose);

        plot.setBaseSectionOutlinePaint(new Color(0, 0, 0));
        plot.setShadowPaint(null);
        plot.setBackgroundPaint(new Color(204, 204, 204));
        plot.setLabelBackgroundPaint(new Color(255, 255, 255));
        plot.setStartAngle(90f);
        plot.setLabelFont(new Font("Ubuntu San Serif", Font.PLAIN, 10));

        try {
            OutputStream imageStream = Files.newOutputStream(outPath);
            ChartUtilities.writeChartAsJPEG(imageStream, chart, resolution, resolution);
            printFinishedStatement(purpose + " chart");
            return true;
        } catch (IOException ioe) {
            System.out.println("[Error - makeGraph] Failed to make pie chart " + ioe);
            ioe.printStackTrace();
        }
        return false;
    }

    private PiePlot setColors(PiePlot chart, String purpose) {

        if (purpose.equals("Difficulty")) {
            chart.setSectionPaint("Monosyllabic", new Color(77, 77, 77));
            chart.setSectionPaint("Polysyllabic", new Color(241, 88, 84));
        } else if (purpose.equals("POS Distribution")) {
            try {
                InputStreamReader inputStreamReader =
                        new InputStreamReader(
                                OutputWriter.class.getResourceAsStream("/posAbbreviations.txt"));

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
    public boolean writeJson() {
        Path outPath = getOutPath("json", "JSON", "json");

        try (BufferedWriter bw = Files.newBufferedWriter(outPath)) {

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

            String[] types = bookStats.getPartsOfSpeech().getSortedKeys();

            Arrays.stream(types).forEach(type -> {
                // Create temporary parent for each type
                JSONObject typeParent = new JSONObject();
                typeParent.put("name", type);
                typeParent.put("description", type);

                // Basic setup
                JSONObject typeObject = new JSONObject();
                typeObject.put("name", type);
                typeObject.put("description", type);
                typeObject.put("size", bookStats.getPartsOfSpeech().get(type));

                JSONArray typeArray = new JSONArray();
                typeArray.add(typeObject);

                //Categorise each type
                typeParent.put("children", typeArray);
                switch (getParentType(type)) {
                    case "Noun":
                        jsonTypes.get("Nouns").add(typeParent);
                        break;
                    case "Verb":
                        jsonTypes.get("Verbs").add(typeParent);
                        break;
                    case "Adverb":
                        jsonTypes.get("Adverbs").add(typeParent);
                        break;
                    case "Adjective":
                        jsonTypes.get("Adjectives").add(typeParent);
                        break;
                    case "Pronoun":
                        jsonTypes.get("Pronouns").add(typeParent);
                        break;
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
            rootObject.put(title, bookJson);

            bw.write(rootObject.toJSONString());
            bw.close();

            printFinishedStatement("JSON");

            return true;

        } catch (IOException e) {
            System.out.println("[Error - writeJSON] Error opening " + outPath.toString()
                    + " for writing");
            e.printStackTrace();
        }
        return false;
    }

    String writeCsv() {
        Path outPath = getOutPath("csv", "CSV", "csv");
        FreqMap<String, Integer> words = bookStats.getWords();


        try (BufferedWriter bw = Files.newBufferedWriter(outPath)) {
            bw.write("Word, Count\n");
            bw.write(words.toCsvString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        printFinishedStatement("CSV");
        return words.toCsvString();
    }

    private Path getOutPath(String typeFolder, String descriptor, String extension) {
        Path outPath;
        String fileName = book.getName() + " " + descriptor + " Results." + extension;
        boolean typeFolderExists = makeDir(Paths.get("results", typeFolder));

        if (typeFolderExists) {
            if (!subdirectory.isEmpty()) {
                makeDir(Paths.get("results", typeFolder, subdirectory));
                outPath = Paths.get("results", typeFolder, subdirectory, fileName);
            } else {
                outPath = Paths.get("results", typeFolder, fileName);
            }
        } else {
            System.out.println("[Error] Failed to create txt results directory");
            outPath = Paths.get(fileName);
        }
        return outPath;
    }

    private boolean makeDir(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            System.out.println("Failed to create output folder at " + path.toString());
            e.printStackTrace();
        }
        return Files.isDirectory(path);
    }

    private void printFinishedStatement(String action) {
        if (verbose) {
            System.out.println(ANSI_GREEN + "☑ - Finished writing " + action + " output for "
                    + book.getName() + ANSI_RESET);
        }
    }

    private static String wrapInBox(String text) {
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

    void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private static int getReadingTimeInMinutes(long wordCount) {
        return (int) (wordCount / 275);
    }

    private static int getSpeakingTimeInMinutes(long wordCount) {
        return (int) (wordCount / 180);
    }

    private static String getParentType(String type) {

        if (type.equals("Noun, singular or mass") || type.equals("Noun, plural")
                || type.equals("Proper noun, singular")
                || type.equals("Proper noun, plural")) {
            return "Noun";
        }

        if (type.equals("Verb, base form") || type.equals("Verb, past tense")
                || type.equals("Verb, gerund or present participle")
                || type.equals("Verb, past participle")
                || type.equals("Verb, non-3rd person singular present")
                || type.equals("Verb, 3rd person singular present")) {
            return "Verb";
        }

        if (type.equals("Adverb") || type.equals("Adverb, comparative")
                || type.equals("Adverb, superlative") || type.equals("Wh-adverb")) {
            return "Adverb";
        }

        if (type.equals("Adjective") || type.equals("Adjective, comparative")
                || type.equals("Adjective, superlative")) {
            return "Adjective";
        }

        if (type.equals("Personal pronoun") || type.equals("Possessive pronoun")
                || type.equals("Possessive wh pronoun") || type.equals("Wh-pronoun")) {
            return "Pronoun";
        }

        return "Other";

    }

}
