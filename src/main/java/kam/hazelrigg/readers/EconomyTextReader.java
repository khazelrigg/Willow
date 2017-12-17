package kam.hazelrigg.readers;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

public class EconomyTextReader extends TextReader {
    private boolean gutenberg;
    private StringBuilder buffer = new StringBuilder();
    private String scratchName;
    private RandomAccessFile randomAccessFile;

    public void readText() {
        scratchName = "randomAccessFile" + path.getFileName().toString() + ".txt";
        try (BufferedReader br = getDecodedBufferedReader(); RandomAccessFile scratch = new RandomAccessFile(scratchName, "rw")) {
            this.randomAccessFile = scratch;

            gutenberg = bookStats.isGutenberg();
            boolean atBook = !gutenberg;
            Annotation document;

            for (String line; (line = br.readLine()) != null; ) {

                if (foundGutenbergHeader(line)) {
                    atBook = true;
                }

                if (atBook && !foundGutenbergFooter(line)) {

                    // Add the current line to the buffer
                    buffer.append(" ").append(line.trim());
                    buffer.trimToSize();

                    document = new Annotation(buffer.toString());
                    pipeline.annotate(document);

                    for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
                        String sentenceString = sentence.toString().trim();
                        String buffered = buffer.toString().trim();

                        // If the sentence and buffer are equal, continue adding lines
                        if (sentenceString.equals(buffered)) {
                            continue;
                        }

                        updateBuffer(sentence, buffered, sentenceString);
                    }
                }

            }

            populateStats();
            bookStats.removeStopWords();
        } catch (IOException e) {
            logger.error("Unable to find file located at {}", path);
        } catch (NullPointerException e) {
            logger.error("Null pointer for file located at {}\n{}", path, e.toString());
        }
    }

    private void populateStats() throws IOException {
        randomAccessFile.seek(0);
        String line = randomAccessFile.readLine();

        while (line != null) {
            char type = line.charAt(0);
            String value = line.substring(2);

            if (type == 'w') {
                bookStats.increaseWords(value);
                bookStats.increaseSyllables(value);
            } else if (type == 'p') {
                bookStats.increasePartsOfSpeech(value);
            } else if (type == 'l') {
                bookStats.increaseLemmas(value);
            }

            line = randomAccessFile.readLine();
        }
        randomAccessFile.close();
        File file = new File(scratchName);
        Files.delete(file.toPath());
    }

    private void writeToScratch(String key, String value) throws IOException {
        randomAccessFile.write((key + "|" + value + "\n").getBytes());
    }

    @Override
    void updateStatsFromSentence(CoreMap sentence) {
        bookStats.increaseSentenceCount();
        CoreMap longestSentence = bookStats.getLongestSentence();

        if (longestSentence == null || sentence.size() > longestSentence.size()) {
            bookStats.setLongestSentence(sentence);
        }

        for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
            try {
                updateScratch(token);
            } catch (IOException e) {
                logger.error("Unable to open scratch file {}", scratchName);
            }
        }
    }

    private void updateScratch(CoreLabel token) throws IOException {
        String word = token.word().toLowerCase();
        String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
        String tag = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);

        if (!word.replaceAll("\\W", "").isEmpty()) {
            writeToScratch("w", word);
            writeToScratch("p", tag);
            writeToScratch("l", lemma);
        }
    }


    private boolean foundGutenbergHeader(String line) {
        return gutenberg && isGutenbergStart(line);
    }

    private boolean foundGutenbergFooter(String line) {
        return gutenberg && isGutenbergEnd(line);
    }

    private void updateBuffer(CoreMap sentence, String buffered, String currentSentence) {
        // If the buffer contains a sentence, tag the sentence and remove it
        if (buffered.contains(currentSentence)) {

            // Remove the sentence and following space
            buffer.delete(buffered.indexOf(currentSentence),
                    buffered.indexOf(currentSentence)
                            + currentSentence.length() + 1);

            updateStatsFromSentence(sentence);
        }
    }

}
