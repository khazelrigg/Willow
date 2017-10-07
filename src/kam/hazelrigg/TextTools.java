package kam.hazelrigg;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TextTools {

    // Set up tagger
    private static final MaxentTagger tagger =
            new MaxentTagger("models/english-bidirectional-distsim.tagger");

    /**
     * Returns the part of speech of a word
     *
     * @param line The word to tag
     * @return Tag of word
     */
    static String[] getTag(String line) {
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
     * Returns whether or not a string is a palindrome
     *
     * @param str String to analyse
     * @return True if the string is a palindrome
     */
    public static boolean isPalindrome(String str) {
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
    static int getSyllableCount(String word) {
        Pattern p = Pattern.compile("[aeiouy]+[^$e(,.:;!?)]");
        Matcher m = p.matcher(word);

        int syllables = 0;
        while (m.find()) {
            syllables++;
        }

        return syllables;
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


}
