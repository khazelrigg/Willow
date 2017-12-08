package kam.hazelrigg;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

public class Book {
    private final FreqMap<String, Integer> partsOfSpeech = new FreqMap<>();
    private final FreqMap<String, Integer> words = new FreqMap<>();
    private final FreqMap<String, Integer> lemmas = new FreqMap<>();
    private final FreqMap<String, Integer> syllables = new FreqMap<>();

    String title = "";
    String author = "";

    private String subdirectory;
    private long wordCount;

    public void setSubdirectory(String subdirectory) {
        this.subdirectory = subdirectory;
    }

    private long syllableCount;
    private long sentenceCount;

    private boolean gutenberg = false;
    private File path;
    private CoreMap longestSentence;
    private StanfordCoreNLP pipeline = Willow.pipeline;

    public Book() {
        this.subdirectory = "";
    }

    public Book(String subdirectory) {
        this.subdirectory = subdirectory;
    }

    /**
     * Get the title of a book by scanning first couple of lines for a title and author
     *
     * @param text File to find title of
     */
    public void setTitleFromText(File text) {
        String title = null;
        String author = null;

        try {
            BufferedReader br = new BufferedReader(new FileReader(text));
            String line = br.readLine();

            // If the first line is empty skip over it until finding a full line
            while (line.isEmpty()) {
                line = br.readLine();
            }

            if (isGutenbergText(line)) {
                this.gutenberg = true;
                while (title == null || author == null) {
                    line = br.readLine();
                    if (line.contains("Title:")) {
                        title = line.substring(6).trim();
                    } else if (line.contains("Author:")) {
                        author = line.substring(7).trim();
                    }
                }
            } else {
                if (line.toLowerCase().contains("title:")) {
                    title = line.substring(6).trim();
                    line = br.readLine().toLowerCase();
                    if (line.contains("author")) {
                        author = line.substring(7).trim();
                    }
                } else {
                    title = text.getName();
                    author = "";
                }
            }

            this.title = title;
            this.author = author;

        } catch (IOException e) {
            System.out.println("[Error - SetTitle] Error opening " + text.getName() + " for setting title");
            e.printStackTrace();
        }
    }

    private boolean isGutenbergText(String line) {
        return line.toLowerCase().contains("gutenberg");
    }

    /**
     * Finds the appropriate file type of book to then reads the text
     */
    public boolean readText(Boolean economy) {
        System.out.println("☐ - Starting analysis of " + getName());
        try {
            String fileType = Files.probeContentType(path.toPath());
            return runFileType(fileType, economy);

        } catch (IOException e) {
            System.out.println("[Error - readText] IOException when probing file type");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("[Error - readText] NullPointerException when probing file type");
            e.printStackTrace();
        }
        return false;
    }

    private boolean runFileType(String fileType, boolean economy) {
        switch (fileType) {
            case "text/plain":
                if (economy) {
                    return readPlainTextEconomy();
                } else {
                    return readPlainText();
                }
            case "application/pdf":
                return readPDF();
            default:
                System.out.println("Unsupported format " + fileType);
                System.exit(-3);
        }
        return false;
    }

    /**
     * Reads and tags a plain text file sentence by sentence
     *
     * @return true if successfully finished
     */
    private boolean readPlainTextEconomy() {

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
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

            removeStopWords();
            return true;
        } catch (IOException e) {
            System.out.println("[Error - readPlainText] Couldn't find file at " + path);
            e.printStackTrace();
            System.exit(2);
        } catch (NullPointerException e) {
            System.out.println("[Error - readPlainText] Null pointer for file at " + path);
            e.printStackTrace();
            System.exit(2);
        }
        return false;
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

