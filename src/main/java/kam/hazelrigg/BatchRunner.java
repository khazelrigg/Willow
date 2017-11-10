package main.java.kam.hazelrigg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class BatchRunner {
    private static ArrayList<Runner> runners = new ArrayList<>();

    /**
     * Start a runners for each file in a directory
     *
     * @param directory Directory to open
     */
    public static void startRunners(File directory) {
        openDirectory(directory);

        for (Runner runner : runners) {
            runner.start();
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
