package edu.sysu.pmglab.sdfa.nagf.reference;

import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.gtb.genome.genotype.Genotype;
import edu.sysu.pmglab.gtb.genome.genotype.IGenotypes;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRNARecord;
import edu.sysu.pmglab.sdfa.nagf.AnnotatedSDFManager;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.RNAAffectedCalculator;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.sv.vcf.format.GTBox;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.HashSet;

/**
 * @author Wenjie Peng
 * @create 2024-09-27 00:54
 * @description
 */
public class RefRNAElement {
    SourceRNARecord rnaRecord;
    TIntSet updateFileIndex = new TIntHashSet();
    List<int[]> overlappedSDSVRangeInDifferentSDSVCache;
    private static final HashSet<Genotype> EMPTY_GTY = new HashSet<>();

    static {
        EMPTY_GTY.add(GenotypeConstant.Wild_TYPE_Homozygous);
        EMPTY_GTY.add(GenotypeConstant.Missing_Wild_TYPE_1);
        EMPTY_GTY.add(GenotypeConstant.Missing_Wild_TYPE_2);
        EMPTY_GTY.add(GenotypeConstant.MISSING_GTY);
    }

    public RefRNAElement(int sizeOfSample) {
        overlappedSDSVRangeInDifferentSDSVCache = new List<>(sizeOfSample);
        for (int i = 0; i < sizeOfSample; i++) {
            overlappedSDSVRangeInDifferentSDSVCache.add(new int[]{-1, -1});
        }
    }

    /**
     * add related sdsv index in the corresponding file cache
     *
     * @param fileID
     * @param overlappedIndexInCache
     */
    public void updateRelatedSDSVIndex(int fileID, int overlappedIndexInCache) {
        updateFileIndex.add(fileID);
        int[] intInterval = this.overlappedSDSVRangeInDifferentSDSVCache.fastGet(fileID);
        if (intInterval == null || intInterval[0] == -1) {
            this.overlappedSDSVRangeInDifferentSDSVCache.set(fileID, new int[]{overlappedIndexInCache, overlappedIndexInCache});
        } else {
            intInterval[1] = overlappedIndexInCache;
        }
    }


    public RefRNAElement setRnaRecord(SourceRNARecord rnaRecord) {
        this.rnaRecord = rnaRecord;
        return this;
    }

    public RefRNAElement setOverlappedSDSVRangeInDifferentSDSVCache(List<int[]> overlappedSDSVRangeInDifferentSDSVCache) {
        this.overlappedSDSVRangeInDifferentSDSVCache = overlappedSDSVRangeInDifferentSDSVCache;
        return this;
    }

    public String getNumOfGene() {
        return rnaRecord.getNameOfGene();
    }

    /**
     * TODO: need to be overridden when inputting is population or single vcf ...
     * update numeric values in multiple VCF files as input
     *
     * @param rnaAffectedCalculator
     * @param numericValues
     */
    public void updateNumericValues(RNAAffectedCalculator rnaAffectedCalculator, List<float[]> numericValues) {
        int startIndex, endIndex;
        TIntIterator iterator = updateFileIndex.iterator();
        while (iterator.hasNext()) {
            int fileID = iterator.next();
            int[] range = overlappedSDSVRangeInDifferentSDSVCache.fastGet(fileID);
            startIndex = range[0];
            endIndex = range[1];
            List<ISDSV> sdsvByFileID = AnnotatedSDFManager.getInstance().getSDSVByFileID(fileID);
            float[] numericValueInFile = numericValues.fastGet(fileID);
            for (int svIndex = startIndex; svIndex <= endIndex; svIndex++) {
                ISDSV sdsv = sdsvByFileID.fastGet(svIndex);
                float[] affect = rnaAffectedCalculator.affect(sdsv, rnaRecord);
                for (int affectedIndex = 0; affectedIndex < affect.length; affectedIndex++) {
                    numericValueInFile[affectedIndex] += affect[affectedIndex];
                    numericValueInFile[affectedIndex] = Math.min(1f, numericValueInFile[affectedIndex]);
                }
            }
        }
    }

