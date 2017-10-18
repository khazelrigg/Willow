package kam.hazelrigg;

import java.io.File;
import java.util.ArrayList;

public class Runner extends Thread {

    private final Book book;
    private final File file;
    private final Thread thread;
    public static boolean running = false;

    private Runner(File file) {
        thread = new Thread(this);
        this.file = file;
        this.book = new Book();
        this.book.setPath(file);
    }

    /**
     * Opens a directory analysing each file on its own thread
     * @param directory Directory to open
     */
    public static void openDirectory(File directory) {
        File[] files = directory.listFiles();
        ArrayList<Runner> runners = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    openDirectory(file);
                    continue;
                } else {
                    runners.add(new Runner(file));
                }
            }
        }


        for (Runner runner : runners) {
            runner.start();

            while (running) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        running = true;
        book.setTitleFromText(file);

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
