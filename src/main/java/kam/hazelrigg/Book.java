package kam.hazelrigg;

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
    private BookStats bookStats = new BookStats();
    private Path path;
    private String title = "";
    private String author = "";
    private String subdirectory;

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

            if (lineContainsGutenberg(line)) {
                bookStats.setGutenberg(true);
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

    private boolean lineContainsGutenberg(String line) {
        return line.toLowerCase().contains("gutenberg");
    }

    /**
     * Finds the appropriate file type of book to then reads the text.
     */
    public boolean readText(Boolean economy) {
        System.out.println("☐ - Starting analysis of " + getName());

        try {
            TextReaderFactory factory = new TextReaderFactory();
            TextReader reader = factory.getTextReader(path, economy);
            reader.readText();
            this.bookStats = reader.getStats();
            return true;
        } catch (IOException e) {
            System.out.println("Unsupported format for " + path.toString());
            e.printStackTrace();
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
