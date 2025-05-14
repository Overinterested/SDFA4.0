package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.option.OptionBundle;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.commandParser.validator.range.Int_1_RangeValidator;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.executor.ITask;
import edu.sysu.pmglab.executor.Pipeline;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.merge.MergeManager;
import edu.sysu.pmglab.sdfa.merge.output.GlobalMergeResultWriter;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVManager;
import edu.sysu.pmglab.sdfa.sv.vcf.SVFilterManager;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * @author Wenjie Peng
 * @create 2024-09-14 22:16
 * @description
 */
@Parser(
        usage = "merge [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.MergeProgram"),
                @UsageItem(key = "About", value = "Merge multiple individual SV files into a population VCF file.")
        }
)
public class MergeProgram extends ICommandProgram {
    @Option(names = "merge", type = FieldType.NULL)
    Object merge;

    @Option(names = {"-t", "--threads"}, type = FieldType.varInt32, validator = Int_1_RangeValidator.class)
    int threads = 4;

    @Option(names = {"-dir", "-d"}, type = FieldType.file, required = true)
    File inputDir;

    @Option(names = {"-o", "--output"}, type = FieldType.file, required = true)
    File outputDir;

    @OptionBundle
    SVFilterManager.SVFilterManagerBuilder builder = new SVFilterManager.SVFilterManagerBuilder();

    public static void main(String[] args) throws IOException {
        MergeProgram mergeProgram = new MergeProgram();
        Logger logger = LogBackOptions.getRootLogger();
        CommandOptions options = mergeProgram.parse(args.length == 1 && args[0].equals("merge") ? new String[]{"--help"} : args);
        if (options.isHelp()){
            logger.info("\n{}", options.usage());
        }else {
            logger.info("\n{}",options);
        }
        // init parameters
        int threads = mergeProgram.threads;
        File inputDir = mergeProgram.inputDir;
        File outputDir = mergeProgram.outputDir;

        // prepare sdf files
        Workflow workflow = new Workflow(threads);
        SDSVManager sdsvManager = Objects.requireNonNull(SDSVManager.of(inputDir))
                .setOutputDir(outputDir)
                .setFilterManager(mergeProgram.builder.build())
                .setReadOption(SDFReadType.MERGE);
        List<Pipeline> pipelines = sdsvManager.parseToSDFFileTask();
        for (Pipeline pipeline : pipelines) {
            workflow.addTask(pipeline);
        }
        workflow.execute();
        workflow.clearTasks();
        // integrate multiple contig
        logger.info("Start to merge SVs among chromosomes");
        ProgressBar bar = new ProgressBar.Builder()
                .setTextRenderer("SV Merge Speed", "Chromosomes")
                .setInitialMax(Integer.MIN_VALUE)
                .build();

        // region add new merge methods
//        TwoSSVMerger.addSSVMerge(SVTypeSign.getStandardizedName("INS"), (var1, var2) -> false);
        // endregion

        MergeManager mergeManager = MergeManager.init(threads, outputDir).setReaderMode(SDFReadType.MERGE);
        mergeManager.collectValidContigList();

        // indexed merge
        int index = 0;
        while (true) {
            int startContigIndex = index * threads;
            int endContigIndex = (index + 1) * threads;
            IntInterval checkedRange = mergeManager.check(startContigIndex, endContigIndex);
            if (checkedRange == null) {
                // merging process of simple svs from all contig names are complete
                workflow.addTask(mergeManager.mergeCSVTask());
                workflow.execute();
                workflow.clearTasks();
                break;
            }
            endContigIndex = checkedRange.end();
            List<ITask> tasks = mergeManager.mergeSSVTasks(startContigIndex, endContigIndex);
            for (ITask task : tasks) {
                workflow.addTask(task);
            }
            workflow.execute();
            workflow.clearTasks();
            workflow.addTask(mergeManager.mergeCSVTask());
            workflow.execute();
            workflow.clearTasks();
            index++;
            bar.step(endContigIndex - startContigIndex);
        }
        bar.close();
        GlobalMergeResultWriter globalMergeResultWriter = GlobalMergeResultWriter.getInstance();
        boolean success = globalMergeResultWriter.closeAndRename();
        File outputFilePath = globalMergeResultWriter.getOutputFilePath();
        if (success) {
            LogBackOptions.getRootLogger().info("Merge result is stored at " + outputFilePath);
        } else {
            LogBackOptions.getRootLogger().warn("Merge result is stored at " + outputFilePath);
        }
    }


}
