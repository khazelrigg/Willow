package kam.hazelrigg;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.util.Properties;
import java.util.regex.Pattern;

public class Willow {
    public static Pattern p = Pattern.compile("[aeiouy]+[^$e(,.:;!?)]");
    static final long startTime = System.currentTimeMillis();
    static StanfordCoreNLP pipeline;

    /**
     * Run main program
     *
     * @param args Path to file/directory
     */
    public static void main(String[] args) {
        File path = null;

        // Set up command line args
        Options options = new Options();
        options.addOption("h", "help", false, "Print help")
                .addOption("v", "verbose", false, "Verbose output")
                //.addOption("w", "words", false, "Only analyse a text for word counts, cannot be used with -i or -j")
                .addOption("e", "economy", false, "Run in economy mode, greatly reduces memory usage at the cost of completion speed. Useful for computers with less memory")
                .addOption("i", "images", false, "Create image outputs")
                .addOption("j", "json", false, "Create JSON output")
                .addOption("c", "csv", false, "Create CSV output")
                .addOption("o", "overwrite", false, "Overwrite any existing results")
                .addOption("t", "threads", true, "Max number of threads to run, 0 = Use number of CPUs available; default = 0");

        HelpFormatter formatter = new HelpFormatter();

        // Check for passed options
        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            if (cmd.getOptions().length == 0 && cmd.getArgs().length == 0) {
                formatter.printHelp("Willow [OPTIONS] [FILE]", "Acceptable file types: Plain text and pdf", options, "");
                System.exit(-1);
            }

            BatchRunner.passOptions(options);

            if (cmd.getArgs().length == 1) {
                path = new File(cmd.getArgs()[0]);
            } else {
                formatter.printHelp("Willow [OPTIONS] [FILE]", "Acceptable file types: Plain text and pdf", options, "");
                System.exit(-1);
            }

            pipeline = createPipeline();

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

    private static StanfordCoreNLP createPipeline() {
        Properties properties = new Properties();
        properties.put("annotators",
                "tokenize, ssplit, pos, lemma");
        properties.put("tokenize.options", "untokenizable=noneDelete");

        return new StanfordCoreNLP(properties);
    }

}
