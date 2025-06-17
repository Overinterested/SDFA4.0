package edu.sysu.pmglab.sdfa.nagf;

import ch.qos.logback.classic.Logger;
import com.sun.org.apache.xerces.internal.impl.xpath.XPath;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.rule.Counter;
import edu.sysu.pmglab.commandParser.annotation.rule.Rule;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.commandParser.validator.range.Int_1_RangeValidator;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.executor.ITask;
import edu.sysu.pmglab.executor.Pipeline;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.annotation.source.SourceManager;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.nagf.annotate.IndexedGeneAnnotation;
import edu.sysu.pmglab.sdfa.nagf.numeric.output.AbstractOutputNumericFeature;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.AffectedNumericConvertor;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.conversion.AffectedStringConvertor;
import edu.sysu.pmglab.sdfa.nagf.reference.RefGenomicElementManager;
import edu.sysu.pmglab.sdfa.nagf.sv.NAGFGenomeSourceOutput;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-11-12 20:57
 * @description
 */
@Parser(
        usage = "<nagf> [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.NAGFProgram"),
                @UsageItem(key = "About", value = "Numeric Annotation of Gene Feature for SV level, one population vcf or multiple individual vcfs.")
        },
        rule = @Rule(
                counter = {
                        @Counter(item = {"--population-vcf", "--multiple-vcf", "--sv-mode"},
                                rule = Counter.Type.EQUAL, count = 1),
                        @Counter(item = {"--control-dir", "-dir"},
                                rule = Counter.Type.EQUAL, count = 1)
                }
        )
)
public class NAGFProgram extends ICommandProgram {
    @Option(names = "nagf", type = FieldType.NULL)
    Object nagf;
    @Option(names = "--population-vcf", type = FieldType.NULL)
    boolean populationVCFMode = false;
    @Option(names = "--multiple-vcf", type = FieldType.NULL)
    boolean multipleVCFMode = false;
    @Option(names = "--sv-mode", type = FieldType.NULL)
    boolean svMode = false;
    @Option(names = "--gene-level", type = FieldType.NULL)
    boolean geneLevel = true;
    @Option(names = "--rna-level", type = FieldType.NULL)
    boolean rnaLevel;
    @Option(names = "--rna-batch", type = FieldType.int32)
    int numOfLoadRefRNA = 500;
    @Option(names = {"-t", "--threads"}, type = FieldType.varInt32, validator = Int_1_RangeValidator.class)
    int threads = 4;
    @Option(names = {"-dir", "-d"}, type = FieldType.file)
    File inputDir;
    @Option(names = {"--case-dir", "-csd"}, type = FieldType.file)
    File caseDir;
    @Option(names = {"--control-dir", "-ctd"}, type = FieldType.file)
    File control;
    @Option(names = {"--annotated-cache-dir", "-acd"}, type = FieldType.file, required = false)
    File annotatedCache;
    @Option(names = {"-o", "--output"}, type = FieldType.file, required = true)
    File outputDir;
    @Option(names = {"--genome-file"}, type = FieldType.file, required = true)
    File genomeFile;
    @Option(names = "--model", type = FieldType.string)
    String model;
    @Option(names = "--case-tag", type = FieldType.string)
    String caseTag;
    @Option(names = {"--output-model", "-om"}, type = FieldType.string, defaultTo = "full")
    String outputModel = "full";


