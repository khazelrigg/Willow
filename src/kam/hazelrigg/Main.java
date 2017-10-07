package kam.hazelrigg;

import java.io.File;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        File file = new File(getFileName());
        long startTime = System.currentTimeMillis();

        if (file.isDirectory()) {
            readDir(file);
        } else if (file.isFile()) {
            readFile(file);
        }

        long endTime = System.currentTimeMillis();

        System.out.println("\nTotal time elapsed " + ((endTime - startTime) / 1000) + " sec.");

    }

    /**
     * Get a file/directory name from the user and ensure it is valid
     *
     * @return String containing the input if the input is a file/directory
     */
    private static String getFileName() {
        // Get a filename and check that the file exists

        Scanner kb = new Scanner(System.in);

        // Keep asking for input path until a valid one is found.
        while (true) {
            System.out.print("File path: ");
            String input = kb.nextLine();
            File file = new File(input);

            // If the file exists it is a valid input
            if (file.exists()) {
                return input;
            } else {
                System.out.println("Try again, no file found at " + input);
            }
        }
    }

    /**
     * Reads and creates the data for an entire directory
     *
     * @param dir Directory to analyse
     */
    private static void readDir(File dir) {
        // Create an array of files containing every file in the directory
        File[] files = dir.listFiles();

        // getFileName ensures files is not null
        assert files != null;

        for (File file : files) {
            // Call readDir recursively to read sub directories
            if (file.isDirectory()) {
                readDir(file);
            } else {
                readFile(file);
            }
        }
    }

    /**
     * Reads and creates the data for an input file
     *
     * @param file File to analyse
     */
    private static void readFile(File file) {
        Book book1 = new Book();
        book1.setTitleFromText(file);
        // If we already have results there is no need to redo results
        if (book1.resultsFileExists()) {
            System.out.println("☑ - " + file.getName() + " already has results.");

        } else {
            book1.setPath(file);
            book1.analyseText();
            book1.writeFrequencies();
        }

    }

    /*
    private static void makeGraph(HashMap<String, Integer> map, File out, String purpose) {

        if (verbose) {
            System.out.println("[*] Creating graph for " + out.getName());
        }

        DefaultPieDataset dataSet = new DefaultPieDataset();

        // Load POS data into data set
        for (String type : map.keySet()) {
            dataSet.setValue(type, map.get(type));
        }

        String title = purpose + " of " +
                out.getName().substring(0, out.getName().lastIndexOf("Results"));

        JFreeChart chart = ChartFactory.createPieChart3D(
                title,
                dataSet,
                false,
                true,
                false);

        PiePlot3D plot = (PiePlot3D) chart.getPlot();

        plot.setBaseSectionOutlinePaint(new Color(0, 0, 0));
        plot.setDarkerSides(true);
        plot.setBackgroundPaint(new Color(204, 204, 204));
        plot.setLabelBackgroundPaint(new Color(255, 255, 255));
        plot.setStartAngle(90f);
        plot.setLabelFont(new Font("Ubuntu San Serif", Font.PLAIN, 10));
        plot.setDepthFactor(0.05f);

        try {
            ChartUtilities.saveChartAsJPEG(new File("results/img/" + out.getName()), chart, 900, 900);
        } catch (IOException ioe) {
            System.out.println("[Error - makeGraph] Failed to make pie chart " + ioe);
        }
    }
    */


}
