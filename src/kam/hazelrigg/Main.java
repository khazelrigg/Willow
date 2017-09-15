package kam.hazelrigg;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private static String fileName;
    public static void main(String[] args) {
        fileName = getFileName();
        // getFileName returns null if file does not exist

        if (fileName != null) {
            if (fileName.endsWith("Results.txt")) {
                readCount();
            } else {
                writeCount(wordCount());
                fileName = fileName.substring(0, fileName.length() - 4) + "Results.txt";
                readCount();
            }
        } else {
            System.out.println("[Error] File not found/is not .txt");
        }
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
            System.out.println("[Error] File not found: " + notFound);
        }
    }


    private static void writeCount(Map<String, Integer> wordFreq) {
        // Write the word counts to a file

        String outFile = fileName.substring(0, fileName.length() - 4) + "Results.txt";
        try {
            FileWriter fstream = new FileWriter(outFile);
            BufferedWriter out = new BufferedWriter(fstream);

            for (String word : wordFreq.keySet()) {
                out.write(word + ", " + wordFreq.get(word) + "\n");
            }
            out.close();
        } catch (java.io.IOException ioExc) {
            System.out.println("[Error] Failed to write file: " + ioExc);
        }
    }

    private static Map<String, Integer> wordCount() {
        // Count the frequency of a words appearance

        try {
            Scanner in = new Scanner(new FileReader(fileName));
            Map<String, Integer> wordFreq = new HashMap<>();

            String blacklist = "this but are on that have the of to and a an in is it for ";

            while (in.hasNext()) {
                // Split line into separate words
                String[] line = in.nextLine().split("\\s");

                for (String word : line) {
                    word = word.toLowerCase() + " ";

                    // If blacklisted word don't add to count
                    if (blacklist.contains(word)) continue;

                    // Remove punctuation from each word
                    word = word.replaceAll("\\W", "");

                    // Check if word has been seen before, if it has then increase its count by 1
                    if (wordFreq.containsKey(word)) wordFreq.put(word, wordFreq.get(word) + 1);
                    else wordFreq.put(word, 1);
                }
            }

            in.close();
            // Sort map wordFreq before returning to make results easier to understand
            wordFreq = sortByValue(wordFreq);
            return wordFreq;

        } catch (FileNotFoundException notFound) {
            System.out.println("[Error] File not found: " + notFound);
            return null;
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
