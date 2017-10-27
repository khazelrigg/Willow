package kam.hazelrigg;

import java.io.File;
import java.util.ArrayList;

public class BatchRunner {
    private static ArrayList<Runner> runners = new ArrayList<>();

    /**
     * Start a runners for each file in a directory
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
     * @param directory Directory to open
     */
    private static void openDirectory(File directory) {
        File[] filesInDir = directory.listFiles();

        for (File file : filesInDir != null ? filesInDir : new File[0]) {
            if (file.isDirectory()) {
                // Open subdirectories
                Book.makeResultDirs(file);
                openDirectory(file, file.getName());
            }
            else {
                runners.add(new Runner(file));
            }
        }
    }

    /**
     * Open a subdirectory, creates result folders for subfolders of the directory
     * @param directory Subdirectory to open
     * @param sub Name of the subdirectory
     */
    private static void openDirectory(File directory, String sub) {
        //TODO check if sub is necessary
        File[] filesInDir = directory.listFiles();

        for (File file : filesInDir != null ? filesInDir : new File[0]) {
            if (file.isDirectory()) {
                // Open subdirectories
                Book.makeResultDirs(file);
                openDirectory(file);
            }
            else {
                runners.add(new Runner(file, sub));
            }
        }
    }


}