    public static void main(String[] args) throws IOException {
        //region init
        NAGFProgram nagfProgram = new NAGFProgram();
        Logger logger = LogBackOptions.getRootLogger();
        CommandOptions options = nagfProgram.parse(args.length == 1 && args[0].equals("nagf") ? new String[]{"--help"} : args);
        if (options.isHelp()) {
            logger.info("\n{}", options.usage());
            return;
        } else {
            logger.info("\n{}", options);
        }
        // init
        int threads = nagfProgram.threads;
        File outputDir = nagfProgram.outputDir;
        File genomeFile = nagfProgram.genomeFile;
        int numOfLoadRefRNA = options.value("--rna-batch");
        NAGFMode mode = options.passed("--sv-mode") ? NAGFMode.SV_Level :
                options.passed("--population-vcf") ? NAGFMode.One_Population_VCF : NAGFMode.Multi_VCF;
        boolean populationVCFMode = options.passed("--population-vcf");
        boolean geneLevel = !options.passed("--rna-level");
        NAGFGenomeSourceOutput.geneLevel(geneLevel);
        outputDir.mkdirs();
        Workflow workflow = new Workflow(threads);
        //endregion
        //region pre-annotate SVs
        IndexedGeneAnnotation indexedGeneAnnotation = new IndexedGeneAnnotation()
                .setMode(mode)
                .setOutputDir(outputDir)
                .setGenomeFile(genomeFile)
                .setThreads(threads);
        if (options.passed("-d")) {
            File inputDir = nagfProgram.inputDir;
            indexedGeneAnnotation.setInputDir(inputDir);
        } else {
            indexedGeneAnnotation.setCaseDir(options.value("-csd"))
                    .setControlDir(options.value("-ctd"));
        }
        if (options.passed("-acd")) {
            indexedGeneAnnotation.setAnnotatedFilesCache(options.value("-acd"));
        }
        indexedGeneAnnotation.annotate();
        if (mode == NAGFMode.SV_Level) {
            return;
        }
        //endregion

        //region define your own numeric feature
        String name = options.value("-om");
        AbstractOutputNumericFeature.setAffectedNumericConvertorName(name);
        AffectedNumericConvertor.add(name, new AffectedStringConvertor());
        //endregion
        List<Pipeline> pipelines = AnnotatedSDFManager.initTasks(
                FileUtils.getSubFile(outputDir, "annotation"),
                options.value("-acd"),
                SDFReadType.ANNOTATION_GT
        );
        for (Pipeline pipeline : pipelines) {
            workflow.addTask(pipeline);
        }
        workflow.execute();
        workflow.clearTasks();
        AnnotatedSDFManager sdsvGenomicIndexManager = AnnotatedSDFManager.getInstance();
        // load reference and map
        genomeFile = new File(SourceManager.getManager().getSourceByIndex(0).getFile().toString());
        RefGenomicElementManager refGenomicElementManager = RefGenomicElementManager.init(
                genomeFile, outputDir, geneLevel,
                populationVCFMode ? NAGFMode.One_Population_VCF : NAGFMode.Multi_VCF
        ).setNumOfLoadRefRNA(numOfLoadRefRNA);
        // start
        logger.info("Start process numeric annotation calculator.");

        ProgressBar bar = new ProgressBar.Builder()
                .setTextRenderer("Numeric annotation speed", "transcripts")
                .build();
        int minRefIndex, maxRefIndex = 0;
        while (true) {
            // load reference
            minRefIndex = maxRefIndex;
            maxRefIndex += numOfLoadRefRNA;
            IntInterval loadPointer = refGenomicElementManager.loadRefRNA(minRefIndex, maxRefIndex, geneLevel);
            if (loadPointer == null) {
                // all reference transcripts have been scanned
                break;
            }
            maxRefIndex = loadPointer.end();
            // load SVs
            List<ITask> loadSVTasks = sdsvGenomicIndexManager.updateSDSV(minRefIndex, loadPointer.end());
            if (loadSVTasks != null) {
                ITask[] tasks = loadSVTasks.toArray(new ITask[0]);
                for (ITask task : tasks) {
                    workflow.addTask(task);
                }
                workflow.execute();
                workflow.clearTasks();
                // handle to output different
                refGenomicElementManager.calcNumericValues();
            }
            bar.step(loadPointer.end() - loadPointer.start());
        }
        bar.close();
        refGenomicElementManager.finishNAGFProcess();

        if (nagfProgram.model != null) {
            if (nagfProgram.caseTag == null) {
                logger.warn("The case tag isn't assigned for specifying the case samples.");
            }
            switch (nagfProgram.model.toLowerCase()) {
                case "multiRegression":
//                    LogisticRegressionAnalyzer var1 = new LogisticRegressionAnalyzer();
//                    var1.multipleRegression(true)
//                            .setOutputDir(outputDir)
//                            .setCaseTag(nagfProgram.caseTag)
//                            .setFileForNAGF(refGenomicElementManager.getOutputFilePath());
//                    try {
//                        var1.submit();
//                    } catch (Exception e) {
//                        logger.error("The analyzer for the NAGF file encounters errors.");
//                    }
                    break;
                case "regression":
//                    LogisticRegressionAnalyzer var2 = new LogisticRegressionAnalyzer();
//                    var2.multipleRegression(false)
//                            .setOutputDir(outputDir)
//                            .setCaseTag(nagfProgram.caseTag)
//                            .setFileForNAGF(refGenomicElementManager.getOutputFilePath());
//                    try {
//                        var2.submit();
//                    } catch (Exception e) {
//                        logger.error("The analyzer for the NAGF file encounters errors.");
//                    }
                    break;
                default:
                    logger.warn("No analysis model called " + nagfProgram.model + ".");
                    break;
            }
        }
    }

}
