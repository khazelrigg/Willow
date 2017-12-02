package kam.hazelrigg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class BatchRunner {
    private static ArrayList<Runner> runners = new ArrayList<>();
    static boolean createJson = false;
    static boolean createImg = false;
    static boolean overwrite = false;

    /**
     * Start a runner on a new thread for each file in a directory
     *
     * @param file File/Dir to run
     */
    static void startRunners(File file) {
        if (file.isDirectory()) {
            openDirectory(file);
        } else if (file.isFile()) {
            openFile(file);
        }

        // Get available cpus and start a fixed thread pool to execute books
        int cpus = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(cpus + 1);

        Runner.createImage(createImg);
        Runner.createJson(createImg);
        Runner.overwrite(overwrite);

        runners.forEach(executor::execute);
        executor.shutdown();
    }

    /**
     * Opens a directory analysing each file on its own thread
     *
     * @param directory Directory to open
     */
    private static void openDirectory(File directory) {
        try {
            Files.walk(Paths.get(directory.getAbsolutePath())).filter(Files::isRegularFile)
                    .forEach(f -> {
                        File file = f.toFile();
                        File subFolder = f.toAbsolutePath().getParent().getFileName().toFile();
                        runners.add(new Runner(file, subFolder));
                    });
        } catch (NullPointerException e) {
            System.out.println("[Error - openDirectory] NullPointer when attempting to walk files in " + directory.getName());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("[Error - openDirectory] IOException when attempting to walk files in " + directory.getName());
            e.printStackTrace();
        }
    }

    private static void openFile(File f) {
        runners.add(new Runner(f, f));
    }
}

class Runner extends Thread {
    private Book book;
    private final File file;
    private static boolean createImg = false;
    private static boolean createJson = false;
    private static boolean overwrite = false;

    Runner(File file, File sub) {
        new Thread(this);
        this.file = file;

        try {
            String parentOfSub = sub.toString();
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
        if (createJson) {
            ow.writeJson();
        }

        if (createImg) {
            ow.makeDiffGraph();
            ow.makePosGraph();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("[FINISHED] Completely finished " + book.getName() + " in "
                + (endTime - WordCount.startTime) / 1000 + "s.");

        this.book = null;
    }

    static void createImage(boolean b) {
        createImg = b;
    }

    static void createJson(boolean b) {
        createJson = b;
    }

    static void overwrite(boolean b) {
        overwrite = b;
    }

    @Override
    public void run() {
        book.setTitleFromText(file);

        if (overwrite) {
            runBook();
        } else if (book.resultsFileExists(createImg, createJson)) {
            System.out.println("☑ - " + file.getName() + " already has results");
        } else {
            runBook();
        }
    }
}