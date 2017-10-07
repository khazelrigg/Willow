package kam.hazelrigg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TextTools {

    /**
     * Returns whether or not a word is a stop word
     *
     * @param word Word to look at
     * @return true if the word is a stop word, false otherwise
     */
    static boolean isStopWord(String word) {
        String stopWords = "|you|us|we|which|where|were|with|was|what|her|him|had|has|have|" +
                "this|that|the|there|their|of|to|my|me|mine|if|or|and|a|an|as|are|on|i|in|is|" +
                "it|so|for|be|been|by|but|from|";

        return stopWords.contains(word + "|");
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
    static HashMap<String, String> nonAbbreviate(File abbreviations) {

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
    boolean isPalindrome(String str) {
        for (int i = 0; i < str.length() / 2; i++) {
            if (str.charAt(i) != str.charAt(str.length() - 1 - i)) {
                return false;
            }
        }

        return true;
    }


}
