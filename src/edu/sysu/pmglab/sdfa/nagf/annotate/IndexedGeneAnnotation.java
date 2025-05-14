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
    private File respondingConfigFile;

    public IndexedGeneAnnotation() {
    }

    public void annotate() throws IOException {
        prepare(genomeFile);
        AnnotationProgram.output(false);
        AnnotationProgram.main(wrapToAnnotationCommandLines());
        respondingConfigFile.delete();
        AnnotationProgram.output(true);
        if (mode == NAGFMode.SV_Level){
            SourceOutputManager sourceOutputManager = SourceOutputManager.getInstance();
            sourceOutputManager.switchToNAGF();
            LogBackOptions.getRootLogger().info("Start output annotation results.");
            SourceOutputManager.switchToWrite(SDFReadType.ANNOTATION);
            // sliding windows
            sourceOutputManager.partialOutput();
        }
    }

    private File wrapRefGenomeToConfig() {
        return genomeFile;
    }

    public String[] wrapToAnnotationCommandLines() {
        List<String> commandLines = new List<>();
        commandLines.add("--threads");
        commandLines.add(String.valueOf(threads));
        commandLines.add("--dir");
        commandLines.add(inputDir.getPath());
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
            writerStream.write(ASCIIUtility.toASCII("file=" + genomeFile + "\n",Constant.CHAR_SET));
            writerStream.write(ASCIIUtility.toASCII("type=genome\n",Constant.CHAR_SET));
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
}