    /**
     * Reads and tags a plain text file by loading into memory
     *
     * @return true if successfully finished
     */
    private boolean readPlainText() {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            boolean atBook = !gutenberg;
            StringBuilder text = new StringBuilder();

            for (String line; (line = br.readLine()) != null; ) {
                if (line.isEmpty()) {
                    continue;
                }

                if (gutenberg) {
                    if (line.contains("START OF THIS PROJECT GUTENBERG EBOOK")
                            || line.contains("START OF THE PROJECT GUTENBERG EBOOK")) {
                        atBook = true;
                        continue;
                    }
                }

                if (atBook) {
                    if (gutenberg) {
                        if (line.contains("End of the Project Gutenberg EBook")
                                || line.contains("End of the Project Gutenberg Ebook")
                                || line.contains("End of Project Gutenberg’s")) {
                            atBook = false;
                        }
                    }
                    text.append(line).append(" ");
                }
            }

            tagText(text.toString());

            return true;
        } catch (IOException e) {
            System.out.println("[Error - readPlainText] Couldn't find file at " + path);
            e.printStackTrace();
            System.exit(2);
        } catch (NullPointerException e) {
            System.out.println("[Error - readPlainText] Null pointer for file at " + path);
            e.printStackTrace();
            System.exit(2);
        }
        return false;
    }

    /**
     * Reads and tags a PDF file
     *
     * @return true if successfully finished
     */
    private boolean readPDF() {
        try {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDFParser parser = new PDFParser(new RandomAccessFile(path, "r"));
            parser.parse();

            tagText(pdfStripper.getText(parser.getPDDocument()));
            return true;
        } catch (IOException e) {
            System.out.println("[Error - readPDF] IOException when opening PDFParser for " + path.getName());
            e.printStackTrace();
        }
        System.exit(2);
        return false;
    }

    /**
     * Tag a text for parts of speech
     *
     * @param text Text to be tagged
     */
    void tagText(String text) {
        Annotation doc = new Annotation(text);
        pipeline.annotate(doc);

        for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
            updateStatsFromSentence(sentence);
        }

        removeStopWords();
    }

    private void updateStatsFromSentence(CoreMap sentence) {
        sentenceCount++;
        if (longestSentence == null || sentence.size() > longestSentence.size()) {
            longestSentence = sentence;
        }

        for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
            updateStatsFromToken(token);
        }
    }

    private void updateStatsFromToken(CoreLabel token) {
        String word = token.word().toLowerCase();
        String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
        String tag = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        tag = TextTools.posAbbrev.get(tag);

        updateSyllables(word);

        // Skip over punctuation
        if (!stripPunctuation(word).isEmpty()) {
            updateWords(word);
            updateLemmas(lemma);
            updatePartsOfSpeech(tag);
        }
    }

    private void updateWords(String word) {
        wordCount++;
        words.increaseFreq(word.toLowerCase());
    }

    private void updateSyllables(String word) {
        int wordSyllables = TextTools.getSyllableCount(word);
        syllableCount += wordSyllables;
        if (wordSyllables == 1) {
            syllables.increaseFreq("Monosyllabic");
        } else {
            syllables.increaseFreq("Polysyllabic");
        }
    }

    private void updateLemmas(String lemma) {
        if (!stripPunctuation(lemma).isEmpty()) {
            lemmas.increaseFreq(lemma);
        }
    }

    private void updatePartsOfSpeech(String tag) {
        if (tag != null) {
            partsOfSpeech.increaseFreq(tag);
        }
    }

    public String getName() {
        // If there is no author don't add the " by XXX"
        if (author.isEmpty()) {
            return title;
        }
        return title + " by " + author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    private void removeStopWords() {
        words.stripStopWords();
        lemmas.stripStopWords();
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
    }

    public boolean hasResults(boolean i, boolean j) {
        File txt = new File("results/txt/" + subdirectory + "/" + getName() + " Results.txt");
        File img = new File("results/img/" + subdirectory + "/" + getName() + " POS Distribution Results.jpeg");
        File diffImg = new File("results/img/" + subdirectory + "/" + getName() + " Difficulty Results.jpeg");
        File json = new File("results/json/" + subdirectory + "/" + getName() + " Results.json");

        if (!i && !j) {
            return txt.exists();
        } else if (i && j) {
            return txt.exists() && img.exists() && diffImg.exists() && json.exists();
        } else if (i) {
            return txt.exists() && img.exists() && diffImg.exists();
        } else {
            return txt.exists() && json.exists();
        }
    }

    public void givePipeline(StanfordCoreNLP pipeline) {
        this.pipeline = pipeline;
    }

    public boolean isGutenberg() {
        return gutenberg;
    }

    long getWordCount() {
        return wordCount;
    }

    FreqMap<String, Integer> getPartsOfSpeech() {
        return partsOfSpeech;
    }

    public FreqMap<String, Integer> getWords() {
        return words;
    }

    FreqMap<String, Integer> getLemmas() {
        return lemmas;
    }

    public FreqMap<String, Integer> getSyllables() {
        return syllables;
    }

    long getSyllableCount() {
        return syllableCount;
    }

    long getSentenceCount() {
        return sentenceCount;
    }

    private String stripPunctuation(String word) {
        return word.replaceAll("\\W", "");
    }

    public String getSubdirectory() {
        return subdirectory;
    }

    CoreMap getLongestSentence() {
        return longestSentence;
    }
}