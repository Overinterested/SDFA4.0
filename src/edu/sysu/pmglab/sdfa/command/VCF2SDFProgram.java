package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.option.OptionBundle;
import edu.sysu.pmglab.commandParser.annotation.rule.Counter;
import edu.sysu.pmglab.commandParser.annotation.rule.Rule;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.commandParser.validator.range.Int_1_RangeValidator;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.executor.Pipeline;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.sdfa.sv.SDSVConversionManager;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVManager;
import edu.sysu.pmglab.sdfa.sv.vcf.SVFilterManager;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInstance;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Wenjie Peng
 * @create 2024-10-04 03:28
 * @description
 */
@Parser(
        usage = "nagf [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.VCF2SDFProgram"),
                @UsageItem(key = "About", value = "Convert VCF to SDF with flexible SV and genotype filter.")
        },
        rule = @Rule(counter = {
                @Counter(item = {"-f", "-dir"}, rule = Counter.Type.EQUAL, count = 1),
        })
)
public class VCF2SDFProgram extends ICommandProgram {
    @Option(names = "vcf2sdf", type = FieldType.NULL)
    Object vcf2sdf;

    @Option(names = {"--threads", "-t"}, type = FieldType.varInt32, defaultTo = "4", validator = Int_1_RangeValidator.class)
    int threads = 4;

    @Option(names = {"--input-dir", "-dir", "-d"}, type = FieldType.file)
    File inputDir;

    @Option(names = {"--input-file", "-f"}, type = FieldType.file)
    File inputFile;

    @Option(names = {"--output-dir", "-o"}, type = FieldType.file)
    File outputDir;

    @Option(names = {"--calling-type", "-ct"}, type = FieldType.string)
    String vcfCalling;

    @OptionBundle()
    SVFilterManager.SVFilterManagerBuilder svFilterManagerBuilder = new SVFilterManager.SVFilterManagerBuilder();

    public static void main(String[] args) throws IOException {

        VCF2SDFProgram vcf2SDFProgram = new VCF2SDFProgram();
        Logger logger = LogBackOptions.getRootLogger();
        CommandOptions options = vcf2SDFProgram.parse(args.length == 1 && args[0].equals("vcf2sdf") ? new String[]{"--help"} : args);
        if (options.isHelp()) {
            logger.info("\n{}", options.usage());
        } else {
            logger.info("\n{}", options);
        }
        SVFilterManager filterManager = vcf2SDFProgram.svFilterManagerBuilder.build();
        if (options.passed("-f")) {
            long l = System.currentTimeMillis();
            VCFInstance vcfInstance = new VCFInstance(LiveFile.of(vcf2SDFProgram.inputFile), vcf2SDFProgram.vcfCalling);
            vcfInstance.setSVFilterManager(filterManager);
            SDSVConversionManager sdsvConversionManager = new SDSVConversionManager();
            File tmpStorage = FileUtils.getSubFile(vcf2SDFProgram.outputDir, UUID.randomUUID().toString());
            sdsvConversionManager.initWriter(tmpStorage);
            vcfInstance.setConversionFromSV2Record(sdsvConversionManager);
            logger.info("Start single SV file conversion task.");
            vcfInstance.parse();
            File trueStorage = FileUtils.getSubFile(vcf2SDFProgram.outputDir, vcf2SDFProgram.inputFile.getName() + ".sdf");
            if (trueStorage.exists()) {
                logger.info("Take " + (System.currentTimeMillis() - l) / 1000f + " seconds to convert input file from vcf to sdf ");
                logger.warn("Converted SDF file is stored at " + tmpStorage);
            } else {
                boolean changeName = tmpStorage.renameTo(trueStorage);
                logger.info("Take " + (System.currentTimeMillis() - l) / 1000f + "seconds to convert input file from vcf to sdf ");
                if (!changeName) {
                    logger.warn("Converted SDF file is stored at " + tmpStorage);
                } else {
                    logger.info("Converted SDF file is stored at " + trueStorage);
                }
            }
            return;
        }
        if (options.passed("-dir")) {
            Workflow workflow = new Workflow(vcf2SDFProgram.threads);
            List<Pipeline> pipelines = SDSVManager.of(vcf2SDFProgram.inputDir)
                    .setFilterManager(filterManager)
                    .setOutputDir(vcf2SDFProgram.outputDir)
                    .setCallingType(vcf2SDFProgram.vcfCalling)
                    .parseToSDFFileTask();
            for (Pipeline pipeline : pipelines) {
                workflow.addTask(pipeline);
            }
            workflow.execute();
            workflow.clearTasks();
        }
    }
}
