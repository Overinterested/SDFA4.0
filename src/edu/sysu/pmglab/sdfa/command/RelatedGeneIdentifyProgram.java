package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.easytools.identify.SDFRelatedGeneIdentify;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.io.FileUtils;
import sun.rmi.runtime.Log;

import java.io.File;

/**
 * @author Wenjie Peng
 * @create 2025-05-28 10:26
 * @description
 */
public class RelatedGeneIdentifyProgram extends ICommandProgram {

    @Option(names = {"--thread", "-t"}, type = FieldType.varInt32)
    int thread = 4;

    @Option(names = {"identify"}, type = FieldType.NULL, required = true)
    Object identify;

    @Option(names = {"--gene-file", "-f"}, type = FieldType.file, required = true)
    File geneFile;

    @Option(names = {"--annotated-dir", "-d"}, type = FieldType.file, required = true)
    File annotatedSDFDirectory;

    @Option(names = {"--output-dir", "-o"}, type = FieldType.file, required = true)
    File outputDir;

    @Option(names = {"--chr-name", "-cn"}, type = FieldType.string)
    String contigName;

    @Option(names = {"--gene-name", "-gn"}, type = FieldType.string, required = true)
    String geneName;

    public static void main(String[] args) {
        Logger logger = LogBackOptions.getRootLogger();
        RelatedGeneIdentifyProgram program = new RelatedGeneIdentifyProgram();
        CommandOptions options = program.parse(args.length == 1 && args[0].equals("identify") ? new String[]{"--help"} : args);
        if (options.isHelp()) {
            logger.info("\n{}", options.usage());
            return;
        } else {
            logger.info("\n{}", options);
        }
        File outputDir = options.value("-o");
        String geneName = options.value("-gn");
        File outputFilePath = FileUtils.getSubFile(outputDir, "related_" + geneName + ".txt");
        Workflow workflow = new Workflow(options.value("-t"));
        SDFRelatedGeneIdentify functionInstance = new SDFRelatedGeneIdentify(options.value("-f"), geneName)
                .setContigName(options.passed("-cn") ? options.value("-cn") : null)
                .setInputSDFDir(options.value("-d"))
                .setOutputFile(outputFilePath);
        functionInstance.submitTo(workflow);
    }
}
