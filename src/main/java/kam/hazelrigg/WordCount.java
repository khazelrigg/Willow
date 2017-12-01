package kam.hazelrigg;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.io.File;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

public class WordCount {
    public static Pattern p = Pattern.compile("[aeiouy]+[^$e(,.:;!?)]");
    static final long startTime = System.currentTimeMillis();
    static StanfordCoreNLP pipeline;
    static File path;

    public static void main(String[] args) {

        // Set up CoreNlp pipeline
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        pipeline = new StanfordCoreNLP(props);

        if (args.length != 0 && new File(args[0]).exists()) {
            path = new File(args[0]);
        } else {
            path = new File(getFileName());
        }

        if (path.isDirectory()) {
            BatchRunner.startRunners(path);
        } else if (path.isFile()) {
            Book book = new Book();
            book.setTitleFromText(path);
            book.setPath(path);
            book.readText();

            new OutputWriter(book).writeTxt();
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
