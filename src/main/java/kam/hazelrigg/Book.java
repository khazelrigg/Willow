package kam.hazelrigg;

import kam.hazelrigg.readers.TextReader;
import kam.hazelrigg.readers.TextReaderFactory;
import org.slf4j.Logger;

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
    private Path path;
    private String title = "";
    private String author = "";
    private String subdirectory;
    private boolean economy;
    private TextReader textReader;
    private Logger logger = Willow.getLogger();

    public Book() {
        this.subdirectory = "";
    }

    public Book(String subdirectory) {
        this.subdirectory = subdirectory;
    }

    /**
     * Get the title of a book by scanning first couple of lines for a title and author.
     */
    public void setTitleFromText() {
        boolean gutenberg = false;

        try (BufferedReader br = getDecodedBufferedReader()) {
            String line = br.readLine();

            // If the first line is empty skip over it until finding a full line
            while (line.isEmpty()) {
                line = br.readLine();
            }

            if (lineContainsGutenberg(line)) {
                gutenberg = true;
                getGutenbergInfo(br);
            } else {
                getRegularTextInfo(br, line);
            }

            bookStats.setGutenberg(gutenberg);

        } catch (IOException e) {
            logger.error("Could not open {} to set title", path.getFileName().toString());
        }
    }

    private boolean lineContainsGutenberg(String line) {
        return line.toLowerCase().contains("gutenberg");
    }

    private void getGutenbergInfo(BufferedReader br) throws IOException {
        while (title.isEmpty() || author.isEmpty()) {
            String line = br.readLine();
            if (line.contains("itle:")) {
                title = line.substring(6).trim();
            } else if (line.contains("Author:")) {
                author = line.substring(7).trim();
            }
        }
    }

    private void getRegularTextInfo(BufferedReader br, String line) throws IOException {
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

    /**
     * Finds the appropriate file type of book to then reads the text.
     */
    public boolean readText(Boolean economy) {
        this.economy = economy;
        try {
            logger.debug("Starting analysis of {}", getName());
            TextReaderFactory factory = new TextReaderFactory();
            textReader = factory.getTextReader(this);
            textReader.readText();

            return true;
        } catch (NullPointerException e) {
            logger.error("Null pointer exception for file {}", path.toString());
        } catch (IOException e) {
            logger.error("Unsupported format for file {}", path.toString());
        }
        return false;
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
        setTitleFromText();
        String resultsDirectory = "results";

        Path txt = Paths.get(resultsDirectory, "txt", subdirectory, getName()
                + " Text Results.txt");
        Path posImg = Paths.get(resultsDirectory, "img", subdirectory, getName()
                + " POS Distribution Results.jpeg");
        Path syllableImage = Paths.get(resultsDirectory, "img", subdirectory, getName()
                + " Difficulty Results.jpeg");
        Path json = Paths.get(resultsDirectory, "json", subdirectory, getName()
                + " JSON Results.json");

        boolean txtExists = txt.toFile().exists();
        boolean imagesExist = posImg.toFile().exists() && syllableImage.toFile().exists();
        boolean jsonExists = json.toFile().exists();

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

    public BookStats getStats() {
        return this.bookStats;
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

    TextReader getTextReader() {
        return textReader;
    }

    private BufferedReader getDecodedBufferedReader() throws IOException {
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);

        InputStream inputStream = Files.newInputStream(path);
        InputStreamReader reader = new InputStreamReader(inputStream, decoder);

        return new BufferedReader(reader);
    }

    public boolean getEconomy() {
        return economy;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
