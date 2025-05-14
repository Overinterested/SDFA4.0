package edu.sysu.pmglab.sdfa.sv.sdsv.container;

import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.executor.ITask;
import edu.sysu.pmglab.executor.Pipeline;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.annotation.output.SourceOutputManager;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.SDSVConversionManager;
import edu.sysu.pmglab.sdfa.sv.vcf.SVFilterManager;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInstance;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-09-08 20:16
 * @description
 */
public class SDSVManager extends ICommandProgram {
    File outputDir;
    String callingType;
    boolean silent = false;
    SVFilterManager filterManager;
    private SDFReadType readerMode;
    private static SDSVManager instance;
    LinkedSet<SingleFileSDSVManager> fileManagers;

    private SDSVManager() {
        fileManagers = new LinkedSet<>();
    }

    public static SDSVManager of(String inputDir) {
        return of(new File(inputDir));
    }

    public static SDSVManager of(File inputDir) {
        instance = new SDSVManager();
        File[] files = inputDir.listFiles();
        if (files == null || files.length == 0) {
            throw new UnsupportedOperationException("There are no files in " + inputDir);
        }
        int index = 0;
        for (File file : files) {
            String name = file.getName();
            boolean isValidFile = name.endsWith(".sdf") ||
                    name.endsWith(".vcf") ||
                    name.endsWith(".vcf.gz") ||
                    name.endsWith(".vcf.bgz");
            if (isValidFile) {
                try {
                    LiveFile liveFile = LiveFile.of(file);
                    instance.fileManagers.add(new SingleFileSDSVManager(liveFile).setIndex(index++));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return instance;
    }

    public void run(int thread) {
        Workflow workflow = new Workflow(thread);
        List<Pipeline> pipelines = parseToSDFFileTask();
        for (Pipeline pipeline : pipelines) {
            workflow.addTask(pipeline);
        }
        workflow.execute();
    }

    public List<Pipeline> parseToSDFFileTask() {
        int numOfParseFile = 0;
        for (SingleFileSDSVManager fileManager : fileManagers) {
            if (fileManager.needParse()) {
                numOfParseFile++;
            }
        }
        int numOfSDFFiles = fileManagers.size();
        if (numOfSDFFiles == 0) {
            throw new UnsupportedOperationException("No SV file is passed in");
        }
        LogBackOptions.getRootLogger().info("Collect " + numOfSDFFiles + " files and " + numOfParseFile + " files need convert to SDF.");
        List<Pipeline> tasks = new List<>(numOfSDFFiles);
        ProgressBar bar;
        if (!silent) {
            bar = new ProgressBar.Builder().setTextRenderer("Parse speed", "files")
                    .setInitialMax(numOfSDFFiles)
                    .build();
        } else {
            bar = null;
        }
        if (readerMode == SDFReadType.ANNOTATION) {
            // init output dir for annotated files
            SourceOutputManager.init(outputDir);
            SourceOutputManager.numOfAnnotatedSDFFiles(numOfSDFFiles);
        }
        File sdfOutputDir = FileUtils.getSubFile(outputDir, "sdf");
        if (numOfParseFile != 0) {
            sdfOutputDir.mkdirs();
        }
        List<ITask> parseTasks = new List<>();
        for (int i = 0; i < numOfSDFFiles; i++) {
            int finalI = i;
            parseTasks.add((
                    (status, context) ->
                    {
                        LiveFile sdfFile = null;
                        SingleFileSDSVManager singleFileSDSVManager = fileManagers.valueOf(finalI);
                        if (singleFileSDSVManager.needParse()) {
                            VCFInstance vcfInstance = new VCFInstance(singleFileSDSVManager.getFile(), callingType);
                            SDSVConversionManager sdsvConversionManager = new SDSVConversionManager();
                            File outputFile = new File(sdfOutputDir + File.separator + singleFileSDSVManager.getFile().getName() + ".sdf");
                            sdsvConversionManager.initWriter(outputFile);
                            vcfInstance.setConversionFromSV2Record(sdsvConversionManager)
                                    .setSVFilterManager(filterManager)
                                    .parse();
                            if (vcfInstance.isEmptyVCF()) {
                                if (!silent && bar != null) {
                                    bar.step(1);
                                    vcfInstance.clear();
                                    LogBackOptions.getRootLogger().warn(singleFileSDSVManager.getFile() + " contains no SV.");
                                    return;
                                }
                            }
                            if (!silent && bar != null) {
                                bar.step(1);
                            }
                            if (outputFile.exists()) {
                                sdfFile = LiveFile.of(outputFile);
                            }
                            vcfInstance = null;
                        } else {
                            sdfFile = singleFileSDSVManager.getFile();
                            if (!silent && bar != null) {
                                bar.step(1);
                            }
                        }
                        if (sdfFile != null) {
                            singleFileSDSVManager.setSdfFile(new File(sdfFile.getPath()));
                        }
                        singleFileSDSVManager.setReaderMode(readerMode = readerMode == null ? SDFReadType.FULL : readerMode);
                    }
            ));
        }
        Pipeline parsePipeline = new Pipeline(parseTasks);
        tasks.add(parsePipeline);
        if (!silent) {
            tasks.add(new Pipeline(true, ((status, context) -> {
                if (bar != null) {
                    bar.close();
                }
            })));
        }

        return tasks;
    }

    public List<ITask> loadSVTask(int startIndex, int endIndex) {
        List<ITask> tasks = new List<>(endIndex - startIndex);
        for (int i = startIndex; i < endIndex; i++) {
            int finalI = i;
            tasks.add((
                    (status, context) -> {
                        SingleFileSDSVManager singleFileSDSVManager = fileManagers.valueOf(finalI);
                        LiveFile file = singleFileSDSVManager.getFile();
                        File annotatedSDFFile = FileUtils.getSubFile(outputDir, file.getName());
                        if (annotatedSDFFile.exists()) {
                            return;
                        }
                        fileManagers.valueOf(finalI).loadWithInit();
                    }
            ));
        }
        return tasks;
    }

    public List<ITask> clearTask(int startIndex, int endIndex) {
        IntInterval range = check(startIndex, endIndex);
        if (range == null) {
            return null;
        }
        List<ITask> tasks = new List<>(range.end() - range.start());
        for (int i = range.start(); i < range.end(); i++) {
            int finalI = i;
            tasks.add((
                    (status, context) -> fileManagers.valueOf(finalI).clear()
            ));
        }
        return tasks;
    }

    public List<ITask> writeTask(int startIndex, int endIndex) {
        File annotationDir = FileUtils.getSubFile(outputDir, "annotation");
        annotationDir.mkdirs();
        List<ITask> tasks = new List<>(endIndex - startIndex);
        for (int i = startIndex; i < endIndex; i++) {
            int finalI = i;
            tasks.add((
                    (status, context) -> {
                        SingleFileSDSVManager singleFileSDSVManager = fileManagers.valueOf(finalI);
                        String fileName = singleFileSDSVManager.getReader().getFile().getName();
                        String outputFileName = FileUtils.getSubFile(annotationDir.toString(), fileName);
                        File outputFile = new File(outputFileName);
                        boolean exists = outputFile.exists();
                        // TODO
                        if (!exists) {
                            singleFileSDSVManager.writeTo(outputFile);
                        }
                        SourceOutputManager.attachAnnotatedFile(finalI, outputFile);
                    }
            ));
        }
        return tasks;
    }

    public IntInterval check(int startIndex, int endIndex) {
        int fileSize = fileManagers.size();
        if (startIndex >= fileSize) {
            return null;
        }
        endIndex = Math.min(endIndex, fileSize);
        return new IntInterval(startIndex, endIndex);
    }

    public static SDSVManager getInstance() {
        return instance;
    }

    public int numOfFileSize() {
        return fileManagers.size();
    }

    public SingleFileSDSVManager getByIndex(int index) {
        return fileManagers.valueOf(index);
    }

    public SDSVManager setOutputDir(Object outputDir) {
        assert outputDir != null;
        this.outputDir = new File(outputDir.toString());
        return this;
    }

    public SDSVManager setReadOption(SDFReadType readerMode) {
        this.readerMode = readerMode;
        return this;
    }

    public LinkedSet<SingleFileSDSVManager> getFileManagers() {
        return fileManagers;
    }

    public List<SDFReader> getSDFReaders() {
        List<SDFReader> returns = new List<>();
        for (SingleFileSDSVManager fileManager : fileManagers) {
            returns.add(fileManager.getReader());
        }
        return returns;
    }

    public SDSVManager setFilterManager(SVFilterManager filterManager) {
        this.filterManager = filterManager;
        return this;
    }


    public SDSVManager setCallingType(String callingType) {
        this.callingType = callingType;
        return this;
    }

    public File getOutputDir() {
        return outputDir;
    }
}
