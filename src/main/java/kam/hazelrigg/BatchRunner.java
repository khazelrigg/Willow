package kam.hazelrigg;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Stream;

class BatchRunner {
    private static Logger logger = Willow.getLogger();

    private BatchRunner() {
        throw new IllegalStateException("Utility class");
    }

    private static final ArrayList<Runner> runners = new ArrayList<>();

    static void passCommandLine(CommandLine cmd) {
        Runner.setOptions(cmd);
    }

    static void startRunners(Path path, int threads) {
        if (path.toFile().isDirectory()) {
            openDirectory(path);
        } else if (path.toFile().isFile()) {
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
            logger.error("Null pointer walking files in {}", directory.getFileName().toString());
        } catch (IOException e) {
            logger.error("IOException walking files in {}", directory.getFileName().toString());
        }
    }

    private static void openFile(Path f) {
        runners.add(new Runner(f, f));
    }
}

class Runner implements Runnable {
    private static CommandLine cmd;
    private static Logger logger = Willow.getLogger();
    private Book book;
    private OutputWriter ow;

    Runner(Path path, Path sub) {
        String parentOfSub = sub.getFileName().toString();
        if (path == sub) {
            this.book = new Book();
        } else {
            this.book = new Book(parentOfSub);
        }

        if (cmd.hasOption("verbose")) {
            logger.info("New book with path at {}", book.getPath());
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
        book.setTitleFromText();
        book.readText(cmd.hasOption("economy"));

        this.ow = new OutputWriter(book);
        ow.setVerbose(cmd.hasOption("verbose"));
        writeResults();
        printFinishTime();
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

    private void printFinishTime() {
        long currentTime = System.currentTimeMillis();
        int seconds = (int) ((currentTime - Willow.START_TIME) / 1000);
        logger.info("Finished analysis of {} in {}s.", book.getName(), seconds);
    }

    public void run() {
        boolean hasResults = book.hasResults(cmd.hasOption("images"), cmd.hasOption("json"));

        if (cmd.hasOption("overwrite")) {
            runBook();
        } else if (hasResults) {
            String pathString = book.getPath().getFileName().toString();
            logger.info("{} already has results", pathString);
        } else {
            runBook();
        }
    }

}