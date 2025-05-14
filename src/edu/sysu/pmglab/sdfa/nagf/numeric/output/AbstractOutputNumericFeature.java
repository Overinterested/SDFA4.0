package edu.sysu.pmglab.sdfa.nagf.numeric.output;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.ByteList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.sdfa.nagf.NAGFMode;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.AffectedNumericConvertor;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.AffectedNumericFilter;
import edu.sysu.pmglab.sdfa.nagf.reference.RefRNAElement;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Wenjie Peng
 * @create 2024-11-13 19:41
 * @description
 */
public abstract class AbstractOutputNumericFeature {
    static int sampleSize;
    static int numericSize;

    protected static Bytes EMPTY_COL_IN_LINE;
    protected NAGFMode mode = NAGFMode.Multi_VCF;
    protected ByteStream cache = new ByteStream();
    protected ByteList validWriteStatsForEachColInLine;
    protected List<float[]> numericValueListInOutputLine;
    protected TIntHashSet updateSampleIDSet = new TIntHashSet();

    public static String rnaAffectedCalculatorName;
    public static String geneAffectedCalculatorName;
    public static String affectedNumericFilterName;
    public static String affectedNumericConvertorName;
    protected AffectedNumericFilter affectedNumericFilter;
    protected AffectedNumericConvertor affectedNumericConvertor;


    abstract public boolean calcForPopulationVCF();

    abstract public boolean calcForMultiVCF();

    /**
     * load a reference element for output line
     *
     * @param loadedRefRNAElement
     * @return true means there are reference element existing in the cache, false means the inversion
     */
    abstract public boolean acceptOne(List<RefRNAElement> loadedRefRNAElement);

    /**
     * write a numeric annotation line about a reference element;  reset numeric values in the updated sample IDs
     *
     * @param writerStream the output numeric annotation file
     * @throws IOException
     */
    public void writeTo(WriterStream writerStream) throws IOException {
        writeFeatureInfo(cache);
        boolean hasAnnotated = false;
        int endLoop = sampleSize - 1;
        for (int i = 0; i < sampleSize; i++) {
            float[] curr = numericValueListInOutputLine.fastGet(i);
            boolean filter = affectedNumericFilter == null || affectedNumericFilter.filter(curr);
            if (filter) {
                updateSampleIDSet.add(i);
                hasAnnotated = true;
            }
            if (mode == NAGFMode.One_Population_VCF) {
                if (validWriteStatsForEachColInLine.fastGet(i) == (byte) 0) {
                    writerStream.write(EMPTY_COL_IN_LINE);
                } else {
                    affectedNumericConvertor.convertTo(curr, cache);
                    validWriteStatsForEachColInLine.fastSet(i, (byte) 0);
                }
            } else {
                affectedNumericConvertor.convertTo(curr, cache);
            }
            if (i != endLoop) {
                cache.write(Constant.TAB);
            } else {
                cache.write(Constant.NEWLINE);
            }
        }
        if (hasAnnotated) {
            writerStream.write(cache.toBytes());
            TIntIterator iterator = updateSampleIDSet.iterator();
            while (iterator.hasNext()) {
                int updatedSampleID = iterator.next();
                Arrays.fill(numericValueListInOutputLine.fastGet(updatedSampleID), 0);
            }
            updateSampleIDSet.clear();
        }
        cache.clear();
    }

    /**
     * write the reference element attributes at the beginning of each line
     *
     * @param cache
     * @throws IOException
     */
    abstract protected void writeFeatureInfo(ByteStream cache) throws IOException;


    /**
     * init numeric annotation convertor and filter functions for each reference element line
     *
     * @param numOfSamples
     */
    public void initSampleSize(int numOfSamples) {
        affectedNumericFilter = AffectedNumericFilter.getByDefault(affectedNumericFilterName);
        affectedNumericConvertor = AffectedNumericConvertor.getByDefault(affectedNumericConvertorName);
        numericSize = affectedNumericConvertor.sizeOfConversionArray();
        sampleSize = numOfSamples;
        numericValueListInOutputLine = new List<>(numOfSamples);
        int sizeOfOutput = affectedNumericConvertor.sizeOfConversionArray();
        for (int i = 0; i < sizeOfOutput; i++) {
            cache.write(Constant.ZERO);
            if (i != sizeOfOutput - 1) {
                cache.write(Constant.COMMA);
            }
        }
        EMPTY_COL_IN_LINE = cache.toBytes().detach();
        cache.clear();
        for (int i = 0; i < numOfSamples; i++) {
            numericValueListInOutputLine.add(new float[sizeOfOutput]);
        }
    }

    public static void setRnaAffectedCalculatorName(String rnaAffectedCalculatorName) {
        AbstractOutputNumericFeature.rnaAffectedCalculatorName = rnaAffectedCalculatorName;
    }

    public static void setGeneAffectedCalculatorName(String geneAffectedCalculatorName) {
        AbstractOutputNumericFeature.geneAffectedCalculatorName = geneAffectedCalculatorName;
    }

    public static void setAffectedNumericFilterName(String affectedNumericFilterName) {
        AbstractOutputNumericFeature.affectedNumericFilterName = affectedNumericFilterName;
    }

    public static void setAffectedNumericConvertorName(String affectedNumericConvertorName) {
        AbstractOutputNumericFeature.affectedNumericConvertorName = affectedNumericConvertorName;
    }

    public AbstractOutputNumericFeature setMode(NAGFMode mode) {
        this.mode = mode;
        return this;
    }
}
