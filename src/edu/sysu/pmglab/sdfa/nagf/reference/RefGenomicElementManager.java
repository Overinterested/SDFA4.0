package edu.sysu.pmglab.sdfa.nagf.reference;

import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRNARecord;
import edu.sysu.pmglab.sdfa.nagf.AnnotatedSDFManager;
import edu.sysu.pmglab.sdfa.nagf.NAGFMode;
import edu.sysu.pmglab.sdfa.nagf.numeric.output.AbstractOutputNumericFeature;
import edu.sysu.pmglab.sdfa.nagf.numeric.output.OutputNumericGeneFeature;
import edu.sysu.pmglab.sdfa.nagf.numeric.output.OutputNumericRNAFeature;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.UUID;

/**
 * @author Wenjie Peng
 * @create 2024-11-15 01:14
 * @description manager the input and output of reference elements
 */
public class RefGenomicElementManager {
    protected NAGFMode mode;
    protected IRecord record;
    protected CCFReader reader;

    protected int sizeOfSample;
    protected boolean geneLevel;
    protected File outputFilePath;
    protected int numOfLoadRefRNA = 500;
    protected WriterStream writerStream;
    protected List<RefRNAElement> loadedRNAList;
    protected AbstractOutputNumericFeature outputNumericFeatureInstance;

    protected static RefGenomicElementManager instance;

    protected RefGenomicElementManager(CCFReader reader, boolean geneLevel) {
        this.reader = reader;
        this.geneLevel = geneLevel;
        this.record = reader.getRecord();
        this.loadedRNAList = new List<>();
        this.outputNumericFeatureInstance = geneLevel ? new OutputNumericGeneFeature() : new OutputNumericRNAFeature();
    }

    /**
     * load property transcripts from reference genome
     *
     * @param minRefIndex the min pointer
     * @param maxRefIndex the max pointer
     * @return the property pointer interval for loading; if more than the max pointer, return null
     * @throws IOException
     */
    public IntInterval loadRefRNA(int minRefIndex, int maxRefIndex) throws IOException {
        int numOfRecords = (int) reader.numOfRecords();
        if (reader.isClosed() || minRefIndex >= numOfRecords) {
            // closed or has read all
            return null;
        }
        int trueMaxRefIndex = Math.min(maxRefIndex, numOfRecords);
        // load new reference transcripts
        for (int i = 0; i < trueMaxRefIndex - minRefIndex; i++) {
            reader.read(record);
            // reuse the instance created before
            if (loadedRNAList.size() <= i) {
                loadedRNAList.add(new RefRNAElement(sizeOfSample));
            }
            loadedRNAList.fastGet(i).setRnaRecord(SourceRNARecord.load(record));
        }
        //region clear expired objects
        if (trueMaxRefIndex != maxRefIndex) {
            List<RefRNAElement> lastList = new List<>();
            int numOfValidIndex = trueMaxRefIndex - minRefIndex;
            for (int j = 0; j < numOfValidIndex; j++) {
                lastList.add(loadedRNAList.fastGet(j));
            }
            loadedRNAList.clear();
            loadedRNAList.addAll(lastList);
        }
        //endregion
        return new IntInterval(minRefIndex, trueMaxRefIndex);
    }

    public static RefGenomicElementManager getInstance() {
        return instance;
    }

    /**
     * update current SDSV index in all related RNA elements
     *
     * @param startIndexOfRNA
     * @param endIndexOfRNA
     * @param fileID
     * @param indexOfSDSVInCache
     */
    public void updateRelatedSDSVInRefRNA(int startIndexOfRNA, int endIndexOfRNA, int fileID, int indexOfSDSVInCache) {
        for (int i = startIndexOfRNA; i <= endIndexOfRNA; i++) {
            loadedRNAList.fastGet(i).updateRelatedSDSVIndex(fileID, indexOfSDSVInCache);
        }
    }

    /**
     * set the size of loading RNAs in a batch
     *
     * @param numOfLoadRefRNA
     * @return
     */
    public RefGenomicElementManager setNumOfLoadRefRNA(int numOfLoadRefRNA) {
        this.numOfLoadRefRNA = numOfLoadRefRNA;
        return this;
    }

    /**
     * accept one numeric feature and write out
     *
     * @throws IOException
     */
    public void calcNumericValues() throws IOException {
        // try to extract an output element from list
        boolean containOutput;
        while (outputNumericFeatureInstance.acceptOne(loadedRNAList)) {
            if (mode == NAGFMode.One_Population_VCF) {
                containOutput = outputNumericFeatureInstance.calcForPopulationVCF();
            } else {
                containOutput = outputNumericFeatureInstance.calcForMultiVCF();
            }
            if (containOutput) {
                outputNumericFeatureInstance.writeTo(writerStream);
            }
        }
    }

