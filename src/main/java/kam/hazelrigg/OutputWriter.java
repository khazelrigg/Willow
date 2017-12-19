package kam.hazelrigg;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import static org.apache.commons.lang3.text.WordUtils.wrap;

class OutputWriter {
    private static Logger logger = Willow.getLogger();
    private final String subdirectory;
    private final Book book;
    private final BookStats bookStats;
    private final String title;

    OutputWriter(Book book) {
        this.book = book;
        this.subdirectory = book.getSubdirectory();
        this.bookStats = book.getStats();
        this.title = book.getTitle();
    }

    boolean writeTxt() {
        FreqMap partsOfSpeech = bookStats.getPartsOfSpeech();
        FreqMap words = bookStats.getWords();
        FreqMap lemmas = bookStats.getLemmas();

        if (partsOfSpeech.size() > 0) {
            Path outPath = getOutPath("txt", "Text", "txt");


            try (BufferedWriter bw = Files.newBufferedWriter(outPath)) {
                String header = "Auto generated results for " + book.getName();

                bw.write(wrapInBox(header));
                bw.write("\n" + wrapInBox("Stats") + bookStats.toFormattedString());
                bw.write("\n" + wrapInBox("Conclusion"));
                writeConclusionString(bw);

                bw.write("\n" + wrapInBox("Parts of Speech") + partsOfSpeech.toString());
                bw.write("\n" + wrapInBox("Word Counts") + words.toString());
                bw.write("\n" + wrapInBox("Concordance") + createConcordance(words));

                bw.write("\n" + wrapInBox("Lemma Counts") + lemmas.toString());
                bw.write("\n" + wrapInBox("Lemma Concordance") + createConcordance(lemmas));

                printFinishedStatement("TXT");
                return true;
            } catch (IOException e) {
                logger.error("Error writing to {}", outPath.toString());
            }
        }
        return false;
    }

    private void writeConclusionString(BufferedWriter bw) throws IOException {
        String classifiedLength = bookStats.getClassifiedLength();
        String gradeLevel = bookStats.getGradeLevel();
        String easyDifficult = bookStats.getEasyDifficult();
        long wordCount = bookStats.getWordCount();
        long uniqueWords = bookStats.getUniqueWords();
        long polySyllable = bookStats.getPolysyllablic();
        long monoSyllable = bookStats.getMonosyllablic();

        bw.write(wrap(String.format("This piece is classified as a %s based on the Nebula "
                        + "Award categories. It has a total of %d words with %d of those being "
                        + "unique. Using the Flesch-Kincaid reading ease test, this text is rated "
                        + "at the %s level."
                        + "Comparing the ratio of (%d) polysyllabic words to (%d) "
                        + "monosyllabic words it can be speculated that this text is %s to read. To"
                        + " read this text at a rate of 275wpm it would take %d minute(s) to finish"
                        + ",to speak at 180wpm, %d minute(s), to type at 40wpm, %d minutes and "
                        + " to write at 13wpm it would take %d minute(s).%n",
                classifiedLength, wordCount, uniqueWords, gradeLevel,
                polySyllable, monoSyllable, easyDifficult,
                getReadingTimeInMinutes(), getSpeakingTimeInMinutes(),
                getTypingTimeInMinutes(), getWritingTimeInMinutes()), 100));
    }

    private String createConcordance(FreqMap words) {
        StringBuilder concordance = new StringBuilder();

        for (String word : words.getSortedKeys()) {
            concordance.append(word).append(" ");
        }

        return wrap(concordance.toString() + "\n", 100);
    }

    boolean makeSyllableDistributionGraph() {
        return makeGraph("Difficulty", bookStats.getSyllables());
    }

    boolean makePartsOfSpeechGraph() {
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


        PiePlot plot = setColors((PiePlot) chart.getPlot(), purpose);

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
            logger.error("Failed to make pie chart at {}", outPath);
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
                                OutputWriter.class.getResourceAsStream("/posAbbreviations.txt")
                                , StandardCharsets.UTF_8);

                BufferedReader br = new BufferedReader(inputStreamReader);
                String line = br.readLine();

