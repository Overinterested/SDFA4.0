package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
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
 * @create 2025-05-22 04:50
 * @description
 * Due to the inconsistency between the sample names in the UKB 500k SV VCF and those in the phenotype data,
 * while the file names actually contain the correct sample names,
 * we need to create a mapping class to link the sample names in the VCF to their corresponding file names.
 */
public class SDFSampleNameFileNameMapProgram extends ICommandProgram {

    @Option(names = "sample_file_map", type = FieldType.NULL)
    Object sampleMap;

    @Option(names = {"-dir", "-d"}, type = FieldType.file, required = true)
    File inputDir;

    @Option(names = {"-o", "--output"}, type = FieldType.file, required = true)
    File outputDir;

    public static void main(String[] args) throws IOException {
        Logger logger = LogBackOptions.getRootLogger();
        SDFSampleNameFileNameMapProgram program = new SDFSampleNameFileNameMapProgram();
        CommandOptions options = program.parse(args.length == 1 && args[0].equals("filter") ? new String[]{"--help"} : args);
        if (options.isHelp()){
            logger.info("\n{}", options.usage());
            return;
        }else {
            logger.info("\n{}",options);
        }
        List<File> sdfFileList = FileUtils.listFiles((File) options.value("-d"), file -> FileUtils.getExtension(file).endsWith("sdf"));
        if (sdfFileList == null || sdfFileList.isEmpty()) {
            logger.warn("There is no SDF file in the input directory.");
            return;
        }
        File output = FileUtils.getSubFile((File)options.value("-o"), "sampleFileMap.txt");
        ByteStream cache = new ByteStream();
        WriterStream writerStream = new WriterStream(output, WriterStream.Option.DEFAULT);
        int size = sdfFileList.size();
        for (int i = 0; i < size; i++) {
            File file = sdfFileList.fastGet(i);
            SDFReader reader = new SDFReader(file, SDFReadType.COORDINATE);
            LinkedSet<String> individuals = reader.getIndividuals();
            writeIndividuals(individuals, file, cache);
            reader.closeAll();
            reader = null;
            CCFTable.gc();
            writerStream.write(cache.toBytes());
            cache.clear();
        }
        cache.close();
        writerStream.close();
    }
    private static void writeIndividuals(LinkedSet<String> individuals, File file, ByteStream cache){
        int size = individuals.size();
        String name = file.getName();
        cache.write(ASCIIUtility.toASCII(name, Constant.CHAR_SET));
        cache.write(Constant.TAB);
        int endLoop = size - 1;
        for (int i = 0; i < size; i++) {
            cache.write(ASCIIUtility.toASCII(individuals.valueOf(i), Constant.CHAR_SET));
            cache.write(i == endLoop?Constant.NEWLINE: Constant.COMMA);
        }
    }

}
