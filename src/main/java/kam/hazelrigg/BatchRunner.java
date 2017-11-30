package kam.hazelrigg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class BatchRunner {
    private static ArrayList<Runner> runners = new ArrayList<>();
    private static boolean running;

    /**
     * Start a runners for each file in a directory
     *
     * @param directory Directory to open
     */
    public static void startRunners(File directory) {
        openDirectory(directory);
        running = true;

        for (Runner runner : runners) {
            runner.start();

            running = false;
        }
    }

    /**
     * Opens a directory analysing each file on its own thread
     *
     * @param directory Directory to open
     */
    private static void openDirectory(File directory) {
        try {
            Files.walk(Paths.get(directory.getName())).filter(Files::isRegularFile)
                    .forEach(f ->
                            runners.add(new Runner(f.toFile(), f.toFile(), directory.getName())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Runner extends Thread {
    private Book book;
    private final File file;

    Runner(File file, File sub, String start) {
        new Thread(this);
        this.file = file;
        try {
            String parentOfSub = sub.getParentFile().toString().substring(start.length() + 1);
            this.book = new Book(parentOfSub);
            System.out.println("┌══════════[ NEW BOOK ]══════════╾\n│ ┌╾ " + parentOfSub
                    + "\n│ └──╾ " + file.getPath() + "\n└════════════════════════════════╾\n");

        } catch (StringIndexOutOfBoundsException e) {
            //If theres is no subdirectory parent we must be in the parent directory
            this.book = new Book();
            System.out.println("┌══════════[ NEW BOOK ]══════════╾\n| ┌╾ ROOT\n│ └──╾ "
                    + file.getPath() + "\n└════════════════════════════════╾\n");
        }
        this.book.setPath(file);
    }

    /**
     * Actions to perform with each book
     */
    private void runBook() {
        book.readText();
        OutputWriter ow = new OutputWriter(book);
        ow.writeTxt();
        ow.writeJson();
        ow.makeDiffGraph();
        ow.makePosGraph();
        long endTime = System.currentTimeMillis();
        System.out.println("[FINISHED] Completely finished " + book.getName() + " in "
                + (endTime - WordCount.startTime) / 1000 + "s.");
    }

    @Override
    public void run() {
        book.setTitleFromText(file);

        if (book.resultsFileExists()) {
            System.out.println("☑ - " + file.getName() + " already has results");
        } else {
            runBook();
        }
    }
}