                while (line != null) {
                    String label = line.substring(line.indexOf(':') + 1, line.indexOf('>')).trim();
                    String hexColor = line.substring(line.indexOf('>') + 1).trim();
                    Color color = Color.decode(hexColor);

                    chart.setSectionPaint(label, color);
                    line = br.readLine();
                }
                br.close();
            } catch (IOException e) {
                logger.error("Error reading posAbbreviations file to set pie chart colors");
            }
        }
        return chart;
    }

    @SuppressWarnings("unchecked")
    boolean writeJson() {
        Path outPath = getOutPath("json", "JSON", "json");
        String d3Children = "children";

        try (BufferedWriter bw = Files.newBufferedWriter(outPath)) {
            String description = "description";

            JSONObject bookJson = new JSONObject();
            bookJson.put("name", book.getName());
            bookJson.put(description, "Parts of speech for " + book.getName());

            String[] posTypes = {"Nouns", "Verbs", "Adverbs", "Adjectives", "Pronouns", "Other"};
            HashMap<String, JSONObject> jsonObjects = new HashMap<>();
            HashMap<String, JSONArray> jsonTypes = new HashMap<>();

            for (String type : posTypes) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", type);
                jsonObject.put(description, type);
                jsonObjects.put(type, jsonObject);

                jsonTypes.put(type, new JSONArray());
            }

            String[] types = bookStats.getPartsOfSpeech().getSortedKeys();

            Arrays.stream(types).forEach((String type) -> {

                // Create temporary parent for each type
                JSONObject typeParent = new JSONObject();
                typeParent.put("name", type);
                typeParent.put(description, type);

                // Basic setup
                JSONObject typeObject = new JSONObject();
                typeObject.put("name", type);
                typeObject.put(description, type);
                typeObject.put("size", bookStats.getPartsOfSpeech().get(type));

                JSONArray typeArray = new JSONArray();
                typeArray.add(typeObject);

                //Categorise each type
                typeParent.put(d3Children, typeArray);
                type = getParentType(type);
                switch (type) {
                    case "Noun":
                        jsonTypes.get(posTypes[0]).add(typeParent);
                        break;
                    case "Verb":
                        jsonTypes.get(posTypes[1]).add(typeParent);
                        break;
                    case "Adverb":
                        jsonTypes.get(posTypes[2]).add(typeParent);
                        break;
                    case "Adjective":
                        jsonTypes.get(posTypes[3]).add(typeParent);
                        break;
                    case "Pronoun":
                        jsonTypes.get(posTypes[4]).add(typeParent);
                        break;
                    default:
                        jsonTypes.get(posTypes[5]).add(typeParent);
                }
            });

            JSONArray jsonParent = new JSONArray();

            Arrays.stream(posTypes).forEach(type -> {
                jsonObjects.get(type).put(d3Children, jsonTypes.get(type));
                jsonParent.add(jsonObjects.get(type));
            });

            bookJson.put(d3Children, jsonParent);

            JSONObject rootObject = new JSONObject();
            rootObject.put(title, bookJson);

            bw.write(rootObject.toJSONString());

            printFinishedStatement("JSON");
            return true;
        } catch (IOException e) {
            logger.error("Unable to write JSON results to {}", outPath.toString());
        }
        return false;
    }

    boolean writeCsv() {
        Path outPath = getOutPath("csv", "CSV", "csv");
        FreqMap<String, Integer> words = bookStats.getWords();


        try (BufferedWriter bw = Files.newBufferedWriter(outPath)) {
            bw.write("Word, Count\n");
            bw.write(words.toCsvString());
            printFinishedStatement("CSV");
            return true;
        } catch (IOException e) {
            logger.error("Unable to write CSV results to {}", outPath);
        }
        return false;
    }

    private Path getOutPath(String typeFolder, String descriptor, String extension) {
        Path outPath;
        String rootFolder = "results";
        String fileName = book.getName() + " " + descriptor + " Results." + extension;
        boolean typeFolderExists = makeDir(Paths.get(rootFolder, typeFolder));

        if (typeFolderExists) {
            if (!subdirectory.isEmpty()) {
                makeDir(Paths.get(rootFolder, typeFolder, subdirectory));
                outPath = Paths.get(rootFolder, typeFolder, subdirectory, fileName);
            } else {
                outPath = Paths.get(rootFolder, typeFolder, fileName);
            }
        } else {
            logger.error("Failed to create results directory {}", typeFolder);
            outPath = Paths.get(fileName);
        }
        return outPath;
    }

    private boolean makeDir(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            logger.error("Failed to create directory at {}", path.toString());
        }
        return path.toFile().isDirectory();
    }

    private void printFinishedStatement(String action) {
        logger.info("Finished writing {} for {}", action, book.getName());
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

    private int getReadingTimeInMinutes() {
        long wordCount = book.getStats().getWordCount();
        return (int) wordCount / 275;
    }

    private int getSpeakingTimeInMinutes() {
        long wordCount = book.getStats().getWordCount();
        return (int) wordCount / 180;
    }

    private int getTypingTimeInMinutes() {
        long wordCount = book.getStats().getWordCount();
        return (int) wordCount / 40;
    }

    private int getWritingTimeInMinutes() {
        long wordCount = book.getStats().getWordCount();
        return (int) wordCount / 13;
    }

    private static String getParentType(String type) {
        type = type.toLowerCase();

        if (type.contains("pronoun")) {
            return "Pronoun";
        }

        if (type.contains("noun")) {
            return "Noun";
        }

        if (type.contains("adverb")) {
            return "Adverb";
        }

        if (type.contains("verb")) {
            return "Verb";
        }

        if (type.contains("adjective")) {
            return "Adjective";
        }

        return "Other";

    }

}
