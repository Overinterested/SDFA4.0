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
import edu.sysu.pmglab.easytools.container.task.SimpleShellTask;
import edu.sysu.pmglab.executor.*;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVManager;
import edu.sysu.pmglab.sdfa.sv.vcf.SVFilterManager;
import edu.sysu.pmglab.sdfa.toolkit.SDF2Plink;
import edu.sysu.pmglab.sdfa.toolkit.SDFConcat;
import edu.sysu.pmglab.utils.ShellUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-10-23 02:43
 * @description
 */
@Parser(
        usage = "gwas [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.SVBasedGWASProgram"),
                @UsageItem(key = "About", value = "Offer a complete SV-based GWAS analyses pipeline, including vcf2sdf, concatenating files, sdf2plink and built-in plink program.")
        },
        rule = @Rule(
                counter = {
                        @Counter(item = {"-d", "-f"},
                                rule = Counter.Type.EQUAL, count = 1)
                }
        )
)
public class SVBasedGWASProgram extends ICommandProgram {
    @Option(names = "gwas", type = FieldType.NULL)
    Object gwas;

    @Option(names = "--concat-files", type = FieldType.NULL)
    boolean concat;

    @Option(names = "--ped-file", type = FieldType.file)
    File pedFile;

    @Option(names = {"-dir", "-d"}, type = FieldType.file)
    File inputDir;

    @Option(names = {"-o", "--output"}, type = FieldType.file, required = true)
    File outputDir;

    @Option(names = {"-f", "--input-file"}, type = FieldType.file)
    File inputFile;

    @Option(names = {"-t", "--threads"}, type = FieldType.varInt32, validator = Int_1_RangeValidator.class, defaultTo = "4")
    int threads = 4;

    @Option(names = {"--calling-type", "-ct"}, type = FieldType.string)
    String vcfCalling;

    // plink
    @Option(names = {"--plink-shell", "--plink-command"}, type = FieldType.livefile)
    LiveFile plinkShell;

    // filter
    @OptionBundle
    SVFilterManager.SVFilterManagerBuilder builder = new SVFilterManager.SVFilterManagerBuilder();

    public static void main(String[] args) throws IOException {
        SVBasedGWASProgram program = new SVBasedGWASProgram();
        Logger logger = LogBackOptions.getRootLogger();
        CommandOptions options = program.parse(args.length == 1 && args[0].equals("gwas") ? new String[]{"--help"} : args);
        if (options.isHelp()){
            logger.info("\n{}", options.usage());
        }else {
            logger.info("\n{}",options);
        }
        if (options.passed("-f")) {
            handleOneSDFFile(program, program.inputFile);
            return;
        }

        if (options.passed("-dir")) {
            Workflow workflow = new Workflow(program.threads);
            SVFilterManager filterManager = program.builder.build();

            // load reader
            SDSVManager sdsvManager = SDSVManager.of(program.inputDir)
                    .setFilterManager(filterManager)
                    .setOutputDir(program.outputDir)
                    .setCallingType(program.vcfCalling);
            List<Pipeline> pipelines = sdsvManager.parseToSDFFileTask();
            for (Pipeline pipeline : pipelines) {
                workflow.addTask(pipeline);
            }
            workflow.execute();
            workflow.clearTasks();

            // concat readers
            LiveFile outputPath;
            if (sdsvManager.numOfFileSize() != 1) {
                SDFConcat concat = new SDFConcat(program.outputDir, program.outputDir).setLogger(LogBackOptions.getRootLogger());
                concat.submitTo(workflow);
                workflow.clearTasks();
                outputPath = LiveFile.of(concat.getOutputPath());
            } else {
                outputPath = sdsvManager.getByIndex(0).getReader().getFile();
            }
            // handle the concat file
            handleOneSDFFile(program, new File(outputPath.getPath()));
        }
    }

    private static void handleOneSDFFile(SVBasedGWASProgram program, File file) throws IOException {
        Logger logger = LogBackOptions.getRootLogger();
        String extension = FileUtils.getExtension(file);
        if (!extension.equals("sdf")) {
            try {
                String parseCommand = "-f " + file + " -o " + program.outputDir;
                if (program.vcfCalling != null) {
                    parseCommand += " --calling-type " + program.vcfCalling;
                }
                VCF2SDFProgram.main(parseCommand.split(" "));
                file = FileUtils.getSubFile(program.outputDir, file.getName() + ".sdf");
            } catch (Exception | Error e) {
                logger.error("The single input file must be sdf or vcf file format.");
                e.printStackTrace();
                return;
            }
        }
        String name = file.getName().replace("." + FileUtils.getExtension(file), "");
        SDF2Plink sdf2Plink = SDF2Plink.of(file, program.outputDir)
                .setPedFile(program.pedFile)
                .setOutputName(name);
        logger.info("Start converting sdf file to plink file.");
        sdf2Plink.submit();
        logger.info("Start executing plink program.");
        if (program.plinkShell != null) {
            String commands;
            try (ReaderStream readerStream = program.plinkShell.openAsText()) {
                commands = readerStream.readAll().toString();
            }
            commands = commands.replaceAll("\n", ";");
            Workflow workflow = new Workflow(1);
            if (commands.length() != 0) {
                String finalCommands = commands;
                File finalFile = file;
                workflow.addTask((status, context) -> {
                    ShellUtils.execute(new SimpleShellTask(program.outputDir, finalCommands)
                            .setName(FileUtils.getSubFile(program.outputDir, finalFile.getName()).getName())
                            .toCommand());
                });
                workflow.execute();
                workflow.clearTasks();
            }
            logger.info("Finish the SV-based GWAS task.");
            return;
        }
        logger.info("Finish the sdf2plink conversion task.");
    }
}
