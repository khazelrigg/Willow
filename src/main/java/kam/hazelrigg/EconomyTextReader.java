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

public class EconomyTextReader implements TextReader {
    private BookStats bookStats = new BookStats();
    Path path;
    private StanfordCoreNLP pipeline = Willow.pipeline;

    @Override
    public void readText() {
        boolean gutenberg = bookStats.isGutenberg();
        try (BufferedReader br = getDecodedBufferedReader()) {
            boolean atBook = !gutenberg;
            StringBuilder buffer = new StringBuilder();
            Annotation document;

            for (String line; (line = br.readLine()) != null; ) {

                if (line.isEmpty()) {
                    continue;
                }

                // Find the header of a Gutenberg text
                if (gutenberg) {
                    atBook = checkForGutenbergStartEnd(line);
                }

                // Find the footer of a Gutenberg text and stop reading
                if (atBook) {
                    if (gutenberg) {
                        atBook = checkForGutenbergStartEnd(line);
                        if (!atBook) {
                            break;
                        }
                    }

                    // Add the current line to the buffer
                    buffer.append(" ").append(line.trim());
                    document = new Annotation(buffer.toString());
                    pipeline.annotate(document);

                    for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
                        String sentenceString = sentence.toString().trim();
                        String buffered = buffer.toString().trim();

                        // If the sentence and buffer are equal, continue adding lines
                        if (sentenceString.equals(buffered)) {
                            continue;
                        }

                        // If the buffer contains a sentence, tag the sentence and remove it
                        if (buffered.contains(sentenceString)) {

                            // Remove the sentence and following space
                            buffer.delete(buffered.indexOf(sentenceString),
                                    buffered.indexOf(sentenceString)
                                            + sentenceString.length() + 1);

                            updateStatsFromSentence(sentence);
                            break;
                        }
                    }
                }
            }

            bookStats.removeStopWords();
        } catch (IOException e) {
            System.out.println("[Error - readPlainText] Couldn't find file at " + path);
            e.printStackTrace();
            System.exit(2);
        } catch (NullPointerException e) {
            System.out.println("[Error - readPlainText] Null pointer for file at " + path);
            e.printStackTrace();
            System.exit(2);
        }
    }

    @Override
    public BookStats getStats() {
        return bookStats;
    }

    @Override
    public void setPath(Path path) {
        this.path = path;
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