    /**
     * prepare for numeric annotation:
     * 1. initialize the reference genome file and determine whether to use gene or RNA as a line output;
     * 2. initialize the output file and its header;
     * 3. specify the number of output columns and the calculation and filter functions for numeric annotation;
     * if not multiple file input, `initHeader()` and `setSizeOfSample()` need to be overridden
     *
     * @param refGenomePath reference genome file
     * @param outputDir     output dir
     * @param geneLevel     line format
     * @throws IOException
     */
    public static RefGenomicElementManager init(File refGenomePath, File outputDir, boolean geneLevel, NAGFMode mode) throws IOException {
        if (instance != null) {
            return instance;
        }
        File outputFilePath = FileUtils.getSubFile(outputDir, UUID.randomUUID() + ".txt");
        WriterStream writerStream = new WriterStream(outputFilePath, WriterStream.Option.DEFAULT);
        int sizeOfSample = AnnotatedSDFManager.getInstance().numOfSamples();
        if (mode == NAGFMode.One_Population_VCF) {
            sizeOfSample = AnnotatedSDFManager.getInstance().getReaderByIndex(0).numOfIndividuals();
        }
        instance = new RefGenomicElementManager(new CCFReader(refGenomePath), geneLevel)
                .setMode(mode)
                .setWriterStream(writerStream)
                .setOutputFilePath(outputFilePath)
                .initHeader(geneLevel)
                .setSizeOfSample(sizeOfSample);
        return instance;
    }

    /**
     * init output file header depending on the nagf mode
     *
     * @param geneLevel
     * @return
     * @throws IOException
     */
    private RefGenomicElementManager initHeader(boolean geneLevel) throws IOException {
        AnnotatedSDFManager annotatedSDFManager = AnnotatedSDFManager.getInstance();
        int fileSize = annotatedSDFManager.sizeOfAnnotationFile();
        if (geneLevel) {
            writerStream.write(ASCIIUtility.toASCII("#Gene\tIsCoding\t", Constant.CHAR_SET));
        } else {
            // rna level
            writerStream.write(ASCIIUtility.toASCII("#Gene\tRNA\tIsCoding\t", Constant.CHAR_SET));
        }
        if (mode == NAGFMode.One_Population_VCF) {
            LinkedSet<String> individuals = annotatedSDFManager.getReaderByIndex(0).getIndividuals();
            int sampleSize = individuals.size();
            for (int i = 0; i < sampleSize; i++) {
                writerStream.write(ASCIIUtility.toASCII(individuals.valueOf(i), Constant.CHAR_SET));
                if (i != sampleSize - 1) {
                    writerStream.write(Constant.TAB);
                } else {
                    writerStream.write(Constant.NEWLINE);
                }
            }
        } else {
            boolean overlap = false;
            HashSet<String> sampleNameSet = new HashSet<>();
            List<String> sampleNames = new List<>(fileSize);
            for (int i = 0; i < fileSize; i++) {
                LinkedSet<String> individuals = annotatedSDFManager.getReaderByIndex(i).getIndividuals();
                int size = individuals.size();
                for (int j = 0; j < size; j++) {
                    String sample = individuals.valueOf(j).toString();
                    if (sampleNameSet.contains(sample)) {
                        overlap = true;
                        break;
                    }
                    sampleNames.add(sample);
                    sampleNameSet.add(sample);
                }
                if (overlap) {
                    break;
                }
            }
            if (sampleNames.size() != fileSize) {
                // sample name overlaps
                sampleNames.close();
                for (int i = 0; i < fileSize; i++) {
                    String fileName = annotatedSDFManager.getReaderByIndex(i).getFile().getPath();
                    writerStream.write(ASCIIUtility.toASCII(fileName, Constant.CHAR_SET));
                    if (i != fileSize - 1) {
                        writerStream.write(Constant.TAB);
                    } else {
                        writerStream.write(Constant.NEWLINE);
                    }
                }
            } else {
                for (int i = 0; i < fileSize; i++) {
                    writerStream.write(ASCIIUtility.toASCII(sampleNames.fastGet(i), Constant.CHAR_SET));
                    if (i != fileSize - 1) {
                        writerStream.write(Constant.TAB);
                    } else {
                        writerStream.write(Constant.NEWLINE);
                    }
                }
                sampleNames.close();
            }
        }
        return this;
    }


    public void finishNAGFProcess() throws IOException {
        writerStream.close();
        File trueOutputPath = FileUtils.getSubFile(
                outputFilePath.getParentFile(),
                (geneLevel ? "gene_" : "rna_") + "numeric_output.txt");
        if (trueOutputPath.exists()) {
            LogBackOptions.getRootLogger().warn("Output file is stored at " + outputFilePath.getPath());
        } else {
            outputFilePath.renameTo(trueOutputPath);
            LogBackOptions.getRootLogger().info("Output file is stored at " + trueOutputPath.getPath());
        }
    }

    protected RefGenomicElementManager setOutputFilePath(File outputFilePath) {
        this.outputFilePath = outputFilePath;
        return this;
    }

    public RefGenomicElementManager setWriterStream(WriterStream writerStream) {
        this.writerStream = writerStream;
        return this;
    }


    /**
     * 1. specify the output columns;
     * 2. specify the calculation and filter functions for numeric annotation;
     *
     * @param sizeOfSample
     */
    public RefGenomicElementManager setSizeOfSample(int sizeOfSample) {
        this.sizeOfSample = sizeOfSample;
        outputNumericFeatureInstance.initSampleSize(this.sizeOfSample);
        return this;
    }

    public RefGenomicElementManager setMode(NAGFMode mode) {
        this.mode = mode;
        this.outputNumericFeatureInstance.setMode(mode);
        return this;
    }

    public File getOutputFilePath() {
        return outputFilePath;
    }
}
