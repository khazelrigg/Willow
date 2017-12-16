package kam.hazelrigg;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Book {
    private final BookStats bookStats = new BookStats();
    private String title = "";
    private String author = "";

    private String subdirectory;

    private boolean gutenberg = false;
    private Path path;
    private StanfordCoreNLP pipeline = Willow.pipeline;

    public Book() {
        this.subdirectory = "";
    }

    public Book(String subdirectory) {
        this.subdirectory = subdirectory;
    }

    /**
     * Get the title of a book by scanning first couple of lines for a title and author.
     *
     * @param path File to find title of
     */
    public void setTitleFromText(Path path) {
        String title = null;
        String author = null;

        try (BufferedReader br = getDecodedBufferedReader()) {
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
                    title = path.getFileName().toString();
                    author = "";
                }
            }

            this.title = title;
            this.author = author;

        } catch (IOException | NullPointerException e) {
            System.out.println("[Error - SetTitle] Error opening "
                    + path.getFileName().toString() + " for setting title");
            e.printStackTrace();
        }
    }

    private boolean isGutenbergText(String line) {
        return line.toLowerCase().contains("gutenberg");
    }

    /**
     * Finds the appropriate file type of book to then reads the text.
     */
    public boolean readText(Boolean economy) {
        System.out.println("☐ - Starting analysis of " + getName());

        try {
            if (path == null) {
                throw new NullPointerException();
            }
            String fileType = FilenameUtils.getExtension(path.getFileName().toString());
            return runFileType(fileType, economy);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean runFileType(String fileType, boolean economy) {
        switch (fileType) {
            case "txt":
                if (economy) {
                    return readPlainTextEconomy();
                } else {
                    return readPlainText();
                }
            case "pdf":
                return readPdf();
            default:
                System.out.println("Unsupported format " + fileType);
        }
        return false;
    }

    /**
     * Reads and tags a plain text file sentence by sentence.
     *
     * @return true if successfully finished
     */
    private boolean readPlainTextEconomy() {
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
     * Reads and tags a plain text file by loading into memory.
     *
     * @return true if successfully finished
     */
    private boolean readPlainText() {
        try (BufferedReader br = getDecodedBufferedReader()) {
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
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Reads and tags a PDF file.
     *
     * @return true if successfully finished
     */
    private boolean readPdf() {
        try {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDFParser parser = new PDFParser(new RandomAccessFile(path.toFile(), "r"));
            parser.parse();

            tagText(pdfStripper.getText(parser.getPDDocument()));
            return true;
        } catch (IOException e) {
            System.out.println("[Error - readPdf] IOException when opening PDFParser for "
                    + path.getFileName());
            e.printStackTrace();
        }
        System.exit(2);
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
     * Tag a text for parts of speech.
     *
     * @param text Text to be tagged
     */
    void tagText(String text) {
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

    public String getName() {
        if (author.isEmpty()) {
            return title;
        }
        return title + " by " + author;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public boolean hasResults(boolean createImage, boolean createJson) {
        Path txt = Paths.get("results", "txt", subdirectory, getName() + " Text Results.txt");
        Path posImg = Paths.get("results", "img", subdirectory, getName() + " POS Distribution Results.jpeg");
        Path syllImg = Paths.get("results", "img", subdirectory, getName() + " Difficulty Results.jpeg");
        Path json = Paths.get("results", "json", subdirectory, getName() + " JSON Results.json");

        boolean txtExists = Files.exists(txt);
        boolean imagesExist = Files.exists(posImg) && Files.exists(syllImg);
        boolean jsonExists = Files.exists(json);

        if (!createImage && !createJson) {
            return txtExists;
        } else if (createImage && createJson) {
            return txtExists && imagesExist && jsonExists;
        } else if (createImage) {
            return txtExists && imagesExist;
        } else {
            return txtExists && jsonExists;
        }
    }

    BookStats getStats() {
        return this.bookStats;
    }

    public void givePipeline(StanfordCoreNLP pipeline) {
        this.pipeline = pipeline;
    }

    public boolean isGutenberg() {
        return gutenberg;
    }

    private String stripPunctuation(String word) {
        return word.replaceAll("\\W", "");
    }

    private boolean isPunctuation(String word) {
        return stripPunctuation(word).isEmpty();
    }

    public String getSubdirectory() {
        return subdirectory;
    }

    public void setSubdirectory(String subdirectory) {
        this.subdirectory = subdirectory;
    }

    public String getTitle() {
        return title;
    }

    private BufferedReader getDecodedBufferedReader() throws IOException {
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);

        InputStream inputStream = Files.newInputStream(path);
        InputStreamReader reader = new InputStreamReader(inputStream, decoder);

        return new BufferedReader(reader);
    }
}
