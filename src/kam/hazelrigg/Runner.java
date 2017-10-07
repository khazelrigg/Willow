package kam.hazelrigg;

import java.io.File;

public class Runner implements Runnable{
    public Book book;
    public File file;
    public Thread thread;
    public boolean running = false;

    Runner(File file) {
        thread = new Thread(this);
        this.file = file;
        this.book = new Book();
        this.book.setPath(file);
        thread.start();
    }

    @Override
    public void run() {
        this.running = true;

        book.setTitleFromText(file);

        if (book.resultsFileExists()) {
            System.out.println("☑ - " + file.getName() + " already has results");
        } else {
            book.analyseText();
            book.writeFrequencies();
        }

        this.running = false;
    }

}
