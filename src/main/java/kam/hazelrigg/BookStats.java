package kam.hazelrigg;

import edu.stanford.nlp.util.CoreMap;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.apache.commons.lang3.text.WordUtils.wrap;

public class BookStats {
    private static Logger logger = Willow.getLogger();
    private boolean gutenberg;

    private long syllableCount = 0;
    private long wordCount = 0;
    private long sentenceCount;

    private final FreqMap<String, Integer> words = new FreqMap<>();
    private final FreqMap<String, Integer> lemmas = new FreqMap<>();
    private final FreqMap<String, Integer> partsOfSpeech = new FreqMap<>();
    private final FreqMap<String, Integer> syllables = new FreqMap<>();
    private static final HashMap<String, String> posAbbrev = nonAbbreviate();

    private CoreMap longestSentence;

    public void increaseWords(String word) {
        wordCount++;
        word = word.replaceAll("\\W", "");
        words.increaseFreq(word);
    }

    public void increaseLemmas(String lemma) {
        lemmas.increaseFreq(lemma);
    }

    public void increasePartsOfSpeech(String tag) {
        tag = posAbbrev.get(tag);
        partsOfSpeech.increaseFreq(tag);
    }

    public void increaseSyllables(String word) {
        int wordSyllables = getSyllableCount(word);
        syllableCount += wordSyllables;
        if (wordSyllables == 1) {
            syllables.increaseFreq("Monosyllabic");
        } else {
            syllables.increaseFreq("Polysyllabic");
        }
    }

    public void increaseSentenceCount() {
        sentenceCount++;
    }

    public void setLongestSentence(CoreMap longestSentence) {
        this.longestSentence = longestSentence;
    }

    private String getKincaidGradeLevel() {
        double score = getFleschKincaidScore();
        return classifyKincaidScore(score);
    }

    double getFleschKincaidScore() {
        return 206.835 - (1.015 * wordCount / sentenceCount)
                - (84.6 * syllableCount / wordCount);
    }

    String classifyKincaidScore(double score) {
        if (score <= 100) {
            if (score > 90) {
                return "5th-grade";
            } else if (score > 80) {
                return "6th-grade";
            } else if (score > 70) {
                return "7th-grade";
            } else if (score > 60) {
                return "8th & 9th grade";
            } else if (score > 50) {
                return "10th to 12th grade";
            } else if (score > 30) {
                return "College";
            } else if (score < 30 && score > 0) {
                return "College graduate";
            }
        }

        return "easiest";
    }

    private String classifyLength() {
        /*
        Classification    Word count
        Novel             40,000 words or over
        Novella           17,500 to 39,999 words
        Novelette         7,500 to 17,499 words
        Short story       under 7,500 words
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

    public void removeStopWords() {
        words.stripStopWords();
        lemmas.stripStopWords();
    }

    long getWordCount() {
        return wordCount;
    }

    long getMonosyllablic() {
        return syllables.get("Monosyllabic");
    }

    long getPolysyllablic() {
        return syllables.get("Polysyllabic");
    }

    String getEasyDifficult() {
        if (getMonosyllablic() > getPolysyllablic()) {
            return "easy";
        }
        return "difficult";
    }

    FreqMap<String, Integer> getWords() {
        return words;
    }

    long getUniqueWords() {
        return words.size();
    }

    FreqMap<String, Integer> getLemmas() {
        return lemmas;
    }

    FreqMap<String, Integer> getPartsOfSpeech() {
        return partsOfSpeech;
    }

    FreqMap<String, Integer> getSyllables() {
        return syllables;
    }

    public CoreMap getLongestSentence() {
        return longestSentence;
    }

    String toFormattedString() {
        return "Total Words: " + wordCount
                + "\nUnique Words: " + words.size()
                + "\nPolysyllabic Words: " + getPolysyllablic()
                + "\nMonosyllabic Words: " + getMonosyllablic()
                + "\nTotal Syllables: " + syllableCount
                + "\nTotal Sentences: " + sentenceCount
                + "\nFlesch-Kincaid Grade: " + getKincaidGradeLevel()
                + "\nClassified Length: " + classifyLength()
                + "\nTop 3 words: " + words.getTopThreeValues()
                + wrap("\nLongest Sentence; " + longestSentence, 100) + "\n";
    }

    String getGradeLevel() {
        return getKincaidGradeLevel();
    }

    String getClassifiedLength() {
        return classifyLength();
    }

    private static int getSyllableCount(String s) {
        s = s.trim();
        if (s.length() <= 3) {
            return 1;
        }
        s = s.toLowerCase();
        s = s.replaceAll("[aeiouy]+", "a");
        s = "x" + s + "x";
        return s.split("a").length - 1;
    }

    private static HashMap<String, String> nonAbbreviate() {

        HashMap<String, String> posNoAbbrev = new HashMap<>();

        try {
            InputStreamReader inputStreamReader = new InputStreamReader(BookStats.class.getClass()
                    .getResourceAsStream("/posAbbreviations.txt"), StandardCharsets.UTF_8);

            BufferedReader br = new BufferedReader(inputStreamReader);

            String line = br.readLine();
            while (line != null) {
                String[] words = line.split(":");
                // Set key to abbreviation and value to non abbreviated
                posNoAbbrev.put(words[0].trim(),
                        words[1].substring(0, words[1].lastIndexOf('>')).trim());
                line = br.readLine();
            }

            br.close();
        } catch (IOException ioe) {
            logger.error("IOException reading abbreviations from disk");
        }

        return posNoAbbrev;
    }

    public boolean isGutenberg() {
        return gutenberg;
    }

    public void setGutenberg(boolean isGutenberg) {
        this.gutenberg = isGutenberg;
    }
}