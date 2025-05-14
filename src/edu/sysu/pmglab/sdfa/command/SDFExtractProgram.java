package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.executor.Pipeline;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.toolkit.SDFExtract;

import java.io.File;

/**
 * @author Wenjie Peng
 * @create 2025-03-12 06:31
 * @description
 */
@Parser(
        usage = "extract [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.SDFExtractProgram"),
                @UsageItem(key = "About", value = "Extract specific samples from a PED format file.")
        }
)
public class SDFExtractProgram extends ICommandProgram {
    @Option(names = "extract", type = FieldType.NULL)
    Object extract;

    @Option(names = {"--thread", "-t"}, type = FieldType.varInt32)
    int thread = 4;

    @Option(names = {"--max-maf"}, type = FieldType.float32)
    float maxMaf = -1;

    @Option(names = {"--min-maf"}, type = FieldType.float32)
    float minMaf = -1;

    @Option(names = {"--ped-file", "--ped"}, type = FieldType.string, required = true)
    String pedFile;

    @Option(names = {"--dir", "-d"}, type = FieldType.string, required = true)
    String inputSDFDir;

    @Option(names = {"--output-dir", "-o", "--output"}, type = FieldType.string, required = true)
    String outputSDFDir;

    public static void main(String[] args) {
        SDFExtractProgram sdfExtractProgram = new SDFExtractProgram();
        Logger logger = LogBackOptions.getRootLogger();
        CommandOptions options = sdfExtractProgram.parse(args.length == 1 && args[0].equals("extract") ? new String[]{"--help"} : args);
        if (options.isHelp()) {
            logger.info("\n{}", options.usage());
            return;
        } else {
            logger.info("\n{}", options);
        }

        File[] files = new File(sdfExtractProgram.inputSDFDir).listFiles((file) -> file.getName().endsWith(".sdf"));
        if (files == null || files.length == 0) {
            logger.warn("There is no SDF file in input directory.");
            return;
        }
        logger.info("Start extraction process from files.");
        ProgressBar.Builder builder = new ProgressBar.Builder().setTextRenderer("Parse speed", "files")
                .setInitialMax(files.length);

        Workflow workflow = new Workflow(sdfExtractProgram.thread);
        workflow.addTask(new Pipeline((status, context) -> {
            ProgressBar bar = builder.build();
            context.put(ProgressBar.class, bar);
        }));
        workflow.addTask(Pipeline.WAIT_FOR_ALL);
        for (File file : files) {
            workflow.addTask((status, context) -> {
                SDFExtract.of(file.toString(),
                                sdfExtractProgram.pedFile,
                                FileUtils.getSubFile(sdfExtractProgram.outputSDFDir, file.getName()))
                        .setMaxMAF(sdfExtractProgram.maxMaf)
                        .setMinMAF(sdfExtractProgram.minMaf)
                        .submit();
                ProgressBar bar = (ProgressBar) context.get(ProgressBar.class);
                bar.step(1);
            });
        }
        workflow.addTask(Pipeline.WAIT_FOR_ALL);
        workflow.execute();
        workflow.clearTasks();
        workflow.addTask(new Pipeline((status, context) -> {
            ProgressBar bar = (ProgressBar) context.get(ProgressBar.class);
            bar.close();
        }));
        workflow.execute();
        workflow.clearTasks();
        logger.info("Finish extraction process");
        workflow.clearTasks();
    }
}
