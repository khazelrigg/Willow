package kam.hazelrigg;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Book {
    final FreqMap posFreq;
    final FreqMap wordFreq;
    final FreqMap lemmaFreq;
    final FreqMap difficultyMap;

    String title;
    String author;
    public String subdirectory;
    long wordCount;
    long syllableCount;
    long sentenceCount;
    CoreMap longestSentence;
    private boolean gutenberg;
    private File path;
    private StanfordCoreNLP pipeline;

    public Book() {
        this.title = "";
        this.author = "";
        this.gutenberg = false;
        this.subdirectory = "";
        this.pipeline = WordCount.pipeline;

        this.posFreq = new FreqMap();
        this.wordFreq = new FreqMap();
        this.lemmaFreq = new FreqMap();
        this.difficultyMap = new FreqMap();
    }

    public Book(String subdirectory) {
        this.title = "";
        this.author = "";
        this.gutenberg = false;
        this.subdirectory = subdirectory;
        this.pipeline = WordCount.pipeline;

        this.posFreq = new FreqMap();
        this.wordFreq = new FreqMap();
        this.lemmaFreq = new FreqMap();
        this.difficultyMap = new FreqMap();
    }
    //TODO Add polysyndeton and parallelism statistics. Look into polyptoton and alliteration as well


    /**
     * Get the title of a book.
     *
     * @param text File to find title of
     */
    public void setTitleFromText(File text) {
        //TODO for files that are not gutenbergs remove extension from title
        String title = "";
        String author = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(text));
            String firstLine = br.readLine();

            // If the first line is very short skip over it
            // TODO What to do if entire file is empty
            while (firstLine.length() < 1) {
                firstLine = br.readLine();
            }

            // Cases of Gutenberg books to check
            if (firstLine.contains("The Project Gutenberg EBook of")) {
                firstLine = firstLine.substring(31);
                this.gutenberg = true;
            } else if (firstLine.contains("Project Gutenberg's")
                    || firstLine.contains("Project Gutenberg’s")) {
                firstLine = firstLine.substring(20);
                this.gutenberg = true;
            }

            // If the pattern "title by author" appears split at the word 'by' to get author and title
            if (gutenberg && firstLine.contains("by")) {
                title = firstLine.substring(0, firstLine.lastIndexOf("by")).trim();
                author = firstLine.substring(firstLine.lastIndexOf("by") + 2).trim();
            } else {
                title = text.getName();
                author = "";
            }

            // Remove any trailing commas
            if (title.endsWith(",")) {
                title = title.substring(0, title.length() - 1);
            }

        } catch (IOException e) {
            System.out.println("[Error - SetTitle] Error opening file for setting title");
            e.printStackTrace();
        }

        this.title = title;
        this.author = author;
    }

    /**
     * Tag a text for parts of speech
     *
     * @param text Text to be tagged
     */
    void tagText(String text) {
        Annotation doc = new Annotation(text);
        pipeline.annotate(doc);

        // Actions for each sentence in the document
        for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
            sentenceCount++;
            if (longestSentence == null || sentence.size() > longestSentence.size()) {
                longestSentence = sentence;
            }

            // Actions for each word
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.word().replaceAll("\\W", "");
                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                String tag = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);


                if (TextTools.getSyllableCount(token.word()) == 1) {
                    difficultyMap.increaseFreq("Monosyllabic");
                } else {
                    difficultyMap.increaseFreq("Polysyllabic");
                }

                // Skip over punctuation
                if (!word.isEmpty()) {
                    wordCount++;
                    wordFreq.increaseFreq(token.word().toLowerCase());
                }

                if (!lemma.replaceAll("\\W", "").isEmpty()) {
                    lemmaFreq.increaseFreq(lemma);
                }

                if (tag != null) {
                    posFreq.increaseFreq(tag);
                }

                syllableCount += TextTools.getSyllableCount(word);
            }
        }

        // Remove stop-words from the word counts
        wordFreq.stripFromFreq();
    }

    /**
     * Reads a text file and tags each line for parts of speech as well as counts word frequencies.
     */
    public boolean readText() {

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            System.out.println("☐ - Starting analysis of " + getName());

            boolean atBook = !gutenberg;
            StringBuilder text = new StringBuilder();

            for (String line; (line = br.readLine()) != null; ) {
                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Skip over the Gutenberg headers
                if (gutenberg && !atBook) {
                    if (line.contains("START OF THIS PROJECT GUTENBERG EBOOK")
                            || line.contains("START OF THE PROJECT GUTENBERG EBOOK")) {
                        atBook = true;
                        line = br.readLine();
                    } else {
                        continue;
                    }
                }

                // Stop at the Gutenberg footer
                if (gutenberg) {
                    if (line.contains("End of the Project Gutenberg EBook")
                            || line.contains("End of Project Gutenberg’s")) {
                        atBook = false;
                    }
                }

                if (atBook) {
                    text.append(line).append(" ");
                }
            }
            tagText(text.toString());

            long endTime = System.currentTimeMillis();
            System.out.println(OutputWriter.ANSI_GREEN +
                    "\n☑ - Finished analysis of " + getName() + " in "
                    + (endTime - WordCount.startTime) / 1000 + "s." + OutputWriter.ANSI_RESET);
            return true;
        } catch (IOException e) {
            System.out.println("[Error - readText] Couldn't find file at " + path);
        } catch (NullPointerException e) {
            System.out.println("[Error - readText] Null pointer for file at " + path);
        }

        return false;
    }

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

    void givePipeline(StanfordCoreNLP pipeline) {
        this.pipeline = pipeline;
    }

    public boolean isGutenberg() {
        return gutenberg;
    }

    public long getWordCount() {
        return wordCount;
    }
}
