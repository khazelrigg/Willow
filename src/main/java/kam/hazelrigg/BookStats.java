package kam.hazelrigg;

import edu.stanford.nlp.util.CoreMap;

import static org.apache.commons.lang3.text.WordUtils.wrap;

class BookStats {
    private long syllableCount;
    private long wordCount;
    private long sentenceCount;

    private FreqMap<String, Integer> words;
    private FreqMap<String, Integer> lemmas;
    private FreqMap<String, Integer> partsOfSpeech;
    private FreqMap<String, Integer> syllables;

    private CoreMap longestSentence;
    private String classifiedLength;
    private String gradeLevel;

    BookStats() {
        this.syllableCount = 0;
        this.wordCount = 0;
        this.words = new FreqMap<>();
        this.lemmas = new FreqMap<>();
        this.partsOfSpeech = new FreqMap<>();
        this.syllables = new FreqMap<>();
        this.classifiedLength = TextTools.classifyLength(wordCount);
        this.gradeLevel = TextTools.getReadingEaseLevel(this);
    }

    void increaseWords(String word) {
        wordCount++;
        words.increaseFreq(word);
    }

    void increaseLemmas(String lemma) {
        lemmas.increaseFreq(lemma);
    }

    void increasePartsOfSpeech(String tag) {
        partsOfSpeech.increaseFreq(tag);
    }

    void increaseSyllables(String word) {
        int wordSyllables = TextTools.getSyllableCount(word);
        syllableCount += wordSyllables;
        if (wordSyllables == 1) {
            syllables.increaseFreq("Monosyllabic");
        } else {
            syllables.increaseFreq("Polysyllabic");
        }
    }

    String getGradeLevel() {
        return gradeLevel;
    }

    void increaseSentenceCount() {
        sentenceCount++;
    }

    void removeStopWords() {
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

    FreqMap<String, Integer> getWords() {
        return words;
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

    long getSyllableCount() {
        return syllableCount;
    }

    CoreMap getLongestSentence() {
        return longestSentence;
    }

    void setLongestSentence(CoreMap longestSentence) {
        this.longestSentence = longestSentence;
    }

    String toFormattedString() {
        return "Total Words: " + wordCount
                + "\nUnique Words: " + words.size()
                + "\nPolysyllabic Words: " + getPolysyllablic()
                + "\nMonosyllabic Words: " + getMonosyllablic()
                + "\nTotal Syllables: " + syllableCount
                + "\nTotal Sentences: " + sentenceCount
                + "\nFlesch-Kincaid Grade: " + gradeLevel
                + "\nClassified Length: " + classifiedLength
                + "\nTop 3 words: " + words.getTopThree()
                + wrap("\nLongest Sentence; " + longestSentence, 100) + "\n";
    }

    String getClassifiedLength() {
        return classifiedLength;
    }

    long getSentenceCount() {
        return sentenceCount;
    }
}