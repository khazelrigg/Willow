package kam.hazelrigg;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PdfTextReader implements TextReader {
    private BookStats bookStats = new BookStats();
    private StanfordCoreNLP pipeline = Willow.pipeline;
    Path path;

    @Override
    public void readText() {
        try {
            PDDocument pdDocument = PDDocument.load(Files.newInputStream(path));
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(pdDocument);
            pdDocument.close();


            tagText(text);
        } catch (IOException | NullPointerException e) {
            System.out.println("[Error - readPdf] IOException when opening PDFParser for "
                    + path.getFileName());
            e.printStackTrace();
        }
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

    @Override
    public BookStats getStats() {
        return null;
    }

    @Override
    public void setPath(Path path) {
        this.path = path;
    }
}
