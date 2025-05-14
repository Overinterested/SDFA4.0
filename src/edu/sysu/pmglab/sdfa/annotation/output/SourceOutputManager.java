package edu.sysu.pmglab.sdfa.annotation.output;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.annotation.source.GenomeSource;
import edu.sysu.pmglab.sdfa.annotation.source.Source;
import edu.sysu.pmglab.sdfa.annotation.source.SourceManager;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVCachedLoserTree;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVLoserTree;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVManager;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SingleFileSDSVManager;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-09-13 01:45
 * @description
 */
public class SourceOutputManager {
    int numOfSource;
    final File outputDir;
    SDSVLoserTree loserTree;
    WriterStream writeForSDFFiles;
    private List<File> annotatedSDFFiles;
    private static int sdsvRecordThreshold = 8192 / 2;
    private static int referenceRecordThreshold = 8194 * 4;
    private static final byte[] header = "#Chr\tPos\tEnd\tSVType\tSVLen\tfileID\t".getBytes();

    List<SourceOutput> sourceOutputSet = new List<>();

    private static SourceOutputManager instance;

    private SourceOutputManager(File outputDir) {
        this.outputDir = outputDir;
    }

    public static synchronized SourceOutputManager init(File outputDir) {
        if (instance == null) {
            instance = new SourceOutputManager(outputDir);
        }
        return instance;
    }

    public static SourceOutputManager getInstance() {
        return instance;
    }

    public static void switchToWrite(SDFReadType readerMode) throws IOException {
        SourceManager.switchToWrite();
        buildLoserTree(readerMode);
        initOutputStream();
    }

    public void partialOutput() throws IOException {
        int sizeOfSDSV;
        boolean successLoad;
        IntList fileIDList = new IntList();
        ByteStream lineWriter = new ByteStream();
        List<ISDSV> collectedSDSVList = new List<>();
        this.numOfSource = SourceManager.numOfSource();
        ProgressBar bar = new ProgressBar.Builder().setTextRenderer("Output Speed", "SVs").build();
        while (true) {
            successLoad = sdsvRecordThreshold - collectedSDSVList.size() != 0;
            successLoad = successLoad && loserTree.getMinRecords(
                    sdsvRecordThreshold - collectedSDSVList.size(),
                    collectedSDSVList, fileIDList, sourceOutputSet
            );
            sizeOfSDSV = collectedSDSVList.size();
            if (!successLoad && sizeOfSDSV == 0) {
                // finish outputting
                break;
            }
            if (successLoad) {
                boolean belowThreshold = checkReferenceRecords(referenceRecordThreshold);
                if (belowThreshold && sizeOfSDSV != sdsvRecordThreshold) {
                    // number of reference files and SV records did not reach the threshold
                    continue;
                }
            }
            collectReferenceRecords();
            for (int i = 0; i < sizeOfSDSV; i++) {
                int fileID = fileIDList.fastGet(i);
                ISDSV sdsv = collectedSDSVList.fastGet(i);
                lineWriter.clear();
                sdsv.writeTo(lineWriter);
                lineWriter.write(Constant.TAB);
                lineWriter.write(ASCIIUtility.toASCII(fileID));
                lineWriter.write(Constant.TAB);
                IntList annotationIndex = sdsv.getAnnotationIndexes();
                for (int j = 0; j < numOfSource; j++) {
                    SourceOutput sourceOutput = sourceOutputSet.fastGet(j);
                    sourceOutput.writeAnnotation(sdsv, lineWriter, annotationIndex.fastGet(2 * j), annotationIndex.fastGet(2 * j + 1));
                    if (j != numOfSource - 1) {
                        lineWriter.write(Constant.TAB);
                    }
                }
                bar.step(1);
                lineWriter.write(Constant.NEWLINE);
                writeForSDFFiles.write(lineWriter.toBytes());
            }
            fileIDList.clear();
            collectedSDSVList.clear();
        }
        bar.close();
        writeForSDFFiles.close();
    }


    private boolean checkReferenceRecords(int numOfSourceRecords) {
        int num = 0;
        for (int i = 0; i < sourceOutputSet.size(); i++) {
            num += sourceOutputSet.fastGet(i).numOfNeededRecords();
        }
        return num < numOfSourceRecords;
    }

    public synchronized static void addSourceOutput(Source source) throws IOException {
        if (instance != null) {
            instance.sourceOutputSet.add(SourceOutput.of(source));
            instance.numOfSource++;
            return;
        }
        throw new UnsupportedOperationException("Source output manager has not been init.");
    }

    /**
     * init annotated file list
     * @param numOfSDFFiles
     */
    public static void numOfAnnotatedSDFFiles(int numOfSDFFiles) {
        List<File> files = new List<>(numOfSDFFiles);
        for (int i = 0; i < numOfSDFFiles; i++) {
            files.add(null);
        }
        instance.annotatedSDFFiles = files;
    }

    public static void attachAnnotatedFile(int index, File annotatedSDF) {
        instance.annotatedSDFFiles.set(index, annotatedSDF);
    }

    private static void buildLoserTree(SDFReadType readerMode) throws IOException {
        instance.loserTree = new SDSVCachedLoserTree(instance.annotatedSDFFiles, readerMode);
    }

    private static void initOutputStream() throws IOException {
        File outputDir = instance.outputDir;
        File outputFile = FileUtils.getSubFile(outputDir, "unified.annot");
        WriterStream writerStream = new WriterStream(outputFile, WriterStream.Option.DEFAULT);
        writeFileIDDetails(writerStream);
        writerStream.write(header);
        int size = instance.numOfSource;
        for (int i = 0; i < size; i++) {
            Bytes header = instance.sourceOutputSet.fastGet(i).getHeader();
            writerStream.write(header);
            if (i != size - 1) {
                writerStream.write(Constant.TAB);
            } else {
                writerStream.write(Constant.NEWLINE);
            }
        }
        writerStream.flush();
        instance.writeForSDFFiles = writerStream;

    }


    private void collectReferenceRecords() throws IOException {
        for (SourceOutput sourceOutput : sourceOutputSet) {
            sourceOutput.mapPointer();
        }
    }

    public void switchToNAGF() {
        SourceManager manager = SourceManager.getManager();
        Source source = manager.getSourceByIndex(0);
        if (source instanceof GenomeSource) {
            manager.clear();
            manager.addExtraSource(((GenomeSource) source).transfer());
        }
    }
    private static void writeFileIDDetails(WriterStream writerStream) throws IOException {
        LinkedSet<SingleFileSDSVManager> fileManagers = SDSVManager.getInstance().getFileManagers();
        int size = fileManagers.size();
        Bytes prefix = new Bytes("<fileID=");
        Bytes value = new Bytes("value=");
        for (int i = 0; i < size; i++) {
            SingleFileSDSVManager singleFileSDSVManager = fileManagers.valueOf(i);
            String path = singleFileSDSVManager.getFile().getPath();
            writerStream.write(Constant.DOUBLE_NUMBER_SIGN);
            writerStream.write(prefix);
            writerStream.write(ASCIIUtility.toASCII(i));
            writerStream.write(ASCIIUtility.toASCII(", ",Constant.CHAR_SET));
            writerStream.write(value);
            writerStream.write(ASCIIUtility.toASCII(path, Constant.CHAR_SET));
            writerStream.write(Constant.GREATER_THAN_SIGN);
            writerStream.write(Constant.NEWLINE);
        }
    }
}
