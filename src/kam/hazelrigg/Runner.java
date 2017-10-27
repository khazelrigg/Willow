package kam.hazelrigg;

import java.io.File;

public class Runner extends Thread {

    private final Book book;
    private final File file;
    public static boolean running = false;

    Runner(File file) {
        new Thread(this);
        this.file = file;
        this.book = new Book();
        this.book.setPath(file);
    }

    Runner(File file, String subDir) {
        new Thread(this);
        this.file = file;
        this.book = new Book();
        this.book.setPath(file);
        this.book.setSubdir(subDir);
    }

    @Override
    public void run() {
        running = true;
        book.setTitleFromText(file);

        System.out.println("Starting thread for " + file.getName());
        if (book.resultsFileExists()) {
            System.out.println("☑ - " + file.getName() + " already has results");
        } else {
            // Execute the following when running
            book.analyseText();
            book.writeFrequencies();
            book.makePosGraph();
            book.makeDifficultyGraph();
        }

        running = false;
    }

}
