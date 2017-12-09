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
    private static Options options = createOptions();

    public static void main(String[] args) {
        Thread.currentThread().setName("Willow");

        File path = null;
        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            if (commandLineOptionsAreEmpty(cmd) && commandLineArgIsEmpty(cmd)) {
                printHelp();
            }

            if (!commandLineArgIsEmpty(cmd)) {
                path = new File(cmd.getArgs()[0]);
            }

            pipeline = createPipeline();
            BatchRunner.passCommandLine(cmd);

            if (cmd.hasOption("threads")) {
                int threadArg = Integer.parseInt(cmd.getOptionValue("threads"));
                startRunners(path, threadArg);
            } else {
                startRunners(path, 0);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    private static void startRunners(File path, int threads) {
        if (threads == 0) {
            threads = Runtime.getRuntime().availableProcessors();
        }
        BatchRunner.startRunners(path, threads);
    }


    private static Options createOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "Print help")
                .addOption("v", "verbose", false, "Verbose output")
                .addOption("e", "economy", false,
                        "Run in economy mode, greatly reduces memory usage at the cost "
                                + "of completion speed. Useful for computers with less memory")
                .addOption("i", "images", false,
                        "Create image outputs")
                .addOption("j", "json", false, "Create JSON output")
                .addOption("c", "csv", false, "Create CSV output")
                .addOption("o", "overwrite", false,
                        "Overwrite any existing results")
                .addOption("t", "threads", true,
                        "Max number of threads to run, 0 = Use number of CPUs available;"
                                + " default = 0");
        return options;
    }

    private static StanfordCoreNLP createPipeline() {
        Properties properties = new Properties();
        properties.put("annotators",
                "tokenize, ssplit, pos, lemma");
        properties.put("tokenize.options", "untokenizable=noneDelete");

        return new StanfordCoreNLP(properties);
    }

    private static boolean commandLineOptionsAreEmpty(CommandLine commandLine) {
        return commandLine.getOptions().length == 0;
    }

    private static boolean commandLineArgIsEmpty(CommandLine commandLine) {
        return commandLine.getArgs().length == 0;
    }

    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Willow [OPTIONS] [FILE]",
                "Acceptable file types: Plain text and pdf", options, "");
        System.exit(-1);
    }

}
