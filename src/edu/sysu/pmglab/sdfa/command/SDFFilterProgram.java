package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.option.OptionBundle;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.runtimecompiler.JavaCompiler;
import edu.sysu.pmglab.sdfa.SDFFilter;
import edu.sysu.pmglab.sdfa.SDFReader;

import java.io.File;

/**
 * @author Wenjie Peng
 * @create 2025-03-12 02:09
 * @description
 */
@Parser(
        usage = "filter [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.SDFFilterProgram"),
                @UsageItem(key = "About", value = "Filter the SDF file using SV filter and GT filter.")
        }
)
public class SDFFilterProgram extends ICommandProgram {
    @Option(names = "filter", type = FieldType.NULL)
    Object filter;

    @Option(names = {"--thread", "-t"}, type = FieldType.varInt32)
    int thread = 4;

    @Option(names = {"--dir", "-d"}, type = FieldType.string, required = true)
    String inputDir;

    @Option(names = {"--output-dir", "-o"}, type = FieldType.string, required = true)
    String outputDir;

    @OptionBundle
    SDFFilter.SDFFilterBuilder filterBuilder = new SDFFilter.SDFFilterBuilder();
    static {
        JavaCompiler.importClass(Bytes.class);
    }
    public static void main(String[] args) {
        Logger logger = LogBackOptions.getRootLogger();
        SDFFilterProgram sdfFilterProgram = new SDFFilterProgram();
        CommandOptions options = sdfFilterProgram.parse(args.length == 1 && args[0].equals("filter") ? new String[]{"--help"} : args);
        if (options.isHelp()){
            logger.info("\n{}", options.usage());
        }else {
            logger.info("\n{}",options);
        }
        File[] files = new File(sdfFilterProgram.inputDir).listFiles(file -> file.getName().endsWith(".sdf"));
        if (files.length == 0) {
            logger.warn("There is no SDF file in input directory.");
            return;
        }
        Workflow workflow = new Workflow(sdfFilterProgram.thread);
        logger.info("Start filter process");
        ProgressBar bar = new ProgressBar.Builder().setTextRenderer("Parse speed", "files")
                .setInitialMax(files.length)
                .build();
        sdfFilterProgram.filterBuilder.build();
        for (int i = 0; i < files.length; i++) {
            int finalI = i;
            workflow.addTask(((status, context) -> {
                File file = files[finalI];
                String outputFile = FileUtils.getSubFile(sdfFilterProgram.outputDir, file.getName());

                SDFReader reader = new SDFReader(file);
                CCFWriter writer = CCFWriter.setOutput(new File(outputFile)).addFields(reader.getRawFields()).instance();
                SDFFilter sdfFilter = new SDFFilter(reader, sdfFilterProgram.filterBuilder);
                IRecord record;
                while ((record = reader.readRecord()) != null) {
                    record = sdfFilter.filter(record);
                    // record could be null when filter the SV
                    if (record != null){
                        writer.write(record);
                    }
                }
                reader.close();
                writer.addMeta(reader.getReaderOption().getSDFTable().getMeta());
                writer.close();
                bar.step(1);
            }));
        }
        workflow.execute();
        workflow.clearTasks();
        bar.close();
        logger.info("Finish filter process.");
    }

}
