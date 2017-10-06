package kam.hazelrigg;

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
     *
     * @return The title of the book
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title of a book
     *
     * @param title Title to use
     */
    public void setTitle(String title) {
        this.title = title;
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