    /**
     * update numeric values using the file ID when inputting multiple VCF files
     *
     * @param rnaAffectedCalculator
     * @param numericValues
     */
    public void updateNumericValuesForMultiVCF(RNAAffectedCalculator rnaAffectedCalculator, List<float[]> numericValues) {
        int startIndex, endIndex;
        TIntIterator iterator = updateFileIndex.iterator();
        while (iterator.hasNext()) {
            int fileID = iterator.next();
            int[] range = overlappedSDSVRangeInDifferentSDSVCache.fastGet(fileID);
            startIndex = range[0];
            endIndex = range[1];
            List<ISDSV> sdsvByFileID = AnnotatedSDFManager.getInstance().getSDSVByFileID(fileID);
            float[] numericValueInFile = numericValues.fastGet(fileID);
            for (int svIndex = startIndex; svIndex <= endIndex; svIndex++) {
                ISDSV sdsv = sdsvByFileID.fastGet(svIndex);
//                // FIXME: here we can get gtys
                Genotype gty = sdsv.getSVGenotypes().getGty(0);
                if (gty == GenotypeConstant.MISSING_GTY){
                    continue;
                }
                float[] affect = rnaAffectedCalculator.affect(sdsv, rnaRecord);
                for (int affectedIndex = 0; affectedIndex < affect.length; affectedIndex++) {
                    numericValueInFile[affectedIndex] += affect[affectedIndex];
                    numericValueInFile[affectedIndex] = Math.min(1f, numericValueInFile[affectedIndex]);
                }
            }
        }
    }

    /**
     * update numeric values using the genotypes when inputting one population VCF file. Furthermore, `updateFileIndex` was updated for recording updated samples ID.
     *
     * @param rnaAffectedCalculator
     * @param numericValues
     */
    public void updateNumericValuesForPopulation(RNAAffectedCalculator rnaAffectedCalculator, List<float[]> numericValues) {
        int startIndex, endIndex;
        updateFileIndex.clear();
        int[] range = overlappedSDSVRangeInDifferentSDSVCache.fastGet(0);
        startIndex = range[0];
        endIndex = range[1];
        // for population mode, there is only one file existing
        List<ISDSV> sdsvByFileID = AnnotatedSDFManager.getInstance().getSDSVByFileID(0);
        for (int svIndex = startIndex; svIndex <= endIndex; svIndex++) {
            ISDSV sdsv = sdsvByFileID.fastGet(svIndex);
            GTBox gtyBox = sdsv.getSVGenotypes().getGtyBox();
            IGenotypes genotypes = gtyBox.get();
            int sampleSize = genotypes.size();
            float[] affect = rnaAffectedCalculator.affect(sdsv, rnaRecord);
            for (int sampleIndex = 0; sampleIndex < sampleSize; sampleIndex++) {
                Genotype gty = genotypes.get(sampleIndex);
                if (gty.bytecode() > 3) {
                    // not in ./. 0/. ./0 0/0
                    // FIXME: update with boolean array
                    updateFileIndex.add(sampleIndex);
                    float[] numericValueInSample = numericValues.fastGet(sampleIndex);
                    for (int affectedIndex = 0; affectedIndex < affect.length; affectedIndex++) {
                        numericValueInSample[affectedIndex] += affect[affectedIndex];
                        if (numericValueInSample[affectedIndex]>=1f){
                            numericValueInSample[affectedIndex] = 1f;
                        }
                    }
                }
            }
        }
    }

    public TIntSet getUpdatedIndex() {
        return updateFileIndex;
    }

    public boolean isCodingRNA() {
        return rnaRecord.isCodingRNA();
    }

    public String getNameOfRNA() {
        return rnaRecord.getNameOfRNA();
    }
}
