package main.java.kam.hazelrigg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class TextTools {
    static HashMap<String, String> posAbbrev = nonAbbreviate();


    /**
     * Finds if a word is monosyllabic.
     *
     * @param word word to count syllables of
     * @return true if word is monosyllabic, false otherwise
     *//*
    static int getSyllableCount(String word) {
        Matcher m = WordCount.p.matcher(word);
        int syllables = 0;
        while (m.find()) {
            syllables++;
        }
        return syllables;
    }
    */
    static int getSyllableCount(String s) {
        s = s.trim();
        if (s.length() <= 3) {
            return 1;
        }
        s = s.toLowerCase();
        s = s.replaceAll("[aeiouy]+", "a");
        s = "x" + s + "x";
        return s.split("a").length - 1;
    }
    /**
     * Uses Nebula Award classifications to classify a text based on its length
     *
     * @param wordCount Total number of words
     * @return String classification
     */
    static String classifyLength(long wordCount) {
        /*
        Classification    Word count
        Novel 	          40,000 words or over
        Novella 	      17,500 to 39,999 words
        Novelette  	      7,500 to 17,499 words
        Short story 	  under 7,500 words
        */

        if (wordCount < 7500) {
            return "short story";
        }

        if (wordCount < 17500) {
            return "novelette";
        }

        if (wordCount < 40000) {
            return "novella";
        }

        return "novel";

    }

    /**
     * Uses average reading speed of 275wpm to find the time required to read in minutes
     *
     * @param wordCount Long number of words
     * @return number of minutes
     */
    static int getReadingTime(long wordCount) {
        return (int) (wordCount / 275);
    }

    /**
     * Uses the average speaking speed of 180wpm to find the time required to read in minutes
     *
     * @param wordCount Long number of words
     * @return number of minutes
     */
    static int getSpeakingTime(long wordCount) {
        return (int) (wordCount / 180);
    }

    /**
     * Compares number of mono/polysyllabic words to determine if a text is difficult
     *
     * @param mono int number of monosyllabic words
     * @param poly int number of polysyllabic words
     * @return String easy if there are more mono than poly, hard if else
     */
    static String classifyDifficulty(int mono, int poly) {
        if (mono > poly) {
            return "easy";
        }
        return "difficult";
    }

    /**
     * Uses the Flesch-Kincaid scale to classify a text's reading ease
     *
     * @param wordCount     Total number of words
     * @param sentenceCount Total number of sentences
     * @param syllableCount Total number of syllables
     * @return String classification
     */
    static String getReadingEaseLevel(long wordCount, long sentenceCount, long syllableCount) {
        // Using Flesch–Kincaid grading scale
        double score = 206.835 - (1.015 * wordCount / sentenceCount) - (84.6 * syllableCount / wordCount);

        if (score <= 100) {
            if (score > 90) return "5th-grade";
            if (score > 80) return "6th-grade";
            if (score > 70) return "7th-grade";
            if (score > 60) return "8th & 9th grade";
            if (score > 50) return "10th to 12th grade";
            if (score > 30) return "College";
            if (score < 30 && score > 0) return "College graduate";
        }

        return "easiest";
    }

    static String getParentType(String type) {

        if (type.equals("Noun, singular or mass") || type.equals("Noun, plural")
                || type.equals("Proper noun, singular")
                || type.equals("Proper noun, plural")) {
            return "Noun";
        }

        if (type.equals("Verb, base form") || type.equals("Verb, past tense")
                || type.equals("Verb, gerund or present participle")
                || type.equals("Verb, past participle")
                || type.equals("Verb, non-3rd person singular present")
                || type.equals("Verb, 3rd person singular present")) {
            return "Verb";
        }

        if (type.equals("Adverb") || type.equals("Adverb, comparative")
                || type.equals("Adverb, superlative") || type.equals("Wh-adverb")) {
            return "Adverb";
        }

        if (type.equals("Adjective") || type.equals("Adjective, comparative")
                || type.equals("Adjective, superlative")) {
            return "Adjective";
        }

        if (type.equals("Personal pronoun") || type.equals("Possessive pronoun")
                || type.equals("Possessive wh pronoun") || type.equals("Wh-pronoun")) {
            return "Pronoun";
        }

        return "Other";

    }

    /**
     * Returns the non-abbreviated versions of abbreviations.
     *
     * @return Hash map containing the key as the abbreviation and the value as its full text
     */
    private static HashMap<String, String> nonAbbreviate() {

        HashMap<String, String> posNoAbbrev = new HashMap<>();

        try {
            InputStreamReader inputStreamReader =
                    new InputStreamReader(TextTools.class.getClass().getResourceAsStream("/posAbbreviations.txt"));

            BufferedReader br =
                    new BufferedReader(inputStreamReader);

            String line = br.readLine();
            while (line != null) {
                String[] words = line.split(":");
                // Set key to abbreviation and value to non abbreviated
                posNoAbbrev.put(words[0].trim(), words[1].substring(0, words[1].lastIndexOf(">")).trim());
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
