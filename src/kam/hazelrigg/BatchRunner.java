package kam.hazelrigg;

import java.io.File;
import java.util.ArrayList;

public class BatchRunner {
    static ArrayList<Runner> runners = new ArrayList<>();

    public static void startRunners(File file) {
        openDirectory(file);

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
                Book.makeResultDirs(file);
                openDirectory(file);
            }
            else {
                runners.add(new Runner(file));
            }
        }

    }

}
