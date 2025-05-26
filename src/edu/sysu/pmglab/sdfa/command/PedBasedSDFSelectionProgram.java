package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.ccf.CCFTable;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.executor.Pipeline;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.gwas.PEDFile;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
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
    @Option(names = {"--ped-file", "-f"}, type = FieldType.string, required = true)
    String pedFile;
    @Option(names = {"-d", "--sdf-dir"}, type = FieldType.file, required = true)
    File sdfDir;
    @Option(names = {"-o", "--output-dir"}, type = FieldType.file, required = true)
    File outputDir;
    @Option(names = {"--count"}, type = FieldType.NULL)
    Object countMode = false;
    @Option(names = {"--sample-map-file", "-smf"}, type = FieldType.file)
    File filePathSampleNameMapFile;

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
        // init
        File outputDir = options.value("-o");
        ProgressBar selectorBar = new ProgressBar.Builder()
                .setTextRenderer("File Copy Speed", "Files")
                .setInitialMax(Integer.MIN_VALUE)
                .build();
        // PED Load
        PEDFile pedInstance = PEDFile.load(options.value("-f"));
        HashSet<String> allUidSet = pedInstance.getAllUidSet();
        if (allUidSet.isEmpty()) {
            logger.warn("There is no sample in the input PED file.");
            return;
        }
        // check sample map file
        if (options.passed("-smf")) {
            HashMap<String, String> sampleNameFileMap = collectSampleNameFileMap(options.value("-smf"));
            for (String uid : allUidSet) {
                String filePath = sampleNameFileMap.get(uid);
                if (filePath != null) {
                    File file = new File(filePath);
                    FileUtils.copy(
                            file,
                            FileUtils.getSubFile(outputDir.toString(), file.getName())
                    );
                    selectorBar.step(1);
                }
            }
            selectorBar.close();
            return;
        }

        List<File> sdfFileList = FileUtils.listFiles((File) options.value("-d"), file -> FileUtils.getExtension(file).endsWith("sdf"));
        if (sdfFileList == null || sdfFileList.isEmpty()) {
            logger.warn("There is no SDF file in the input directory.");
            return;
        }


        int pedFileCount = 0;


        int size = sdfFileList.size();
        for (int i = 0; i < size; i++) {
            File file = sdfFileList.fastGet(i);
            SDFReader reader = new SDFReader(file, SDFReadType.COORDINATE);
            LinkedSet<String> individuals = reader.getIndividuals();
            String currSample = individuals.valueOf(0);
            if (individuals.size() == 1 && allUidSet.contains(currSample)) {
                if (!options.passed("--count")) {
                    allUidSet.remove(currSample);
                    FileUtils.copy(file.toString(), FileUtils.getSubFile(outputDir, file.getName()).getPath());
                }
                pedFileCount++;
                selectorBar.step(1);
            }
            reader.closeAll();
            reader = null;
            CCFTable.gc();
        }
        selectorBar.close();
        if (options.passed("--count")) {
            logger.info("Detect " + pedFileCount + " individuals from the ped file in this input directory.");
        } else {
            logger.info("Finish copy " + pedFileCount + " SDF files from the raw input directory");
        }
    }

    private static HashMap<String, String> collectSampleNameFileMap(File file) throws IOException {
        HashMap<String, String> sampleNameFileMap = new HashMap<>();
        ByteStream cache = new ByteStream();
        ReaderStream reader = LiveFile.of(file).openAsText();
        BytesSplitter tabSplitter = new BytesSplitter(Constant.TAB);
        while (reader.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            tabSplitter.init(line);
            String fileName = tabSplitter.next().toString();
            String sampleName = tabSplitter.next().toString();
            sampleNameFileMap.put(sampleName, fileName);
            cache.clear();
        }
        reader.close();
        return sampleNameFileMap;
    }
}
