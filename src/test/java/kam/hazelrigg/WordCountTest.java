package test.java.kam.hazelrigg;


import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import main.java.kam.hazelrigg.Book;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class WordCountTest {


    private Properties props = new Properties(
            PropertiesUtils.asProperties("annotators", "tokenize, ssplit, pos, lemma"));
    private StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

    @Test
    public void getsCorrectWordCounts() {
        String testString = "I eNjoy writing a lot of unit tests. Unit tests are a loT of fUn to make.";
        String expected = "a:2|.:2|lot:2|unit:2|tests:2|of:2|i:1|enjoy:1|are:1|writing:1|to:1|make:1|fun:1|";

        Book test = new Book();
        test.givePipeline(pipeline);
        test.setText(testString);

        assertEquals("Failure creating word counts", test.wordFreq.getSimpleString(), expected);
    }

    @Test
    public void getsCorrectPOSTags() {
        String testString = "So they were trying to re-invent themselves and their universe."
                + " Science fiction was a big help.";
        String expected = "Noun, singular or mass:3|Verb, past tense:2|Personal pronoun:2|Proper noun, singular:1|Preposition or subordinating conjunction:1|Adjective:1|Coordinating conjunction:1|Determiner:1|Possessive pronoun:1|Verb, gerund or present participle:1|Verb, base form:1|to:1|";

        Book testBook = new Book();
        testBook.givePipeline(pipeline);
        testBook.setText(testString);
        assertEquals(testBook.posFreq.getSimpleString(), expected);
    }

    @Test
    public void getsTitleFromText() {
        File testf = null;
        try {
            testf = new File(this.getClass().getResource("/test.txt").toURI());
            System.out.println(testf.exists());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        Book test = new Book();
        test.setTitleFromText(testf);

        assertEquals("2 B R 0 2 B by Kurt Vonnegut", test.getName());
    }

}
