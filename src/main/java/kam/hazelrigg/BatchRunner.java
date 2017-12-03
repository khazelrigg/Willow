package kam.hazelrigg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

class BatchRunner {
    private static ArrayList<Runner> runners = new ArrayList<>();

    static void passOptions(HashMap<String, Boolean> options) {
        Runner.setOptions(options);
    }

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
    private static HashMap<String, Boolean> options = new HashMap<>();

    Runner(File file, File sub) {
        new Thread(this);
        this.file = file;

        String parentOfSub = sub.toString();
        this.book = new Book(parentOfSub);

        if (options.get("v")) {
            System.out.println("┌══════════[ NEW BOOK ]══════════╾\n│ ┌╾ " + parentOfSub
                    + "\n│ └──╾ " + file.getPath() + "\n└════════════════════════════════╾\n");
        }

        this.book.setPath(file);
    }

    /**
     * Actions to perform with each book
     */
    private void runBook() {
        book.readText(options.get("economy"));
        OutputWriter ow = new OutputWriter(book);
        ow.writeTxt();
        if (options.get("json")) {
            ow.writeJson();
        }

        if (options.get("images")) {
            ow.makeDiffGraph();
            ow.makePosGraph();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("[FINISHED] Completely finished " + book.getName() + " in "
                + (endTime - WordCount.startTime) / 1000 + "s.");

        this.book = null;
    }

    static void setOptions(HashMap<String, Boolean> options) {
        Runner.options = options;
    }

    @Override
    public void run() {
        book.setTitleFromText(file);

        if (options.get("overwrite")) {
            runBook();
        } else if (book.resultsFileExists(options.get("images"), options.get("json"))) {
            System.out.println("☑ - " + file.getName() + " already has results");
        } else {
            runBook();
        }
    }
}