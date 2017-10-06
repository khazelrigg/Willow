package kam.hazelrigg;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import org.jfree.chart.ChartFactory;
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
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    // Set up tagger
    private static final MaxentTagger tagger =
            new MaxentTagger("models/english-bidirectional-distsim.tagger");
    private static Boolean verbose = false;

    private static Book current = new Book();

    public static void main(String[] args) {
        if (args.length != 0 && args[0].equals("-v")) {
            verbose = true;
        }

        File file = new File(getFileName());
        long startTime = System.currentTimeMillis();

        if (file.isDirectory()) {
            readDir(file);
        } else if (file.isFile()) {
            readFile(file);
        }

        long endTime = System.currentTimeMillis();

        System.out.println("\nTotal time elapsed " + ((endTime - startTime) / 1000) + " sec.");

    }

    /**
     * Get a file/directory name from the user and ensure it is valid
     *
     * @return String containing the input if the input is a file/directory
     */
    private static String getFileName() {
        // Get a filename and check that the file exists

        Scanner kb = new Scanner(System.in);

        // Keep asking for input path until a valid one is found.
        while (true) {
            System.out.print("File path: ");
            String input = kb.nextLine();
            File file = new File(input);

            // If the file exists it is a valid input
            if (file.exists()) {
                return input;
            } else {
                System.out.println("Try again, no file found at " + input);
            }
        }
    }

    /**
     * Reads and creates the data for an entire directory
     *
     * @param dir Directory to analyse
     */
    private static void readDir(File dir) {
        // Create an array of files containing every file in the directory
        File[] files = dir.listFiles();

        // getFileName ensures files is not null
        assert files != null;

        for (File file : files) {
            // Call readDir recursively to read sub directories
            if (file.isDirectory()) {
                readDir(file);
            } else {
                readFile(file);
            }
        }
    }

    /**
     * Reads and creates the data for an input file
     *
     * @param file File to analyse
     */
    private static void readFile(File file) {
        current = new Book();
        File resultFile = new File(outputOfFile(file));

        // If we already have results there is no need to redo results
        if (resultFile.exists()) {
            System.out.println("☑ - " + file.getName() + " already has results.");

            if (verbose) {
                printFile(resultFile);
            }

        } else {
            try {
                // Get wordCount information from the input file
                Map<String, Map<String, Integer>> count = wordCount(file);

                // Write information into results file
                writeCount(count, resultFile);

                if (verbose) {
                    printFile(resultFile);
                }

                // Create a parts of speech graph
                makeGraph(count.get("POS"),
                        new File("results/img/" + resultFile.getName().
                                replaceAll(".txt", ".jpeg")), "POS distribution");

                // Create a difficulty graph
                Map<String, Integer> monoVsPoly = new HashMap<>();
                monoVsPoly.put("Monosyllabic", count.get("OTHER").get("Monosyllabic"));
                monoVsPoly.put("Polysyllabic", count.get("OTHER").get("Polysyllabic"));


                makeGraph(monoVsPoly, new File("results/img/" +
                                resultFile.getName().replaceAll(".txt", "") + " Difficulty.jpeg"),
                        "Difficulty");


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Converts an input file into its output file
     *
     * @param file The file to convert to an output
     * @return The conversion of input file
     */
    private static String outputOfFile(File file) {
        setBookTitle(file);

        String out = current.getTitle() + " by " + current.getAuthor();

        String fileName;

        if (makeResultDirs()) {
            // If we have a results directory, which we should, put the output in there
            fileName = "results/txt/" + out + " Results.txt";
        } else {
            fileName = out + "Results.txt";
        }

        return fileName;
    }

    /**
     * Creates results directories
     *
     * @return True if directories already exist or were created, false if they were not
     */
    private static boolean makeResultDirs() {
        // Create two files containing output directory path
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
     * Sets the title and author of a book if one is found on the first line
     *
     * @param file File you want to get title of
     */
    private static void setBookTitle(File file) {
        String title = "";
        String author = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String firstLine = br.readLine();

            // If the first line is very short skip over it
            while (firstLine.length() < 3) {
                firstLine = br.readLine();
            }

            // Cases of Gutenberg books to check
            if (firstLine.contains("The Project Gutenberg EBook of")) {
                firstLine = firstLine.substring(31);
                current.setGutenberg(true);
            }

            if (firstLine.contains("Project Gutenberg's") ||
                    firstLine.contains("Project Gutenberg’s")) {
                firstLine = firstLine.substring(20);
                current.setGutenberg(true);
            }

            // If the pattern "title by author" appears split at the word 'by' to get author and title
            if (firstLine.contains("by")) {
                title = firstLine.substring(0, firstLine.lastIndexOf("by")).trim();
                author = firstLine.substring(firstLine.lastIndexOf("by") + 2).trim();
            } else {
                title = file.getName();
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
        current.setAuthor(author);
        current.setTitle(title);
    }

    /**
     * Counts parts of speech and general word counts of a file
     *
     * @param file The input file to be analyzed
     * @return A map containing the type of information and then its values as a second map
     * @throws IOException if the file is unable to be opened
     */
    private static Map<String, Map<String, Integer>> wordCount(File file) throws IOException {

        System.out.println("☐ - Analysing:\t" + file.getName());


        Map<String, Map<String, Integer>> results = new HashMap<>();

        // Maps to store information in
        FreqMap posFreq = new FreqMap();
        FreqMap wordFreq = new FreqMap();
        FreqMap otherMap = new FreqMap();

        Boolean gutenberg = current.isGutenberg();


        boolean atBook = false;

        // If book is not a Gutenberg text don't worry about a header
        if (!gutenberg) {
            System.out.println("✘ - " + file.getName() + " is not a Gutenberg Book.");
            atBook = true;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line = br.readLine();

            while (line != null) {

                // Split line into separate words
                if (line.length() == 0) {
                    line = br.readLine();
                    continue;
                }

                // Wait to start reading text until we pass Gutenberg header
                if (gutenberg && !atBook) {
                    if (line.contains("START OF THIS PROJECT GUTENBERG EBOOK") ||
                            line.contains("START OF THE PROJECT GUTENBERG EBOOK")) {
                        atBook = true;
                        line = br.readLine();
                    } else {
                        line = br.readLine();
                        continue;
                    }
                }


                // Stop reading at the end of text if it is a Gutenberg text
                if (gutenberg && line.contains("End of the Project Gutenberg EBook") ||
                        line.contains("End of Project Gutenberg’s")) {
                    break;
                }

                // Tag each line in the file
                for (String tag : getTag(line)) {
                    posFreq.increaseFreq(tag);
                }

                // Add POS Tags
                results.put("POS", posFreq.getFrequency());

                /* Begin the word count section by first
                   Splitting the line into words
                */

                String[] words = line.split("\\s");

                for (String word : words) {
                    // Word prep, make lowercase and remove punctuation
                    word = word.toLowerCase().replaceAll("\\W", "");

                    // If the word is a stop word skip over it
                    if (word.length() == 0 || FreqMap.stopWords.contains(word + "|")) {
                        line = br.readLine();
                        continue;
                    }

                    // Increase word count and then put into results
                    wordFreq.increaseFreq(word);
                    results.put("WORD", wordFreq.getFrequency());

                    if (isPalindrome(word)) {
                        otherMap.increaseFreq("Palindrome");
                    }

                    // Get syllable lengths and add to the map
                    if (isMonosyllabic(word)) {
                        otherMap.increaseFreq("Monosyllabic");
                    } else {
                        otherMap.increaseFreq("Polysyllabic");
                    }

                    // Put Other map into results map
                    otherMap.increaseFreq("Total words");
                    results.put("OTHER", otherMap.getFrequency());

                    line = br.readLine();
                }
            }

            br.close();

            System.out.println("☑ - Finished:\t" + file.getName() + "\n");
            return results;

        }
    }

    /**
     * Returns the part of speech of a word
     *
     * @param line The word to tag
     * @return Tag of word
     */
    private static String[] getTag(String line) {
        Map<String, String> posAbbrev = nonAbbreviate(new File("posAbbreviations.txt"));
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
     * Returns the non-abbreviated versions of abbreviations
     *
     * @param abbreviations ":" Separated file containing abbreviations and full text
     * @return Hash map containing the key as the abbreviation and the value as its full text
     */
    private static HashMap<String, String> nonAbbreviate(File abbreviations) {

        HashMap<String, String> posNoAbbrev = new HashMap<>();
        try {
            BufferedReader br =
                    new BufferedReader(new FileReader(abbreviations));

            String line = br.readLine();
            while (line != null) {
                String[] words = line.split(":");
                // Set key to abbreviation and value to non abbreviated
                posNoAbbrev.put(words[0].trim(), words[1].trim());
                line = br.readLine();
            }

            br.close();
            return posNoAbbrev;
        } catch (IOException ioe) {
            System.out.println("[Error - nonAbbreviate] " + ioe);
        }

        return posNoAbbrev;
    }

    /**
     * Returns whether or not a string is a palindrome
     *
     * @param str String to analyse
     * @return True if the string is a palindrome
     */
    private static boolean isPalindrome(String str) {
        for (int i = 0; i < str.length() / 2; i++) {
            if (str.charAt(i) != str.charAt(str.length() - 1 - i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Finds if a word is monosyllabic
     *
     * @param word word to count syllables of
     * @return true if word is monosyllabic, false otherwise
     */
    private static boolean isMonosyllabic(String word) {
        Pattern p = Pattern.compile("[aeiouy]+[^$e(,.:;!?)]");
        Matcher m = p.matcher(word);

        int syllables = 0;
        while (m.find()) {
            syllables++;
        }

        return syllables == 1;
    }

    /**
     * Writes the counts of a Map to a file out
     *
     * @param counts The map to use as input
     * @param out    File to write
     */
    private static void writeCount(Map<String, Map<String, Integer>> counts, File out) {

        if (verbose) {
            System.out.println("[*] Writing counts to " + out.getName());
        }

        if (counts.keySet().size() == 0) {
            System.out.println("[Error - writeCount] Counts are empty for file: " + out.getName());
            System.exit(9);
        }

        try {
            BufferedWriter br =
                    new BufferedWriter(new FileWriter(new File("results/txt/" + out.getName())));

            br.write("====================[ Conclusions ]====================\n");
            br.write(getConclusionString(counts));
            br.write("\n");

            // Write counts information
            for (String id : counts.keySet()) {
                br.write("====================[ " + id + " ]====================\n");
                for (String key : counts.get(id).keySet()) {
                    br.write(key + ", " + counts.get(id).get(key) + "\n");
                }
                br.write("\n");
            }

            br.close();
        } catch (java.io.IOException ioExc) {
            System.out.println("[Error - writeCount] Failed to write file: " + ioExc);
        }
    }

    /**
     * Creates a short conclusion based on text information
     *
     * @param counts Map with word frequencies
     * @return String containing conclusion
     */
    private static String getConclusionString(Map<String, Map<String, Integer>> counts) {
        String conclusion = "This " + getDifficultyString(counts.get("OTHER"));
        conclusion += "The most frequently used word out of the " +
                counts.get("WORD").entrySet().size() +
                " unique words seen in the text was \"" +
                counts.get("WORD").entrySet().iterator().next().getKey() + "\".";

        return conclusion + "\n";
    }

    /**
     * Creates a string for writing in conclusion section of results file
     *
     * @param counts Map containing Mono vs Polysyllabic counts
     * @return String that is formatted for being written directly to file
     */
    private static String getDifficultyString(Map<String, Integer> counts) {
        if (counts.get("Polysyllabic") >= counts.get("Monosyllabic")) {
            return "text is a difficult read, it has " + counts.get("Polysyllabic") +
                    " polysyllabic words and " + counts.get("Monosyllabic") +
                    " monosyllabic words. ";
        }
        return "text is a simple read, it has " + counts.get("Monosyllabic") +
                " monosyllabic words and " + counts.get("Polysyllabic") +
                " polysyllabic words. ";
    }

    /**
     * Prints the contents of a file to the terminal
     *
     * @param file The file to print contents of
     */
    private static void printFile(File file) {

        File resultsFile = new File
                ("results/txt/" + file.getName());

        try {
            BufferedReader br = new BufferedReader(new FileReader(resultsFile));
            System.out.println("\n----[ Using results from " + file.getName() + " ]----\n");

            String line = br.readLine();
            while (line != null) {
                System.out.println(line);
                line = br.readLine();
            }

            br.close();
        } catch (IOException ioe) {
            System.out.println("[Error - printFile] File not found: " + ioe);
        }
    }

    /**
     * Create an image representation of parts of speech tag distribution in a Map
     *
     * @param posMap Map to use as input data
     * @param out    File to save image to
     */
    private static void makeGraph(Map<String, Integer> posMap, File out, String purpose) {

        if (verbose) {
            System.out.println("[*] Creating graph for " + out.getName());
        }

        DefaultPieDataset dataSet = new DefaultPieDataset();

        // Load POS data into data set
        for (String type : posMap.keySet()) {
            dataSet.setValue(type, posMap.get(type));
        }

        String title = purpose + " of " +
                out.getName().substring(0, out.getName().lastIndexOf("Results"));

        JFreeChart chart = ChartFactory.createPieChart3D(
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
            ChartUtilities.saveChartAsJPEG(new File("results/img/" + out.getName()), chart, 900, 900);
        } catch (IOException ioe) {
            System.out.println("[Error - makeGraph] Failed to make pie chart " + ioe);
        }
    }

}
