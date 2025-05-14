package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.commandParser.validator.range.Int_1_RangeValidator;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.executor.ITask;
import edu.sysu.pmglab.executor.Pipeline;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.annotation.output.SourceOutputManager;
import edu.sysu.pmglab.sdfa.annotation.preprocess.ConfigInput;
import edu.sysu.pmglab.sdfa.annotation.source.Source;
import edu.sysu.pmglab.sdfa.annotation.source.SourceManager;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * @author Wenjie Peng
 * @create 2024-09-08 07:32
 * @description
 */
@Parser(
        usage = "annotate [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.AnnotationProgram"),
                @UsageItem(key = "About", value = "Annotate SVs using a resource config file recording the corresponding type and path of each resource.")
        }
)
public class AnnotationProgram extends ICommandProgram {
    @Option(names = "annotate", type = FieldType.NULL)
    Object annotate;

    @Option(names = {"-t", "--threads"}, type = FieldType.varInt32, validator = Int_1_RangeValidator.class)
    int threads = 4;

    @Option(names = {"-dir", "-d", "--dir"}, type = FieldType.file, required = true)
    File inputDir;

    @Option(names = {"-o", "--output"}, type = FieldType.file, required = true)
    File outputDir;

    @Option(names = {"--config"}, type = FieldType.file, required = true)
    File configFilePath;

    static boolean output = true;

    private static HashMap<String, BiFunction<ISDSV, IRecord, Boolean>> furthAnnotateFunctionList = new HashMap<>();

    public static void main(String[] args) throws IOException {
        Logger logger = LogBackOptions.getRootLogger();

        AnnotationProgram program = new AnnotationProgram();
        CommandOptions options = program.parse(args.length == 1 && args[0].equals("annotate") ? new String[]{"--help"} : args);
        if (options.isHelp()) {
            logger.info("\n{}", options.usage());
        } else {
            logger.info("\n{}", options);
        }
        // init
        int threads = options.value("-t");
        File outputDir = options.value("-o");
        File inputDir = options.value("--dir");
        LiveFile config = LiveFile.of((File) options.value("--config"));

        // load and parse raw files to sdf files
        Workflow workflow = new Workflow(threads);
        SDSVManager sdsvManager = Objects.requireNonNull(SDSVManager.of(inputDir)).setOutputDir(outputDir).setReadOption(SDFReadType.ANNOTATION);
        List<Pipeline> pipelines = sdsvManager.parseToSDFFileTask();
        for (Pipeline pipeline : pipelines) {
            workflow.addTask(pipeline);
        }
        workflow.execute();
        workflow.clearTasks();

        // load source
        pipelines = new ConfigInput(outputDir, config).annotationResourcePrepareTasks();
        for (Pipeline pipeline : pipelines) {
            workflow.addTask(pipeline);
        }
        workflow.execute();
        workflow.clearTasks();

        SourceManager sourceManager = SourceManager.getManager();
        // region: add further overlap function
        registerFunction(sourceManager);
        // endregion
        ITask[] tasks = sourceManager.loadSource().toArray(new ITask[0]);
        for (ITask task : tasks) {
            workflow.addTask(task);
        }
        workflow.execute();
        workflow.clearTasks();
        // indexed annotation
        workflow.addTask(((status, context) -> {
            logger.info("Start indexed annotation for SVs.");
            context.put(
                    ProgressBar.class,
                    new ProgressBar.Builder().setTextRenderer("Indexed annotation speed", "files")
                            .setInitialMax(sdsvManager.numOfFileSize())
                            .build());
        }));
        int index = 0;
        workflow.execute();
        workflow.clearTasks();
        // start annotation
        while (true) {
            int startFileIndex = threads * index;
            int endFileIndex = threads * (index + 1);
            IntInterval check = sdsvManager.check(startFileIndex, endFileIndex);
            if (check == null) {
                workflow.addTask(((status, context) -> {
                    ProgressBar bar = (ProgressBar) context.get(ProgressBar.class);
                    if (bar != null) {
                        bar.close();
                    }
                }));
                workflow.execute();
                workflow.clearTasks();
                break;
            }
            startFileIndex = check.start();
            endFileIndex = check.end();
            // load svs from sdf
            List<ITask> loadTasks = sdsvManager.loadSVTask(check.start(), check.end());
            tasks = loadTasks.toArray(new ITask[0]);
            for (ITask task : tasks) {
                workflow.addTask(task);
            }
            workflow.execute();
            workflow.clearTasks();
            loadTasks.clear();
            // annotate
            List<ITask> annotateTasks = sourceManager.annotateTask(startFileIndex, endFileIndex);
            tasks = annotateTasks.toArray(new ITask[0]);
            for (ITask task : tasks) {
                workflow.addTask(task);
            }
            workflow.execute();
            workflow.clearTasks();
            annotateTasks.clear();
            // write
            List<ITask> writeTasks = sdsvManager.writeTask(startFileIndex, endFileIndex);
            tasks = writeTasks.toArray(new ITask[0]);
            for (ITask task : tasks) {
                workflow.addTask(task);
            }
            workflow.execute();
            workflow.clearTasks();
            writeTasks.clear();
            // step
            int finalEndFileIndex = endFileIndex;
            int finalStartFileIndex = startFileIndex;
            workflow.addTask(((status, context) -> {
                ProgressBar bar = (ProgressBar) context.get(ProgressBar.class);
                if (bar != null) {
                    bar.step(finalEndFileIndex - finalStartFileIndex);
                }
            }));
            workflow.execute();
            workflow.clearTasks();
            index++;
        }
        // start output
        if (output) {
            LogBackOptions.getRootLogger().info("Start output annotation results.");
            SourceOutputManager.switchToWrite(SDFReadType.ANNOTATION);
            // sliding windows
            SourceOutputManager.getInstance().partialOutput();
        }
    }

    public static void output(boolean output) {
        AnnotationProgram.output = output;
    }

    public static synchronized void addFurtherAnnotateFunction(String annotationFileName, BiFunction<ISDSV, IRecord, Boolean> furtherOverlapFunction) {
        furthAnnotateFunctionList.put(annotationFileName, furtherOverlapFunction);
    }

    private static synchronized void registerFunction(SourceManager sourceManager) {
        Set<String> annotationFileNameList = furthAnnotateFunctionList.keySet();
        for (String annotationFileName : annotationFileNameList) {
            Source sourceByFile = sourceManager.getSourceByFile(annotationFileName);
            if (sourceByFile == null) {
                throw new UnsupportedOperationException("The annotation file, named " + annotationFileName + " , is not registered.");
            }
            sourceByFile.setFurtherOverlap(furthAnnotateFunctionList.get(annotationFileName));
        }
    }
}
