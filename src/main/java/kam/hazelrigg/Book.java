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
    final FreqMap<String, Integer> posFreq;
    final FreqMap<String, Integer> wordFreq;
    final FreqMap<String, Integer> lemmaFreq;
    final FreqMap<String, Integer> difficultyMap;

    String title;
    String author;
    public String subdirectory;
    long wordCount;
    long syllableCount;
    long sentenceCount;
    private boolean gutenberg;
    private File path;
    CoreMap longestSentence;
    private StanfordCoreNLP pipeline;

    public Book() {
        this.title = "";
        this.author = "";
        this.gutenberg = false;
        this.subdirectory = "";
        this.pipeline = WordCount.pipeline;

        this.posFreq = new FreqMap<>();
        this.wordFreq = new FreqMap<>();
        this.lemmaFreq = new FreqMap<>();
        this.difficultyMap = new FreqMap<>();
    }

    public Book(String subdirectory) {
        this.title = "";
        this.author = "";
        this.gutenberg = false;
        this.subdirectory = subdirectory;
        this.pipeline = WordCount.pipeline;

        this.posFreq = new FreqMap<>();
        this.wordFreq = new FreqMap<>();
        this.lemmaFreq = new FreqMap<>();
        this.difficultyMap = new FreqMap<>();
    }
    //TODO Add polysyndeton and parallelism statistics. Look into polyptoton and alliteration as well


    /**
     * Get the title of a book.
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
                    title = path.getName();
                    author = "";
                }
            }

            this.title = title;
            this.author = author;


        } catch (IOException e) {
            System.out.println("[Error - SetTitle] Error opening " + text.getName() + " for setting title");
            e.printStackTrace();
        }

        //this.title = title;
        //this.author = author;
    }

    private boolean isGutenbergText(String line) {
        return line.toLowerCase().contains("gutenberg");
    }


    /**
     * Finds the appropriate file type of book to then read text
     */
    public boolean readText(Boolean econ) {
        try {
            System.out.println("☐ - Starting analysis of " + getName());
            String fileType = Files.probeContentType(path.toPath());

            switch (fileType) {
                case "text/plain":
                    if (econ) {
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

        } catch (IOException e) {
            System.out.println("[Error - readText] IOException when probing file type");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("[Error - readText] NullPointerException when probing file type");
            e.printStackTrace();
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
            Annotation doc;

            for (String line; (line = br.readLine()) != null; ) {
                if (line.isEmpty()) {
                    continue;
                }

                // Find the header of a Gutenberg text
                if (gutenberg) {
                    if (line.contains("START OF THIS PROJECT GUTENBERG EBOOK")
                            || line.contains("START OF THE PROJECT GUTENBERG EBOOK")) {
                        atBook = true;
                        continue;
                    }
                }

                // Find the footer of a Gutenberg text and stop reading
                if (atBook) {
                    if (gutenberg) {
                        if (line.contains("End of the Project Gutenberg EBook")
                                || line.contains("End of the Project Gutenberg Ebook")
                                || line.contains("End of Project Gutenberg’s")) {
                            break;
                        }
                    }

                    // Add the current line to the buffer
                    buffer.append(" ").append(line.trim());
                    doc = new Annotation(buffer.toString());
                    pipeline.annotate(doc);

                    for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
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
                            updateStats(sentence);
                            break;
                        }
                    }
                }
            }

            // Remove stop-words from the word counts
            wordFreq.stripStopWords();
            lemmaFreq.stripStopWords();
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
     * Reads and tags a plain text file by loading into memory
     *
     * @return true if successfully finished
     */
    private boolean readPlainText() {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            boolean atBook = !gutenberg;
            StringBuilder text = new StringBuilder();

            for (String line; (line = br.readLine()) != null; ) {
                // Skip empty lines
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
            updateStats(sentence);
        }

        // Remove stop-words from the word counts
        wordFreq.stripStopWords();
        lemmaFreq.stripStopWords();
    }

    private void updateStats(CoreMap sentence) {
        sentenceCount++;
        if (longestSentence == null || sentence.size() > longestSentence.size()) {
            longestSentence = sentence;
        }


        for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
            String word = token.word().toLowerCase().replaceAll("\\W", "");
            String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
            String tag = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            tag = TextTools.posAbbrev.get(tag);

            // Skip over punctuation
            if (!word.isEmpty()) {
                wordCount++;
                wordFreq.increaseFreq(word);

                if (TextTools.getSyllableCount(token.word()) == 1) {
                    difficultyMap.increaseFreq("Monosyllabic");
                } else {
                    difficultyMap.increaseFreq("Polysyllabic");
                }


                if (!lemma.replaceAll("\\W", "").isEmpty()) {
                    lemmaFreq.increaseFreq(lemma);
                }

                if (tag != null) {
                    posFreq.increaseFreq(tag);
                }
            }

            syllableCount += TextTools.getSyllableCount(word);
        }
    }

    /**
     * Create a name for the book, format: title by author
     *
     * @return Formatted string
     */
    public String getName() {
        // If there is no author don't add the " by XXX"
        if (author.equals("")) {
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

    public void setAuthor(String author) {
        this.author = author;
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
    }

    public boolean resultsFileExists(boolean i, boolean j) {
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
}
