package kam.hazelrigg;

import org.apache.commons.cli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Stream;

class BatchRunner {
    private static ArrayList<Runner> runners = new ArrayList<>();

    static void passCommandLine(CommandLine cmd) {
        Runner.setOptions(cmd);
    }

    static void startRunners(Path path, int threads) {
        if (Files.isDirectory(path)) {
            openDirectory(path);
        } else if (Files.isRegularFile(path)) {
            openFile(path);
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
    private static void openDirectory(Path directory) {
        try (Stream<Path> fileWalker = Files.walk(directory)) {
            fileWalker
                    .filter(Files::isRegularFile)
                    .forEach(file -> runners.add(new Runner(file, file.getParent())));

        } catch (NullPointerException e) {
            System.out.println("[Error - openDirectory] NullPointer when walking files in "
                    + directory.getFileName().toString());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("[Error - openDirectory] IOException when walking files in "
                    + directory.getFileName().toString());
            e.printStackTrace();
        }
    }

    private static void openFile(Path f) {
        runners.add(new Runner(f, f));
    }
}

class Runner extends Thread {
    private static CommandLine cmd;
    private final Path path;
    private Book book;
    private OutputWriter ow;

    Runner(Path path, Path sub) {
        new Thread(this);
        this.path = path;

        String parentOfSub = sub.getFileName().toString();
        if (path == sub) {
            this.book = new Book();
        } else {
            this.book = new Book(parentOfSub);
        }

        if (cmd.hasOption("verbose")) {
            System.out.println("┌══════════[ NEW BOOK ]══════════╾\n│ ┌╾ " + parentOfSub
                    + "\n│ └──╾ " + path.getFileName().toString() + "\n└════════════════════════════════╾\n");
        }

        this.book.setPath(path);
    }

    static void setOptions(CommandLine cmd) {
        Runner.cmd = cmd;
    }

    /**
     * Actions to perform with each book
     */
    private void runBook() {
        book.readText(cmd.hasOption("economy"));
        long endReadTime = System.currentTimeMillis();
        System.out.println(OutputWriter.ANSI_GREEN +
                "\n☑ - Finished analysis of " + book.getName() + " in "
                + (endReadTime - Willow.startTime) / 1000 + "s." + OutputWriter.ANSI_RESET);

        this.ow = new OutputWriter(book);
        ow.setVerbose(cmd.hasOption("verbose"));
        writeResults();

        long endTime = System.currentTimeMillis();
        System.out.println("[FINISHED] Completely finished " + book.getName() + " in "
                + (endTime - Willow.startTime) / 1000 + "s.");

        this.book = null;
    }

    private void writeResults() {
        ow.writeTxt();
        writeJson();
        writeCsv();
        makeImages();
    }

    private void writeJson() {
        if (cmd.hasOption("json")) {
            ow.writeJson();
        }
    }

    private void writeCsv() {
        if (cmd.hasOption("csv")) {
            ow.writeCsv();
        }
    }

    private void makeImages() {
        if (cmd.hasOption("images")) {

            ow.makeSyllableDistributionGraph();
            ow.makePartsOfSpeechGraph();
        }
    }

    @Override
    public void run() {
        book.setTitleFromText(path);

        if (cmd.hasOption("overwrite")) {
            runBook();
        } else if (book.hasResults(cmd.hasOption("images"), cmd.hasOption("json"))) {
            System.out.println("☑ - " + path.getFileName().toString() + " already has results");
        } else {
            runBook();
        }
    }

}