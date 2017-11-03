package kam.hazelrigg;

import java.io.File;

public class Runner extends Thread {

    private final Book book;
    private final File file;
    public static boolean running = false;

    Runner(File file, File sub, String start) {
        new Thread(this);
        this.file = file;
        this.book = new Book();
        this.book.setPath(file);
        System.out.println(sub.getPath());
        System.out.println(sub.getParentFile().toString().substring(start.length()));

        this.book.setSubdirectory(sub.getParentFile().toString().substring(start.length() + 1));
    }

    /**
     * Actions to perform with each book
     */
    private void runBook() {
        book.analyseText();
        book.writeText();
        book.makePosGraph();
        book.makeDifficultyGraph();
    }

    @Override
    public void run() {
        running = true;
        book.setTitleFromText(file);

        if (book.resultsFileExists()) {
            System.out.println("☑ - " + file.getName() + " already has results");
        } else {
            runBook();
        }

        running = false;
    }

}
