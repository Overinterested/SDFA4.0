package edu.sysu.pmglab.sdfa.nagf.annotate;

import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.sdfa.annotation.output.SourceOutputManager;
import edu.sysu.pmglab.sdfa.command.AnnotationProgram;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.nagf.NAGFMode;
import sun.rmi.runtime.Log;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-09-10 23:34
 * @description
 */
public class IndexedGeneAnnotation {
    File inputDir;
    NAGFMode mode;
    File outputDir;
    File genomeFile;
    int threads = 4;
    File caseDir;
    File controlDir;
    File annotatedFilesCache;

    private File respondingConfigFile;

    public IndexedGeneAnnotation() {
    }

    public void annotate() throws IOException {
        prepare(genomeFile);
        AnnotationProgram.output(false);
        try {
            AnnotationProgram.main(wrapToAnnotationCommandLines());
        } finally {
            if (respondingConfigFile.exists()) {
                boolean delete = respondingConfigFile.delete();
                if (!delete) {
                    LogBackOptions.getRootLogger().warn("The temporary annotation configuration file cannot be deleted.");
                }
            }
        }
        if (mode == NAGFMode.SV_Level) {
            AnnotationProgram.output(true);
            SourceOutputManager sourceOutputManager = SourceOutputManager.getInstance();
            sourceOutputManager.switchToNAGF();
            LogBackOptions.getRootLogger().info("Start output annotation results.");
            SourceOutputManager.switchToWrite(SDFReadType.ANNOTATION);
            // sliding windows
            sourceOutputManager.partialOutput();
        }
    }

    public String[] wrapToAnnotationCommandLines() {
        List<String> commandLines = new List<>();
        commandLines.add("--threads");
        commandLines.add(String.valueOf(threads));
        if (inputDir != null) {
            commandLines.add("--dir");
            commandLines.add(inputDir.getPath());
        } else {
            commandLines.add("-csd");
            commandLines.add(caseDir.getPath());
            commandLines.add("-ctd");
            commandLines.add(controlDir.getPath());
        }
        if (annotatedFilesCache != null) {
            commandLines.add("-acd");
            commandLines.add(annotatedFilesCache.getPath());
        }
        commandLines.add("--output");
        commandLines.add(outputDir.getPath());
        commandLines.add("--config");
        commandLines.add(respondingConfigFile.getPath());
        return commandLines.toArray(new String[0]);
    }

    public IndexedGeneAnnotation setThreads(int threads) {
        this.threads = threads;
        return this;
    }

    public IndexedGeneAnnotation setInputDir(File inputDir) {
        this.inputDir = inputDir;
        return this;
    }

    public IndexedGeneAnnotation setOutputDir(File outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    public IndexedGeneAnnotation setGenomeFile(File genomeFile) {
        this.genomeFile = genomeFile;
        return this;
    }

    private void prepare(File genomeFile) {
        File tmpConfigFile = FileUtils.getSubFile(outputDir, "tmpConfig.txt");
        if (tmpConfigFile.exists()) {
            LogBackOptions.getRootLogger().warn(tmpConfigFile.getPath() + " has existed.");
            this.respondingConfigFile = tmpConfigFile;
            return;
        }
        this.respondingConfigFile = tmpConfigFile;
        try {
            WriterStream writerStream = new WriterStream(tmpConfigFile, WriterStream.Option.DEFAULT);
            writerStream.write(ASCIIUtility.toASCII("[[annotation]]\n", Constant.CHAR_SET));
            writerStream.write(ASCIIUtility.toASCII("file=" + genomeFile + "\n", Constant.CHAR_SET));
            writerStream.write(ASCIIUtility.toASCII("type=gene\n", Constant.CHAR_SET));
            writerStream.flush();
            writerStream.close();
        } catch (IOException e) {
            LogBackOptions.getRootLogger().error(tmpConfigFile.getPath() + " can't be written.");
        }
    }

    public IndexedGeneAnnotation setMode(NAGFMode mode) {
        this.mode = mode;
        return this;
    }

    public IndexedGeneAnnotation setCaseDir(File caseDir) {
        this.caseDir = caseDir;
        return this;
    }

    public IndexedGeneAnnotation setControlDir(File controlDir) {
        this.controlDir = controlDir;
        return this;
    }

    public IndexedGeneAnnotation setAnnotatedFilesCache(File annotatedFilesCache) {
        this.annotatedFilesCache = annotatedFilesCache;
        return this;
    }
}
