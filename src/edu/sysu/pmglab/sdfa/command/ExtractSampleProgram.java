package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.ccf.CCFTable;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-21 09:40
 * @description
 */

public class ExtractSampleProgram extends ICommandProgram {
    @Option(names = "extract_samples", type = FieldType.NULL)
    Object extractSamples;

    @Option(names = "-d", type = FieldType.file, required = true)
    File inputDir;

    @Option(names = "-o", type = FieldType.string, required = true)
    String outputDir;

    public static void main(String[] args) throws IOException {
        Logger logger = LogBackOptions.getRootLogger();
        ExtractSampleProgram program = new ExtractSampleProgram();
        CommandOptions options = program.parse(args.length == 1 && args[0].equals("extract_samples") ? new String[]{"--help"} : args);
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
        String subFile = FileUtils.getSubFile(options.value("-o").toString(), "samples.txt");
        WriterStream writer = new WriterStream(new File(subFile), WriterStream.Option.DEFAULT);
        int size = sdfFileList.size();
        for (int i = 0; i < size; i++) {
            File file = sdfFileList.fastGet(i);
            SDFReader reader = new SDFReader(file, SDFReadType.COORDINATE);
            LinkedSet<String> individuals = reader.getIndividuals();
            if (individuals.size() == 1) {
                writer.write(ASCIIUtility.toASCII(individuals.valueOf(0), Constant.CHAR_SET));
                writer.write(Constant.NEWLINE);
            }
            reader.closeAll();
            reader = null;
            CCFTable.gc();
        }
        writer.close();
    }
}
