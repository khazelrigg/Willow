package kam.hazelrigg;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static int totalWordCount = 0;

    private static final String FILE_NAME = getFileName();
    private static final String RESULTS_FILE =
            "results/" + FILE_NAME.substring(0, FILE_NAME.length() - 4) + "Results.txt";

    // Set up tagger
    private static MaxentTagger tagger =
            new MaxentTagger("models/english-bidirectional-distsim.tagger");

    public static void main(String[] args) {

        //long startTime = System.currentTimeMillis();

        if (hasResults()) {
            System.out.println("[NOTE] " + FILE_NAME + " already has results!");
            readCount();
        } else {
            try {
                Map<String, Map<String, Integer>> counts = wordCount();
                writeCount(counts);
                readCount();
                makeGraph(counts.get("POS"));
            } catch (IOException ioe) {
                System.out.println("[Error - Main] " + ioe);
                System.exit(-1);
            }
        }

        //long endTime = System.currentTimeMillis();
        //System.out.println("\nTIME TO RUN: " + (endTime - startTime) + "ms");
    }

    private static String getFileName() {
        // Get a filename and check that the file exists

        Scanner kb = new Scanner(System.in);
        while (true) {
            System.out.print("File path: ");
            String input = kb.nextLine();
            File file = new File(input);

            if (file.exists() && !file.isDirectory()) {
                return input;
            } else {
                System.out.println("Try again, no file found at " + input);
            }
        }
    }

    private static boolean hasResults() {
        // Find out if a file already has a results file

        try {
            File file = new File(RESULTS_FILE);
            if (file.exists() && !file.isDirectory()) {
                return true;
            }
        } catch (NullPointerException nullptr) {
            return false;
        }

        return false;
    }

    private static Map<String, Map<String, Integer>> wordCount() throws IOException {
        // Count the frequency of a words appearance

        Map<String, Map<String, Integer>> results = new HashMap<>();

        FreqMap posFreq = new FreqMap();
        FreqMap wordFreq = new FreqMap();

        HashMap<String, Integer> otherMap = new HashMap<>();

        otherMap.put("Palindrome", 0);

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {

            String stopWords = "which|was|what|has|have|this|that|the|of|to|and|a|an|as|are|on|in|is|it|so|for|be|been|by|but|";

            while (br.readLine() != null) {

                // Split line into separate words
                String[] line = br.readLine().split("\\s");

                for (String word : line) {
                    word = word.toLowerCase().replaceAll("\\W", "");

                    // If the word is a stop word skip over it
                    if (word.length() == 0 || stopWords.contains(word + "|")) {
                        continue;
                    }

                    totalWordCount++;

                    // Tag each word
                    String tagType = getTag(word);

                    // Add counts and tags to corresponding maps
                    posFreq.increaseFreq(tagType);
                    wordFreq.increaseFreq(word);

                    results.put("WORD", wordFreq.getFrequency());
                    results.put("POS", posFreq.getFrequency());


                    if (isPalindrome(word)) {
                        otherMap.put("Palindrome", otherMap.get("Palindrome") + 1);
                    }

                    results.put("OTHER", otherMap);
                }
            }

            br.close();

            return results;

        }
    }

    private static String getTag(String word) {
        // Get non abbreviated tags
        Map<String, String> posAbbrev = nonAbbreviate();

        // Tag word with its POS
        String tagged = tagger.tagString(word);
        String tagType = tagged.substring(word.length()).replace("_", "")
                .toLowerCase().trim();

        tagType = posAbbrev.get(tagType);

        if (tagType == null) {
            System.out.println("[UNKNOWN TAG] " + word);
            tagType = "Unknown";
        }

        return tagType;
    }

    private static HashMap<String, String> nonAbbreviate() {
        HashMap<String, String> posNoAbbrev = new HashMap<>();
        try {
            Scanner in = new Scanner(new FileReader("posAbbreviations.txt"));
            while (in.hasNextLine()) {
                String[] line = in.nextLine().split(":");
                posNoAbbrev.put(line[0].trim(), line[1].trim());
            }
            return posNoAbbrev;
        } catch (FileNotFoundException notFound) {
            System.out.println("[Error - nonAbbreviate] " + notFound);
        }

        return posNoAbbrev;
    }

    private static boolean isPalindrome(String str) {
        for (int i = 0; i < str.length() / 2; i++) {
            if (str.charAt(i) != str.charAt(str.length() - 1 - i)) {
                return false;
            }
        }

        return true;
    }

    private static void writeCount(Map<String, Map<String, Integer>> counts) {
        // Write the word counts to a file

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(RESULTS_FILE));
            System.out.println("[WRITE] " + RESULTS_FILE);
            out.write("Total word count (Excluding Stop Words): " + totalWordCount + "\n");

            // Write counts information
            for (String id : counts.keySet()) {
                out.write("====================[ " + id + " ]====================\n");
                for (String key : counts.get(id).keySet()) {
                    out.write(key + ", " + counts.get(id).get(key) + "\n");
                }
                out.write("\n");
            }

            out.close();
        } catch (java.io.IOException ioExc) {
            System.out.println("[Error - writeCount] Failed to write file: " + ioExc);
        }
    }

    private static void readCount() {
        // Read Results file for counts

        try {
            Scanner in = new Scanner(new FileReader(RESULTS_FILE));
            System.out.println("\n----[ Using results from " + FILE_NAME + " ]----\n");

            while (in.hasNext()) {
                System.out.println(in.nextLine());
            }
            in.close();
        } catch (FileNotFoundException notFound) {
            System.out.println("[Error - readCount] File not found: " + notFound);
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
                "Parts of Speech in " + FILE_NAME,
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

        File pieChart = new File(RESULTS_FILE.replaceAll(".txt", "") + ".jpg");

        try {
            ChartUtilities.saveChartAsJPEG(pieChart, chart, 900, 900);
        } catch (IOException ioe) {
            System.out.println("[Error - makeGraph] Failed to make pie chart " + ioe);
        }
    }

}
