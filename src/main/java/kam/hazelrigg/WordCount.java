package kam.hazelrigg;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

public class WordCount {
    public static Pattern p = Pattern.compile("[aeiouy]+[^$e(,.:;!?)]");
    static final long startTime = System.currentTimeMillis();
    static StanfordCoreNLP pipeline;
    private static HashMap<String, Boolean> chosenOptions = new HashMap<>();

    public static void main(String[] args) {
        File path = null;

        // Set up command line args
        Options options = new Options();
        options.addOption("h", "help", false, "Print help")
                .addOption("v", "verbose", false, "Verbose output")
                //.addOption("w", "words", false, "Only analyse a text for word counts, cannot be used with -i or -j")
                .addOption("e", "economy", false, "Run in economy mode, greatly reduces memory usage at the cost of completion speed. Useful for computers with less memory")
                .addOption("k", "interactive", false, "Run interactive mode, choose options when run instead of in command line")
                .addOption("i", "images", false, "Create image outputs")
                .addOption("j", "json", false, "Create JSON output")
                .addOption("c", "csv", false, "Create CSV output")
                .addOption("o", "overwrite", false, "Overwrite any existing results")
                .addOption("t", "threads", true, "Max number of threads to run, 0 = Use CPUs available; default = 0");


        HelpFormatter formatter = new HelpFormatter();

        // Check for passed options
        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            if (cmd.getOptions().length == 0 && cmd.getArgs().length == 0) {
                formatter.printHelp("wordCount [OPTIONS] [FILE]", "Acceptable file types: Plain text and pdf", options, "");
                System.exit(-1);
            }

            for (Option option : options.getOptions()) {
                chosenOptions.put(option.getOpt(), cmd.hasOption(option.getOpt()));
            }

            BatchRunner.passOptions(chosenOptions);

            if (cmd.getArgs().length == 1) {
                path = new File(cmd.getArgs()[0]);
            } else if (cmd.hasOption("k")) {
                path = runInteractive();
                BatchRunner.passOptions(chosenOptions);
            } else {
                formatter.printHelp("wordCount [OPTIONS] [FILE]", "Acceptable file types: Plain text and pdf", options, "");
                //formatter.printHelp("wordCount [OPTION]... [FILE]...", options);
                System.exit(-1);
            }

            // Set up CoreNlp pipeline after ensuring program will be run
            Properties properties = new Properties();
            properties.put("annotators",
                    "tokenize, ssplit, pos, lemma, depparse, natlog, openie");
            properties.put("tokenize.options", "untokenizable=noneDelete");

            pipeline = new StanfordCoreNLP(properties);

            if (cmd.hasOption("threads") && !cmd.getOptionValue("threads").equals("0")) {
                startRunners(path, Integer.parseInt(cmd.getOptionValue("threads")));
            } else {
                int threads = Runtime.getRuntime().availableProcessors();
                startRunners(path, threads + 1);
            }

        } catch (ParseException e) {
            System.out.println("[Error - main] Error parsing command line options");
            e.printStackTrace();
        }

    }

    private static void startRunners(File path, int threads) {
        BatchRunner.startRunners(path, threads);
    }

    private static File runInteractive() {
        chosenOptions.put("i", getYesNo("Create image outputs"));
        chosenOptions.put("j", getYesNo("Create JSON output"));
        chosenOptions.put("e", getYesNo("Run in economy mode to save memory"));
        return getFileName();
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
    private static File getFileName() {
        // Get a filename and check that the file exists
        Scanner kb = new Scanner(System.in);

        // Keep asking for input path until a valid one is found.
        while (true) {
            System.out.print("File path: ");
            String input = kb.nextLine();
            File file = new File(input);

            // If the file exists it is a valid input
            if (file.exists()) {
                return file;
            } else {
                System.out.println("Try again, no file found at " + input);
            }
        }
    }

}
