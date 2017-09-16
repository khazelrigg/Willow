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

    private static String fileName;

    public static void main(String[] args) {
        //fileName = getFileName();

        fileName = "teddysInauguralAddress.txt";
        // getFileName returns null if file does not exist

        if (fileName != null) {

            if (hasResults()) {
                fileName = fileName.substring(0, fileName.length() - 4) + "Results.txt";
                readCount();
            } else {
                Map<String, Integer> pos = sortByValue(partOfSpeech());

                writeCount(wordCount(), pos);

                //fileName = fileName.substring(0, fileName.length() - 4) + "Results.txt";
                readCount();
                makeGraph(pos);
            }

        } else {
            System.out.println("[Error - Main] File not found/is not .txt");
        }

    }

    private static boolean hasResults() {
        try {
            File file = new File(fileName.substring(0, fileName.length() - 4) + "Results.txt");
            if (file.exists() && !file.isDirectory()) return true;
        } catch (NullPointerException nullpointer) {
            return false;
        }
        return false;
    }

    private static String getFileName() {
        // Get input (fileName) from user and check its validity

        Scanner kb = new Scanner(System.in);
        System.out.print("File path: ");
        String input = kb.nextLine();

        if (input.length() < 4) return null;
        if (!input.endsWith(".txt")) return null;

        File file = new File(input.substring(0, input.length() - 4) + "Results.txt");

        // File.isFile will return true if a Results file exists
        if (file.isFile()) {
            return input.substring(0, input.length() - 4) + "Results.txt";
        } else {
            return input;
        }

    }

    private static void readCount() {
        // Read Results file for counts

        try {
            Scanner in = new Scanner(new FileReader(fileName));
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

        //String outFile = fileName.substring(0, fileName.length() - 4) + "Results.txt";
        try {
            fileName = fileName.substring(0, fileName.length() - 4) + "Results.txt";
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName));

            for (String word : wordFreq.keySet()) {
                out.write(word + ", " + wordFreq.get(word) + "\n");
            }
            out.write("\n");
            // Write part of sentence tags

            for (String type : wordType.keySet()) {
                out.write(type + ", " + wordType.get(type) + "\n");
            }

            out.close();
        } catch (java.io.IOException ioExc) {
            System.out.println("[Error - writeCount] Failed to write file: " + ioExc);
        }
    }

    private static Map<String, Integer> partOfSpeech() {
        // Count the frequency of POS

        try {
            Map<String, String> posAbbrev = new HashMap<>();

            Scanner in = new Scanner(new FileReader(fileName));
            Scanner posAbbrevIn = new Scanner(new FileReader("posAbbreviations.txt"));

            // Unabbreviate results for easier understanding
            while (posAbbrevIn.hasNext()) {
                String[] line = posAbbrevIn.nextLine().split(":");
                posAbbrev.put(line[0].trim(), line[1].trim());
            }

            String trainedFile = "models/english-bidirectional-distsim.tagger";
            MaxentTagger tagger = new MaxentTagger(trainedFile);
            Map<String, Integer> partOfSentence = new HashMap<>();

            while (in.hasNextLine()) {
                String[] line = in.nextLine().split("\\s");

                for (String word : line) {
                    word = word.replaceAll("\\W", "");
                    // Catch words that are only whitespace
                    if (word.length() == 0) continue;

                    String tagged = tagger.tagString(word);
                    String tagType = tagged.substring(word.length()).replace("_", "")
                            .toLowerCase().trim();

                    // Get the non abbreviated form as tagType
                    tagType = posAbbrev.get(tagType);

                    if (tagType == null) {
                        tagType = "Unknown";
                    }

                    if (partOfSentence.containsKey(tagType))
                        partOfSentence.put(tagType, partOfSentence.get(tagType) + 1);
                    else partOfSentence.put(tagType, 1);
                }
            }

            return partOfSentence;
        } catch (IOException ioe) {
            System.out.println("[Error - partOfSpeech] File not found: " + ioe);
            return null;
        }
    }

    private static Map<String, Integer> wordCount() {
        // Count the frequency of a words appearance

        try {
            Scanner in = new Scanner(new FileReader(fileName));
            //MaxentTagger tagger = new MaxentTagger("models/english-left3words-distsim.tagger");


            Map<String, Integer> wordFreq = new HashMap<>();
            //Map<String, Integer> wordType = new HashMap<>();

            String blacklist = "this but are on that have the of to and a an in is it for ";

            while (in.hasNext()) {
                // Split line into separate words
                String[] line = in.nextLine().split("\\s");

                for (String word : line) {
                    word = word.toLowerCase() + " ";

                    //String tag = tagger.tagString(word);
                    //tag = tag.substring(word.length());

                    // If blacklisted word don't add to count
                    if (blacklist.contains(word)) continue;

                    // Remove punctuation from each word
                    word = word.replaceAll("\\W", "");

                    // Check if word has been seen before, if it has then increase its count by 1
                    if (wordFreq.containsKey(word))
                        wordFreq.put(word, wordFreq.get(word) + 1);
                    else wordFreq.put(word, 1);

                    /*
                    if (wordType.containsKey(tag))
                        wordType.put(tag, wordType.get(word) + 1);
                    else wordType.put(tag, 1);
                    */
                }
            }

            in.close();
            // Sort map wordFreq before returning to make results easier to understand
            wordFreq = sortByValue(wordFreq);

            return wordFreq;

        } catch (FileNotFoundException notFound) {
            System.out.println("[Error - wordCount] File not found: " + notFound);
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
        plot.setLabelFont(new Font("Ubuntu San Serif", Font.TRUETYPE_FONT, 10));
        plot.setDepthFactor(0.05f);

        File pieChart = new File(fileName.substring(0, fileName.length() - 4) + ".jpg");

        try {
            ChartUtilities.saveChartAsJPEG(pieChart, chart, 900, 900);
        } catch (IOException ioexc) {
            System.out.println("[Error - makeGraph] Failed to make pie chart " + ioexc);
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
