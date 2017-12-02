package kam.hazelrigg;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

public class WordCount {
    public static Pattern p = Pattern.compile("[aeiouy]+[^$e(,.:;!?)]");
    static final long startTime = System.currentTimeMillis();
    static StanfordCoreNLP pipeline;

    public static void main(String[] args) {
        File path = null;

        // Set up command line args
        Options options = new Options();
        options.addOption("h", "help", false, "Print help")
                .addOption("k", "interactive", false, "Run interactive mode, choose options when run instead of in command line")
                .addOption("i", "images", false, "Create image outputs")
                .addOption("j", "json", false, "Create JSON output")
                .addOption("o", "overwrite", false, "Overwrite any results");

        HelpFormatter formatter = new HelpFormatter();

        // Check for passed options
        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            if (cmd.getOptions().length == 0 && cmd.getArgs().length == 0) {
                formatter.printHelp("wordCount [OPTION]... [FILE]...", options);
                System.exit(-1);
            }

            if (cmd.hasOption("i")) {
                BatchRunner.createImg = true;
            }
            if (cmd.hasOption("j")) {
                BatchRunner.createJson = true;
            }
            if (cmd.hasOption("o")) {
                BatchRunner.overwrite = true;
            }

            if (cmd.getArgs().length == 1) {
                path = new File(cmd.getArgs()[0]);
            } else if (cmd.hasOption("k")) {
                path = runInteractive();
            } else {
                formatter.printHelp("wordCount [OPTION]... [FILE]...", options);
                System.exit(-1);
            }

            // Set up CoreNlp pipeline
            Properties props = new Properties();
            props.put("annotators", "tokenize, ssplit, pos, lemma");
            pipeline = new StanfordCoreNLP(props);

            start(path);
        } catch (ParseException e) {
            System.out.println("[Error - main] Error parsing command line options");
            e.printStackTrace();
        }

    }

    private static void start(File path) {
        BatchRunner.startRunners(path);
    }


    private static File runInteractive() {
        BatchRunner.createImg = getYesNo("Create image outputs");
        BatchRunner.createJson = getYesNo("Create JSON output");
        return new File(getFileName());
    }

    /**
     * Ask the user a yes or no question
     *
     * @param msg Question to ask, doesn't need question mark
     * @return true if user answers yes, false if they answer no
     */
    private static boolean getYesNo(String msg) {
        Scanner kb = new Scanner(System.in);
        while (true) {
            System.out.print(msg + " [y/n] ");
            String ans = kb.nextLine().toLowerCase().trim();

            if (ans.equals("y") || ans.equals("yes")) {
                return true;
            } else if (ans.equals("n") || ans.equals("no")) {
                return false;
            }
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
