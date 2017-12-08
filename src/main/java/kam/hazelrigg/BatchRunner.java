package kam.hazelrigg;

import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

class BatchRunner {
    private static ArrayList<Runner> runners = new ArrayList<>();

    static void passOptions(Options options) {
        Runner.setOptions(options);
    }

    /**
     * Start a thread pool to analyse texts
     *
     * @param file File/Dir to run
     * @param threads Number of threads to use in pool
     */
    static void startRunners(File file, int threads) {
        if (file.isDirectory()) {
            openDirectory(file);
        } else if (file.isFile()) {
            openFile(file);
        }

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
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
            System.out.println("[Error - openDirectory] NullPointer when walking files in "
                    + directory.getName());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("[Error - openDirectory] IOException when walking files in "
                    + directory.getName());
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
    private static Options options;

    Runner(File file, File sub) {
        new Thread(this);
        this.file = file;

        String parentOfSub = sub.getName();
        if (file == sub) {
            this.book = new Book();
        } else {
            this.book = new Book(parentOfSub);
        }

        if (options.hasOption("verbose")) {
            System.out.println("┌══════════[ NEW BOOK ]══════════╾\n│ ┌╾ " + parentOfSub
                    + "\n│ └──╾ " + file.getPath() + "\n└════════════════════════════════╾\n");
        }

        this.book.setPath(file);
    }

    /**
     * Actions to perform with each book
     */
    private void runBook() {
        book.readText(options.hasOption("economy"));
        long endReadTime = System.currentTimeMillis();
        System.out.println(OutputWriter.ANSI_GREEN +
                "\n☑ - Finished analysis of " + book.getName() + " in "
                + (endReadTime - Willow.startTime) / 1000 + "s." + OutputWriter.ANSI_RESET);

        OutputWriter ow = new OutputWriter(book);
        ow.writeTxt();

        if (options.hasOption("json")) {
            ow.writeJson();
        }


        if (options.hasOption("csv")) {
            ow.writeCSV();
        }

        if (options.hasOption("images")) {
            ow.makeSyllableDistributionGraph();
            ow.makePartsOfSpeechGraph();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("[FINISHED] Completely finished " + book.getName() + " in "
                + (endTime - Willow.startTime) / 1000 + "s.");

        this.book = null;
    }

    static void setOptions(Options options) {
        Runner.options = options;
    }

    @Override
    public void run() {
        book.setTitleFromText(file);

        if (options.hasOption("overwrite")) {
            runBook();
        } else if (book.hasResults(options.hasOption("images"), options.hasOption("json"))) {
            System.out.println("☑ - " + file.getName() + " already has results");
        } else {
            runBook();
        }
    }
}