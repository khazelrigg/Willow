package kam.hazelrigg;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;

import java.nio.file.Path;

public class Runner implements Runnable {
    private static final Logger logger = Willow.getLogger();
    private Book book;
    private OutputWriter outputWriter;
    private CommandLine commandLine = BatchRunner.getCommandLine();


    Runner(Path path, Path subdirectory) {
        if (path == subdirectory) {
            this.book = new Book();
        } else {
            String parentFolder = subdirectory.getFileName().toString();
            this.book = new Book(parentFolder);
        }

        this.book.setPath(path);
    }

    @Override
    public void run() {
        boolean overwrite = commandLine.hasOption("overwrite");

        boolean createImages = commandLine.hasOption("images");
        boolean createJson = commandLine.hasOption("json");
        boolean bookHasResults = book.hasResults(createImages, createJson);

        if (overwrite) {
            runBook();
        } else if (bookHasResults) {
            logger.info("{} already has results", book.getPath());
        } else {
            runBook();
        }
    }

    private void runBook() {
        book.setTitleFromText();

        boolean economy = commandLine.hasOption("economy");
        boolean successfullyRead = book.readText(economy);

        if (successfullyRead) {
            setUpOutputWriter();
            writeResults();
            printFinishTime();
        }
    }

    private void setUpOutputWriter() {
        outputWriter = new OutputWriter(book);
        boolean verbose = commandLine.hasOption("verbose");
        outputWriter.setVerbose(verbose);
    }

    private void writeResults() {
        outputWriter.writeTxt();
        writeJson();
        writeCsv();
        makeImages();
    }

    private void writeJson() {
        if (commandLine.hasOption("json")) {
            outputWriter.writeJson();
        }
    }

    private void writeCsv() {
        if (commandLine.hasOption("csv")) {
            outputWriter.writeCsv();
        }
    }

    private void makeImages() {
        if (commandLine.hasOption("images")) {
            outputWriter.makeSyllableDistributionGraph();
            outputWriter.makePartsOfSpeechGraph();
        }
    }

    private void printFinishTime() {
        long currentTime = System.currentTimeMillis();
        int seconds = (int) ((currentTime - Willow.START_TIME) / 1000);
        logger.info("Finished analysis of {} in {}s.", book.getName(), seconds);
    }

    public Book getBook() {
        return book;
    }

    void setCommandLine(CommandLine commandLine) {
        this.commandLine = commandLine;
    }
}
