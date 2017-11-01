package kam.hazelrigg;

import java.io.File;
import java.util.Scanner;

public class WordCount {

    public static void main(String[] args) {
        File path;
        if (args.length != 0 && new File(args[0]).exists()) {
            path = new File(args[0]);
        } else {
            path = new File(getFileName());
        }

        if (path.isDirectory()) {
            BatchRunner.startRunners(path);
        } else {
            Book book = new Book();
            book.setTitleFromText(path);
            book.setPath(path);
            book.analyseText();
            book.writeText();
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

}
