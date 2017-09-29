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

    final private static String FILE_NAME = getFileName();
    final private static String RESULTS_FILE = "results/" + FILE_NAME.substring(0, FILE_NAME.length() - 4) + "Results.txt";

    // Set up tagger
    private static MaxentTagger tagger = new MaxentTagger("models/english-bidirectional-distsim.tagger");

    public static void main(String[] args) {

        //long startTime = System.currentTimeMillis();

        if (hasResults()) {
            System.out.println("[NOTE] " + FILE_NAME + " already has results");
            readCount();
        } else {
            Map<String, Map<String, Integer>> counts = wordCount();

            writeCount(counts);
            readCount();
            makeGraph(counts.get("POS"));
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

            if (file.exists() && !file.isDirectory()) return input;
            else System.out.println("Try again, no file found at " + input);
        }
    }

    private static boolean hasResults() {
        // Find out if a file already has a results file

        try {
            File file = new File(RESULTS_FILE);
            if (file.exists() && !file.isDirectory()) return true;
        } catch (NullPointerException nullptr) {
            return false;
        }
        return false;
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

    private static void writeCount(Map<String, Map<String, Integer>> counts) {
        // Write the word counts to a file

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(RESULTS_FILE));
            System.out.println("[WRITE] " + RESULTS_FILE);

            // Write word frequency
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

    private static Map<String, Map<String, Integer>> wordCount() {
        // Count the frequency of a words appearance

        Map<String, Map<String, Integer>> results = new HashMap<>();
       // Map<String, Integer> z = new HashMap<>();

        results.put("OTHER", null);
        results.put("POS", null);
        results.put("WORD", null);

        /* THIS IS UGLY, FIND OUT HOW TO MAKE THIS BEAUTIFUL */
        Map<String, Integer> POS = new HashMap<>();
        Map<String, Integer> WORD = new HashMap<>();
        Map<String, Integer> OTHER = new HashMap<>();

        OTHER.put("Palindrome", 0);

        try {
            Scanner in = new Scanner(new FileReader(FILE_NAME));

            String stopWords = "this but are on that have the of to and a an in is it for ";

            while (in.hasNext()) {
                // Split line into separate words
                String[] line = in.nextLine().split("\\s");

                for (String word : line) {
                    word = word.toLowerCase();
                    if (stopWords.contains(word + " ")) continue; // If the word is a stop word skip over it

                    // Remove punctuation from each word
                    word = word.replaceAll("\\W", "");
                    if (word.length() == 0) continue;

                    // Tag each word
                    String tagType = getTag(word);

                    // Add counts and tags to corresponding maps
                    WORD = addFreq(WORD, word);
                    POS = addFreq(POS, tagType);

                    results.put("WORD", WORD);
                    results.put("POS", POS);

                    if (isPalindrome(word)) {
                        OTHER.put("Palindrome", OTHER.get("Palindrome") + 1);
                    }

                    results.put("OTHER", OTHER);
                }
            }

            in.close();

            // Sort each Map
            for (String id : results.keySet()) {
                results.put(id, sortByValue(results.get(id)));
            }

            return results;

        } catch (FileNotFoundException notFound) {
            System.out.println("[Error - wordCount] File not found: " + notFound);
            return results;
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
        HashMap <String, String> posNoAbbrev = new HashMap<>();
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
            if (str.charAt(i) != str.charAt(str.length() - 1 - i)) return false;
        }
        return true;
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

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        /* Found on stack overflow:
            https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java#2581754
        */

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
