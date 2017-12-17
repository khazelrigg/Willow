package kam.hazelrigg.readers;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import kam.hazelrigg.BookStats;
import kam.hazelrigg.Willow;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class TextReader {
    Path path;
    BookStats bookStats;
    final StanfordCoreNLP pipeline = Willow.getPipeline();
    Logger logger = Willow.getLogger();

    public abstract void readText();

    void setPath(Path path) {
        this.path = path;
    }

    void setBookStats(BookStats stats) {
        this.bookStats = stats;
    }

    void tagText(String text) {
        Annotation doc = new Annotation(text);
        pipeline.annotate(doc);

        for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
            updateStatsFromSentence(sentence);
        }

        bookStats.removeStopWords();
    }

    void updateStatsFromSentence(CoreMap sentence) {
        bookStats.increaseSentenceCount();
        CoreMap longestSentence = bookStats.getLongestSentence();

        if (longestSentence == null || sentence.size() > longestSentence.size()) {
            bookStats.setLongestSentence(sentence);
        }

        for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
            updateStatsFromToken(token);
        }
    }

    boolean isGutenbergEnd(String line) {
        line = line.toLowerCase();
        return bookStats.isGutenberg() && (line.contains("end of the project gutenberg"));
    }

    boolean isGutenbergStart(String line) {
        return bookStats.isGutenberg() && (line.contains("START OF THIS PROJECT GUTENBERG EBOOK")
                || line.contains("START OF THE PROJECT GUTENBERG EBOOK"));
    }

    BufferedReader getDecodedBufferedReader() throws IOException {
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);

        InputStream inputStream = Files.newInputStream(path);
        InputStreamReader reader = new InputStreamReader(inputStream, decoder);

        return new BufferedReader(reader);
    }

    private void updateStatsFromToken(CoreLabel token) {
        String word = token.word().toLowerCase();
        String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
        String tag = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);

        bookStats.increaseSyllables(word);

        if (!isPunctuation(word)) {
            bookStats.increaseWords(word);
            bookStats.increasePartsOfSpeech(tag);
            bookStats.increaseLemmas(lemma);
        }
    }

    private boolean isPunctuation(String word) {
        return stripPunctuation(word).isEmpty();
    }

    private String stripPunctuation(String word) {
        return word.replaceAll("\\W", "");
    }

}
