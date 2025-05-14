package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.sdfa.toolkit.SDFConcat;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-03-12 06:18
 * @description
 */
@Parser(
        usage = "concat [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.SDFConcatProgram"),
                @UsageItem(key = "About", value = "Concat multiple SDF files.")
        }
)
public class SDFConcatProgram extends ICommandProgram {
    @Option(names = "concat", type = FieldType.NULL)
    Object concat;
    @Option(names = {"--thread", "-t"}, type = FieldType.varInt32)
    int thread = 4;
    @Option(names = {"--dir", "-d"}, type = FieldType.string, required = true)
    String inputDir;
    @Option(names = {"--output-dir", "-o"}, type = FieldType.string, required = true)
    String outputDir;

    public static void main(String[] args) throws IOException {
        SDFConcatProgram sdfConcatProgram = new SDFConcatProgram();
        Logger logger = LogBackOptions.getRootLogger();
        CommandOptions options = sdfConcatProgram.parse(args.length == 1 && args[0].equals("concat") ? new String[]{"--help"} : args);
        if (options.isHelp()) {
            logger.info("\n{}", options.usage());
        } else {
            logger.info("\n{}", options);
        }
        new SDFConcat(sdfConcatProgram.inputDir, sdfConcatProgram.outputDir)
                .submitTo(new Workflow(sdfConcatProgram.thread));
    }
}
