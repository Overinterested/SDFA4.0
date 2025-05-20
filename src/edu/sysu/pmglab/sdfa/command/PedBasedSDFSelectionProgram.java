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
import edu.sysu.pmglab.executor.Context;
import edu.sysu.pmglab.executor.ITask;
import edu.sysu.pmglab.executor.Status;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.gwas.PEDFile;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

/**
 * @author Wenjie Peng
 * @create 2025-05-20 07:49
 * @description
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
    @Option(names = {"-d", "--sdf-dir"}, type = FieldType.file, required = true)
    String outputDir;
    @Option(names = {"--thread", "-t"}, type = FieldType.file, required = true, defaultTo = "4")
    int thread;

    public static void main(String[] args) throws IOException {
        Logger logger = LogBackOptions.getRootLogger();
        PedBasedSDFSelectionProgram program = new PedBasedSDFSelectionProgram();
        CommandOptions options = program.parse(args.length == 1 && args[0].equals("merge") ? new String[]{"--help"} : args);
        if (options.isHelp()){
            logger.info("\n{}", options.usage());
            return;
        }else {
            logger.info("\n{}",options);
        }
        List<File> sdfFileList = FileUtils.listFiles((File) options.value("-d"), file -> FileUtils.getExtension(file).endsWith("sdf"));
        if (sdfFileList == null||sdfFileList.isEmpty()){
            logger.warn("There is no SDF file in the input directory.");
            return;
        }
        PEDFile pedInstance = PEDFile.load(options.value("-f"));
        HashSet<String> allUidSet = pedInstance.getAllUidSet();
        if (!allUidSet.isEmpty()){
            logger.warn("There is no sample in the input PED file.");
            return;
        }
        String outputDir = options.value("-o");
        Workflow workflow = new Workflow(options.value("-t"));
        workflow.addTask((status, context) -> {
           context.put("select_bar", new ProgressBar.Builder()
                   .setTextRenderer("File Copy Speed", "Chromosomes")
                   .setInitialMax(Integer.MIN_VALUE)
                   .build());
        });
        for (int i = 0; i < sdfFileList.size(); i++) {
            int finalI = i;
            workflow.addTask((status, context) -> {
                File file = sdfFileList.fastGet(finalI);
                SDFReader reader = new SDFReader(file);
                LinkedSet<String> individuals = reader.getIndividuals();
                if (individuals.size() == 1&& allUidSet.contains(individuals.valueOf(0))){
                    FileUtils.copy(file.toString(), FileUtils.getSubFile(outputDir, file.getName()));
                    ((ProgressBar)context.get("select_bar")).step(1);
                }
                reader.closeAll();
            });
        }
        workflow.addTask((status, context) -> {
            ((ProgressBar)context.get("select_bar")).close();
        });
        workflow.execute();
    }
}
