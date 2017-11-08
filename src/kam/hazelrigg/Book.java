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
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class Book {
    final FreqMap posFreq;
    final FreqMap wordFreq;
    final FreqMap difficultyMap;
    final FreqMap lemmaMap;

    String title;
    String author;
    public String subdirectory;
    long wordCount;
    long syllableCount;
    long sentenceCount;
    String longestSentence;
    private boolean gutenberg;
    private File path;

    public Book() {
        this.title = "";
        this.author = "";
        this.gutenberg = false;
        this.subdirectory = "";
        this.longestSentence = "";

        this.posFreq = new FreqMap();
        this.wordFreq = new FreqMap();
        this.difficultyMap = new FreqMap();
        this.lemmaMap = new FreqMap();
    }

    /**
     * Get the title of a book.
     *
     * @param text File to find title of
     */
    public void setTitleFromText(File text) {
        String title = "";
        String author = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(text));
            String firstLine;
            firstLine = br.readLine();

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

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.title = title;
        this.author = author;
    }

    /**
     * Tag a text for parts of speech, gets lemma counts, and wordcounts
     *
     * @param text Text to be tagged
     */
    private void analyseText(String text) {
        // Set up CoreNlp pipeline
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        Annotation doc = new Annotation(text);
        pipeline.annotate(doc);

        // Get pos tags as human readable format
        HashMap<String, String> posAbbrev = TextTools.nonAbbreviate();

        List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            sentenceCount++;

            // Check for new longest sentence
            if (sentence.toString().length() > longestSentence.length()) {
                longestSentence = sentence.toString();
            }

            // Tokenize sentence and loop through each token
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                wordCount++;

                String word = token.get(CoreAnnotations.TextAnnotation.class).replaceAll("\\W", "");
                String pos = posAbbrev
                        .get(token.get(CoreAnnotations.PartOfSpeechAnnotation.class).toLowerCase());
                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);

                if (pos != null) {
                    posFreq.increaseFreq(pos);
                }
                // Set syllable count information
                if (TextTools.getSyllableCount(word) > 1) {
                    difficultyMap.increaseFreq("poly");
                } else {
                    difficultyMap.increaseFreq("mono");
                }


                if (!word.isEmpty()) {
                    wordFreq.increaseFreq(word);
                    lemmaMap.increaseFreq(lemma);
                }
            }
        }

        syllableCount = TextTools.getSyllableCount(text);
    }

    /**
     * Reads the text file from path and creates a string passed to tagger
     */
    public void readText() {

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            System.out.println("☐ - Starting analysis of " + getName());
            long startTime = System.currentTimeMillis();

            Boolean atBook = false;
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
                        break;
                    }
                }

                text.append(line);
            }
            br.close();

            analyseText(text.toString());

            long endTime = System.currentTimeMillis();
            System.out.println(
                    "\n☑ - Finished analysis of " + getName() + " in " + (endTime - startTime) / 1000 + "s.");

        } catch (IOException e) {
            System.out.println("Couldn't find file at " + path);
        }

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

    public boolean resultsFileExists() {
        File txt = new File("results/txt/" + subdirectory + "/" + getName() + " Results.txt");
        File img = new File("results/img/" + subdirectory + "/" + getName() + " POS Distribution Results.jpeg");
        File img2 = new File("results/img/" + subdirectory + "/" + getName() + " Difficulty Results.jpeg");
        File json = new File("results/json/" + subdirectory + "/" + getName() + " Results.json");
        return txt.exists() && img.exists() && img2.exists() && json.exists();
    }

    void setSubdirectory(String dir) {
        this.subdirectory = dir;
    }
}
