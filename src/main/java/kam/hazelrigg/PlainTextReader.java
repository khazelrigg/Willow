package kam.hazelrigg;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlainTextReader implements TextReader {
    private BookStats bookStats = new BookStats();
    private Path path;
    private StanfordCoreNLP pipeline = Willow.pipeline;

    @Override
    public void readText() {
        boolean gutenberg = bookStats.isGutenberg();

        try (BufferedReader br = getDecodedBufferedReader()) {
            boolean atBook = !gutenberg;
            StringBuilder text = new StringBuilder();

            for (String line; (line = br.readLine()) != null; ) {
                if (line.isEmpty()) {
                    continue;
                }

                if (gutenberg) {
                    atBook = checkForGutenbergStartEnd(line);
                }

                if (atBook) {
                    if (gutenberg) {
                        atBook = checkForGutenbergStartEnd(line);
                    }
                    text.append(line).append(" ");
                }
            }

            tagText(text.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPath(Path path) {
        this.path = path;
    }

    @Override

    public BookStats getStats() {
        return bookStats;
    }

    private void tagText(String text) {
        Annotation doc = new Annotation(text);
        pipeline.annotate(doc);

        for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
            updateStatsFromSentence(sentence);
        }

        bookStats.removeStopWords();
    }

    private void updateStatsFromSentence(CoreMap sentence) {
        bookStats.increaseSentenceCount();
        CoreMap longestSentence = bookStats.getLongestSentence();
        if (longestSentence == null || sentence.size() > longestSentence.size()) {
            bookStats.setLongestSentence(sentence);
        }

        for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
            updateStatsFromToken(token);
        }
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

    private boolean checkForGutenbergStartEnd(String line) {
        if (line.contains("End of the Project Gutenberg")
                || line.contains("End of Project Gutenberg’s")) {
            return false;
        } else if (line.contains("START OF THIS PROJECT GUTENBERG EBOOK")
                || line.contains("START OF THE PROJECT GUTENBERG EBOOK")) {
            return true;
        }

        return false;
    }

    private BufferedReader getDecodedBufferedReader() throws IOException {
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);

        InputStream inputStream = Files.newInputStream(path);
        InputStreamReader reader = new InputStreamReader(inputStream, decoder);

        return new BufferedReader(reader);
    }

}
