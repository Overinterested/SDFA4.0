package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.executor.Pipeline;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.gwas.PEDFile;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Wenjie Peng
 * @create 2025-05-20 07:49
 * @description select
 */
@Parser(
        usage = "select [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.PedBasedSDFSelectionProgram"),
                @UsageItem(key = "About", value = "Copy the samples within the PED file to output.")
        }
)
public class PedBasedSDFSelectionProgram extends ICommandProgram {
    @Option(names = {"select"}, type = FieldType.NULL, required = true)
    Object select;
    @Option(names = {"-f", "--ped-file"}, type = FieldType.string, required = true)
    String pedFile;
    @Option(names = {"-d", "--sdf-dir"}, type = FieldType.file, required = true)
    File sdfDir;
    @Option(names = {"-o", "--output-dir"}, type = FieldType.string, required = true)
    String outputDir;
    @Option(names = {"--thread", "-t"}, type = FieldType.varInt32)
    int thread = 4;
    @Option(names = {"--count"}, type = FieldType.NULL)
    Object countMode = false;

    public static void main(String[] args) throws IOException {
        Logger logger = LogBackOptions.getRootLogger();
        PedBasedSDFSelectionProgram program = new PedBasedSDFSelectionProgram();
        CommandOptions options = program.parse(args.length == 1 && args[0].equals("merge") ? new String[]{"--help"} : args);
        if (options.isHelp()) {
            logger.info("\n{}", options.usage());
            return;
        } else {
            logger.info("\n{}", options);
        }
        List<File> sdfFileList = FileUtils.listFiles((File) options.value("-d"), file -> FileUtils.getExtension(file).endsWith("sdf"));
        if (sdfFileList == null || sdfFileList.isEmpty()) {
            logger.warn("There is no SDF file in the input directory.");
            return;
        }
        PEDFile pedInstance = PEDFile.load(options.value("-f"));
        HashSet<String> allUidSet = pedInstance.getAllUidSet();
        if (allUidSet.isEmpty()) {
            logger.warn("There is no sample in the input PED file.");
            return;
        }
        String outputDir = options.value("-o");
        Workflow workflow = new Workflow(options.value("-t"));
        workflow.addTask((status, context) -> {
            ProgressBar build = new ProgressBar.Builder()
                    .setTextRenderer("File Copy Speed", "Files")
                    .setInitialMax(Integer.MIN_VALUE)
                    .build();
            context.put("select_bar", build);
        });
        AtomicInteger pedFileCount = new AtomicInteger(0);
        for (int i = 0; i < sdfFileList.size(); i++) {
            int finalI = i;
            workflow.addTask(new Pipeline((status, context) -> {
                File file = sdfFileList.fastGet(finalI);
                SDFReader reader = new SDFReader(file);
                LinkedSet<String> individuals = reader.getIndividuals();
                if (individuals.size() == 1 && allUidSet.contains(individuals.valueOf(0))) {
                    if (!options.passed("--count")) {
                        FileUtils.copy(file.toString(), FileUtils.getSubFile(outputDir, file.getName()));
                    }
                    pedFileCount.incrementAndGet();
                    ((ProgressBar) context.get("select_bar")).step(1);
                }
                reader.closeAll();
                reader = null;
            }));
        }
        workflow.addTask(new Pipeline((status, context) -> {
            ((ProgressBar) context.get("select_bar")).close();
        }));
        workflow.execute();
        if (options.passed("--count")) {
            logger.info("Detect " + pedFileCount.get() + " individuals from the ped file in this input directory.");
        } else {
            logger.info("Finish copy "+pedFileCount.get()+" SDF files from the raw input directory");
        }
    }
}
