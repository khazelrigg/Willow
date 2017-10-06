package kam.hazelrigg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Book {
    private String title;
    private String author;
    private boolean gutenberg;


    public Book() {
        this.title = "";
        this.author = "";
        this.gutenberg = false;
    }

    /**
     * Get the title of a book
     * @param text File to find title of
     */
     void getTitleFromText(File text) {
         String title = "";
         String author = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(text));
            String firstLine = br.readLine();

            // If the first line is very short skip over it
            while (firstLine.length() < 3) {
                firstLine = br.readLine();
            }

            // Cases of Gutenberg books to check
            if (firstLine.contains("The Project Gutenberg EBook of")) {
                firstLine = firstLine.substring(31);
                this.gutenberg = true;
            }

            if (firstLine.contains("Project Gutenberg's") ||
                    firstLine.contains("Project Gutenberg’s")) {
                firstLine = firstLine.substring(20);
                this.gutenberg = true;
            }

            // If the pattern "title by author" appears split at the word 'by' to get author and title
            if (firstLine.contains("by")) {
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
     * Set the title of a book
     *
     * @param title Title to use
     */
    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    /**
     * Get the author of a book
     *
     * @return The author's name
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Set the author of a book
     *
     * @param author Author to use
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Return if book is a Gutenberg text
     *
     * @return True if it is a Gutenberg text, false otherwise
     */
    public boolean isGutenberg() {
        return gutenberg;
    }

    /**
     * Set whether or not a text is a Gutenberg Book
     *
     * @param gutenberg Boolean set to true if it is a Gutenberg book
     */
    public void setGutenberg(boolean gutenberg) {
        this.gutenberg = gutenberg;
    }

}
