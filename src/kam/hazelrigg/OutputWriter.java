package kam.hazelrigg;

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
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.text.WordUtils.wrap;
import static org.jfree.chart.ChartFactory.createPieChart;

public class OutputWriter {
    private Book book;

    public OutputWriter(Book book) {
        this.book = book;
    }

    private void makeResultDirs() {
        makeParentDirs();
        if (!book.subdirectory.equals("")) {
            makeDir("results/txt/" + book.subdirectory);
            makeDir("results/img/" + book.subdirectory);
            makeDir("results/json/" + book.subdirectory);
        }
    }

    /**
     * Creates results directories for files to be saved to.
     */
    private void makeParentDirs() {
        if (!(makeDir("results/txt") || makeDir("results/img")
                || makeDir("results/json"))) {
            System.out.println("[x] Failed to create result directories!");
        }
    }

    private boolean makeDir(String path) {
        File dir = new File(path);
        return dir.exists() || dir.mkdirs();
    }

    public void writeTxt() {
        if (book.posFreq.getSize() > 0) {
            makeResultDirs();
            File outFile;

            if (book.subdirectory.equals("")) {
                outFile = new File("results/txt/" + book.getName() + " Results.txt");
            } else {
                outFile = new File("results/txt/" + book.subdirectory + "/" + book.getName()
                        + " Results.txt");
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
                String header = "Auto generated results for " + book.title;
                if (!book.author.isEmpty()) {
                    header += " by " + book.getAuthor();
                }

                bw.write(wrapInBox(header));
                bw.write("\n" + wrapInBox("Stats") + "\n" + getStats());
                bw.write("\n" + wrapInBox("Conclusion") + "\n" + getConclusionString());
                bw.write("\n" + wrapInBox("Parts of Speech Tags") + "\n" + book.posFreq.toString());
                bw.write("\n" + wrapInBox("Concordance") + "\n" + createConcordance());
                bw.write("\n" + wrapInBox("Word Counts") + "\n" + book.wordFreq.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private String wrapInBox(String text) {
        //                   TR   TC   TL   LL   LC   LR   COL
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
        return wrapped.toString();
    }

    private String getStats() {
        return "Total Words: " + book.wordCount
                + "\nUnique Words: " + book.wordFreq.getSize()
                + "\nPolysyllabic Words: " + book.difficultyMap.get("Polysyllabic")
                + "\nMonosyllabic Words: " + book.difficultyMap.get("Monosyllabic")
                + "\nTotal Syllables: " + book.syllableCount
                + "\nTotal Sentences: " + book.sentenceCount
                + "\nFlesch-Kincaid Grade: "
                + TextTools.getReadingEaseLevel(book.wordCount, book.sentenceCount, book.syllableCount)
                + "\nClassified Length: " + TextTools.classifyLength(book.wordCount)
                + "\nTop 3 words: " + book.wordFreq.getTopThree()
                + wrap("\nLongest Sentence: "
                + TextTools.convertHasWordToString(book.longestSentence), 100) + "\n";
    }

    private String getConclusionString() {
        long wordCount = book.wordCount;
        long sentenceCount = book.sentenceCount;
        long syllableCount = book.syllableCount;
        int monoSyllable = book.difficultyMap.get("Monosyllabic");
        int polySyllable = book.difficultyMap.get("Polysyllabic");

        String classifiedLength = TextTools.classifyLength(wordCount);
        String gradeLevel = TextTools.getReadingEaseLevel(wordCount, sentenceCount, syllableCount);
        String difficulty = TextTools.classifyDifficulty(monoSyllable, polySyllable);

        //TODO format times/
        String conclusion = String.format("This piece is classified as a %s based on the Nebula "
                        + "Award categories. It has a total of %d words with %d of those being "
                        + "unique. Using the Flesch-Kincaid reading ease test, this text is rated "
                        + "at the %s level. Comparing the ratio of (%d) polysyllabic words to (%d) "
                        + "monosyllabic words it can be speculated that this text is %s to read. To"
                        + " read this text at a rate of 275wpm it would take %d minute(s) to finish"
                        + ", %d minute(s) to speak at 180wpm, %d minutes to type at 40wpm and "
                        + "%d minute(s) to write at 13wpm.", classifiedLength, wordCount,
                book.wordFreq.getSize(), gradeLevel, polySyllable, monoSyllable, difficulty
                , TextTools.getReadingTime(wordCount), TextTools.getSpeakingTime(wordCount)
                , wordCount / 40, wordCount / 13);

        return wrap(conclusion + "\n", 100);
    }

    /**
     * Creates a concordance of all unique words in the text
     *
     * @return String wrapped at 100 characters
     */
    private String createConcordance() {
        StringBuilder concordance = new StringBuilder();

        for (String word : book.wordFreq.getSortedByKey()) {
            concordance.append(word).append(" ");
        }

        return wrap(concordance.toString(), 100);
    }

    /**
     * Creates a parts of speech distribution pie graph.
     */
    public void makePosGraph() {
        makeGraph("POS Distribution", book.posFreq);
    }

    /**
     * Creates a difficulty pie graph that uses syllable.
     */
    public void makeDifficultyGraph() {
        makeGraph("Difficulty", book.difficultyMap);
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
            outPath = "results/img/" + book.subdirectory + "/" + book.getName() + " " + purpose + " Results.jpeg";
        } else {
            System.out.println("[Error] Failed to create image results directories");
            outPath = book.title + "  " + book.author + " " + purpose + "Results.jpeg";
        }


        // Load POS data into data set
        HashMap<String, Integer> map = freq.getFrequency();

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            dataSet.setValue(type, count);
        }

        JFreeChart chart = createPieChart(
                purpose + " of " + book.getName(),
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

    @SuppressWarnings("unchecked")
    public void writeJson() {
        File out;
        // Create results directories
        if (book.subdirectory.equals("")) {
            out = new File("results/json/" + book.getName() + " Results.json");
        } else {
            out = new File("results/json/" + book.subdirectory + "/" + book.getName()
                    + " Results.json");
        }


        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {

            /* TODO Find a way to reduce redundancy
               Maybe a list or hashmap could do that */
            JSONObject json = new JSONObject();
            json.put("name", book.getName());
            json.put("description", "Parts of speech for " + book.getName());

            JSONObject nouns = new JSONObject();
            nouns.put("name", "Nouns");
            nouns.put("description", "Nouns");

            JSONObject verbs = new JSONObject();
            verbs.put("name", "Verbs");
            verbs.put("description", "Verbs");

            JSONObject adverbs = new JSONObject();
            adverbs.put("name", "Adverbs");
            adverbs.put("description", "Adverbs");

            JSONObject adjective = new JSONObject();
            adjective.put("name", "Adjectives");
            adjective.put("description", "Adjectives");

            JSONObject pronouns = new JSONObject();
            pronouns.put("name", "Pronouns");
            pronouns.put("description", "Pronouns");

            JSONObject others = new JSONObject();
            others.put("name", "Other");
            others.put("description", "Other");

            JSONArray nounTypes = new JSONArray();
            JSONArray verbTypes = new JSONArray();
            JSONArray adverbTypes = new JSONArray();
            JSONArray adjectiveTypes = new JSONArray();
            JSONArray pronounTypes = new JSONArray();
            JSONArray otherTypes = new JSONArray();

            for (String type : book.posFreq.keySet()) {
                // Create temporary parent for each type
                JSONObject parent = new JSONObject();
                parent.put("name", type);
                parent.put("description", type);

                // Basic setup
                JSONArray array = new JSONArray();
                JSONObject object = new JSONObject();
                object.put("name", type);
                object.put("description", type);
                object.put("size", book.posFreq.get(type));
                array.add(object);
                parent.put("children", array);

                //Categorise each type
                if (TextTools.getParentType(type).equals("Noun")) {
                    nounTypes.add(parent);
                } else if (TextTools.getParentType(type).equals("Verb")) {
                    verbTypes.add(parent);
                } else if (TextTools.getParentType(type).equals("Adverb")) {
                    adverbTypes.add(parent);
                } else if (TextTools.getParentType(type).equals("Adjective")) {
                    adjectiveTypes.add(parent);
                } else if (TextTools.getParentType(type).equals("Pronoun")) {
                    pronounTypes.add(parent);
                } else {
                    otherTypes.add(parent);
                }
            }

            // Give each parent a child
            nouns.put("children", nounTypes);
            verbs.put("children", verbTypes);
            adverbs.put("children", adverbTypes);
            adjective.put("children", adjectiveTypes);
            pronouns.put("children", pronounTypes);
            others.put("children", otherTypes);


            // Add all parent speech types to root parent
            JSONArray rootParent = new JSONArray();
            rootParent.add(nouns);
            rootParent.add(verbs);
            rootParent.add(adverbs);
            rootParent.add(adjective);
            rootParent.add(pronouns);
            rootParent.add(others);

            json.put("children", rootParent);

            bw.write(json.toJSONString());
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("☑ - Finished writing JSON information for " + book.getName());

    }

}
