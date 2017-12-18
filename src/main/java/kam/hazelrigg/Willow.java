package kam.hazelrigg;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Willow {
    static final long START_TIME = System.currentTimeMillis();
    static StanfordCoreNLP pipeline;
    private static final Options options = createOptions();
    private static Logger logger = LoggerFactory.getLogger(Willow.class);
    private static int threads = 0;

    public static void main(String[] args) {
        Thread.currentThread().setName("Willow");

        Path filePath;
        try {
            CommandLine cmd = getCommandLine(args);
            if (commandLineOptionsAreEmpty(cmd) && commandLineArgIsEmpty(cmd)) {
                printHelp();
            }

            filePath = Paths.get(cmd.getArgs()[0]);
            pipeline = createPipeline();
            BatchRunner.passCommandLine(cmd);


            if (cmd.hasOption("t")) {
                threads = Integer.parseInt(cmd.getOptionValue("t"));
            }
            startRunners(filePath, threads);
        } catch (ParseException e) {
            logger.error("Parse exception for {}", e.toString());
        }
    }

    private static void startRunners(Path path, int threadArg) {
        if (threadArg == 0) {
            threadArg = Runtime.getRuntime().availableProcessors();
        }
        try {
            BatchRunner.startRunners(path, threadArg);
        } catch (IOException e) {
            logger.error("No such file {}", path);
        }
    }


    private static Options createOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "Print help")
                .addOption("v", "verbose", false, "Verbose output")
                .addOption("e", "economy", false,
                        "Run in economy mode, reduces memory usage at the cost "
                                + "of completion speed. Useful for computers with less memory")
                .addOption("i", "images", false,
                        "Create image outputs")
                .addOption("j", "json", false, "Create JSON output")
                .addOption("c", "csv", false, "Create CSV output")
                .addOption("o", "overwrite", false,
                        "Overwrite any existing results")
                .addOption("t", "threads", true,
                        "Max number of threads to run in pool, 0 = Use number of CPUs available;"
                                + " default = 0");
        return options;
    }

    private static StanfordCoreNLP createPipeline() {
        Properties properties = new Properties();
        properties.put("annotators", "tokenize, ssplit, pos, lemma");
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
        throw new IllegalArgumentException("No args");
    }

    public static StanfordCoreNLP getPipeline() {
        return pipeline;
    }

    public static Logger getLogger() {
        return logger;
    }

    static CommandLine getCommandLine(String[] args) throws ParseException {
        return new DefaultParser().parse(options, args);
    }
}
