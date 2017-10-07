package kam.hazelrigg;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        File path = new File(getFileName());

        if (path.isDirectory()) {
            File[] files = path.listFiles();

            ArrayList<Runner> runners = new ArrayList<>();

            if (files != null) {
                for (File file : files) {
                    runners.add(new Runner(file));
                }
            }

            for (Runner runner : runners) {
                runner.thread.start();

                while (runner.running) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        } else {
            Book book = new Book();
            book.setTitleFromText(path);
            book.setPath(path);
            book.analyseText();
            book.writeFrequencies();
        }

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
