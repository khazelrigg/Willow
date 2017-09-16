package kam.hazelrigg;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    final private static String fileName = getFileName();
    private static String resultsFile = "";

    public static void main(String[] args) {

        if (fileName != null) {
            resultsFile = fileName.substring(0, fileName.length() - 4) + "Results.txt";

            if (hasResults()) {
                System.out.println("[NOTE] " + fileName + " already has results");
                readCount();
            } else {
                Map<String, Integer> pos = sortByValue(partOfSpeech());

                writeCount(wordCount(), pos);
                readCount();
                makeGraph(pos);
            }

        } else {
            System.out.println("[Error - Main] File not found/is not .txt");
        }

    }

    private static String getFileName() {
        // Get input (fileName) from user and check its validity

        Scanner kb = new Scanner(System.in);
        System.out.print("File path: ");
        String input = kb.nextLine();

        if (input.length() < 4) return null;
        if (!input.endsWith(".txt")) return null;
        return input;
    }

    private static boolean hasResults() {
        // Find out if a file already has a results file

        try {
            File file = new File(resultsFile);
            System.out.println("[FILE] " + resultsFile);
            if (file.exists() && !file.isDirectory()) return true;
        } catch (NullPointerException nullptr) {
            return false;
        }
        return false;
    }

    private static void readCount() {
        // Read Results file for counts

        try {
            Scanner in = new Scanner(new FileReader(resultsFile));
            System.out.println("\n----[ Using results from " + fileName + " ]----\n");

            while (in.hasNext()) {
                System.out.println(in.nextLine());
            }

            in.close();
        } catch (FileNotFoundException notFound) {
            System.out.println("[Error - readCount] File not found: " + notFound);
        }
    }


    private static void writeCount(Map<String, Integer> wordFreq, Map<String, Integer> wordType) {
        // Write the word counts to a file

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(resultsFile));
            System.out.println("[WRITE] " + resultsFile);

            // Write word frequency
            for (String word : wordFreq.keySet()) {
                out.write(word + ", " + wordFreq.get(word) + "\n");
            }

            out.write("\n");

            // Write POS tags to the bottom of the file
            for (String type : wordType.keySet()) {
                out.write(type + ", " + wordType.get(type) + "\n");
            }

            out.close();
        } catch (java.io.IOException ioExc) {
            System.out.println("[Error - writeCount] Failed to write file: " + ioExc);
        }
    }


    private static Map<String, Integer> wordCount() {
        // Count the frequency of a words appearance

        try {
            Scanner in = new Scanner(new FileReader(fileName));

            Map<String, Integer> wordFreq = new HashMap<>();

            String stopWords = "this but are on that have the of to and a an in is it for ";

            while (in.hasNext()) {
                // Split line into separate words
                String[] line = in.nextLine().split("\\s");

                for (String word : line) {
                    word = word.toLowerCase();

                    if (stopWords.contains(word + " ")) continue; // If the word is a stop word skip over it

                    // Remove punctuation from each word
                    word = word.replaceAll("\\W", "");

                    wordFreq = addFreq(wordFreq, word);
                }
            }

            in.close();

            wordFreq = sortByValue(wordFreq); // Sort map wordFreq before returning to make results easier to understand
            return wordFreq;

        } catch (FileNotFoundException notFound) {
            System.out.println("[Error - wordCount] File not found: " + notFound);
            return null;
        }
    }

    private static Map<String, Integer> addFreq(Map<String, Integer> map, String key) {
        // Add keys into map with a count

        if (map.containsKey(key))
            map.put(key, map.get(key) + 1);
        else
            map.put(key, 1);
        return map;
    }

    // Incorporate partOfSpeech into wordCount so that only one pass is being made over file

    private static Map<String, Integer> partOfSpeech() {
        // Count the frequency of POS

        try {
            Map<String, String> posAbbrev = new HashMap<>();
            Map<String, Integer> posCounts = new HashMap<>();

            Scanner in = new Scanner(new FileReader(fileName));
            Scanner posAbbrevIn = new Scanner(new FileReader("posAbbreviations.txt"));

            String trainedFile = "models/english-bidirectional-distsim.tagger";
            MaxentTagger tagger = new MaxentTagger(trainedFile);

            // Unabbreviate results for easier understanding
            while (posAbbrevIn.hasNext()) {
                String[] line = posAbbrevIn.nextLine().split(":");
                posAbbrev.put(line[0].trim(), line[1].trim());
            }

            while (in.hasNextLine()) {
                String[] line = in.nextLine().split("\\s");

                for (String word : line) {
                    word = word.replaceAll("\\W", "");
                    if (word.length() == 0) continue;  // Catch words that are only whitespace

                    // Tag word with its POS
                    String tagged = tagger.tagString(word);
                    String tagType = tagged.substring(word.length()).replace("_", "")
                            .toLowerCase().trim();

                    // Get the non abbreviated form as tagType
                    tagType = posAbbrev.get(tagType);

                    if (tagType == null) {
                        System.out.println("[UNKNOWN] " + line[0]);
                        tagType = "Unknown";
                    }

                    if (posCounts.containsKey(tagType))
                        posCounts.put(tagType, posCounts.get(tagType) + 1);
                    else posCounts.put(tagType, 1);
                }
            }
            return posCounts;

        } catch (IOException ioe) {
            System.out.println("[Error - partOfSpeech] File not found: " + ioe);
            return null;
        }
    }

    private static void makeGraph(Map<String, Integer> posMap) {
        // Create an image representing the distribution of parts of speech

        DefaultPieDataset dataset = new DefaultPieDataset();

        // Load POS data into dataset
        for (String type : posMap.keySet()) {
            dataset.setValue(type, posMap.get(type));
        }

        JFreeChart chart = ChartFactory.createPieChart3D(
                "Parts of Speech in " + fileName,
                dataset,
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

        File pieChart = new File(resultsFile.replaceAll(".txt", "") + ".jpg");

        try {
            ChartUtilities.saveChartAsJPEG(pieChart, chart, 900, 900);
        } catch (IOException ioe) {
            System.out.println("[Error - makeGraph] Failed to make pie chart " + ioe);
        }
    }

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        // Found on stack overflow

        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                //                                  Sort descending instead of ascending
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

}
