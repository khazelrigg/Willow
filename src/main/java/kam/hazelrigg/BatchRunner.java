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
    private static CommandLine cmd;

    private BatchRunner() {
        throw new IllegalStateException("Utility class");
    }

    private static ArrayList<Runner> runners = new ArrayList<>();

    static void passCommandLine(CommandLine commandLine) {
        cmd = commandLine;
    }

    static CommandLine getCommandLine() {
        return cmd;
    }

    static void startRunners(Path path, int threads) throws IOException {
        if (path.toFile().isDirectory()) {
            openDirectory(path);
        } else if (path.toFile().isFile()) {
            openFile(path);
        } else {
            throw new IOException("No such file " + path.toString());
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

    static ArrayList<Runner> getRunners() {
        return runners;
    }

    static void clear() {
        runners = new ArrayList<>();
    }
}