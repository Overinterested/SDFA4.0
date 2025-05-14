package edu.sysu.pmglab.sdfa.nagf.numeric.output;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.GeneAffectedCalculator;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.RNAAffectedCalculator;
import edu.sysu.pmglab.sdfa.nagf.reference.RefRNAElement;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;

import java.util.Arrays;

/**
 * @author Wenjie Peng
 * @create 2024-11-13 20:27
 * @description contain a gene feature for output line
 */
public class OutputNumericGeneFeature extends AbstractOutputNumericFeature {
    protected String geneName;
    protected boolean codingGene;
    /**
     * multiple RNAs in the current gene
     */
    protected List<RefRNAElement> subRNAs;
    protected List<float[]> rnaNumericValueList;

    public OutputNumericGeneFeature() {
        this.subRNAs = new List<>();
    }

    @Override
    public boolean calcForPopulationVCF() {
        RNAAffectedCalculator rnaAffectedCalculator = RNAAffectedCalculator.getByDefault(rnaAffectedCalculatorName);
        GeneAffectedCalculator geneAffectedCalculator = GeneAffectedCalculator.getByDefault(geneAffectedCalculatorName);
        boolean hasOutput = false;
        for (RefRNAElement subRNA : subRNAs) {
            TIntSet updateFileIndex = subRNA.getUpdatedIndex();
            if (updateFileIndex.isEmpty()) {
                continue;
            }
            // here only contains one file ID
            hasOutput = true;
            subRNA.updateNumericValuesForPopulation(rnaAffectedCalculator, rnaNumericValueList);
            TIntIterator iterator = updateFileIndex.iterator();
            while (iterator.hasNext()) {
                int updatedSampleIndex = iterator.next();
                // record updated sample ID for only resetting these updated sample ID values in next reference element
                updateSampleIDSet.add(updatedSampleIndex);
                float[] subjectOverlap = rnaNumericValueList.fastGet(updatedSampleIndex);
                validWriteStatsForEachColInLine.fastSet(updatedSampleIndex, (byte)1);
                geneAffectedCalculator.merge(numericValueListInOutputLine.fastGet(updatedSampleIndex), subjectOverlap);
                Arrays.fill(subjectOverlap, 0);
            }
            updateFileIndex.clear();
        }
        return hasOutput;
    }

    @Override
    public boolean calcForMultiVCF() {
        RNAAffectedCalculator rnaAffectedCalculator = RNAAffectedCalculator.getByDefault(rnaAffectedCalculatorName);
        GeneAffectedCalculator geneAffectedCalculator = GeneAffectedCalculator.getByDefault(geneAffectedCalculatorName);
        boolean hasOutput = false;
        for (RefRNAElement subRNA : subRNAs) {
            TIntSet updateFileIndex = subRNA.getUpdatedIndex();
            if (updateFileIndex.isEmpty()) {
                continue;
            }
            hasOutput = true;
            subRNA.updateNumericValuesForMultiVCF(rnaAffectedCalculator, rnaNumericValueList);
            TIntIterator iterator = updateFileIndex.iterator();
            while (iterator.hasNext()) {
                int updatedFileID = iterator.next();
                float[] subjectOverlap = rnaNumericValueList.fastGet(updatedFileID);
                geneAffectedCalculator.merge(numericValueListInOutputLine.fastGet(updatedFileID), subjectOverlap);
                Arrays.fill(subjectOverlap, 0);
            }
            updateFileIndex.clear();
        }
        return hasOutput;
    }

    /**
     * load a reference gene from a list of reference rna
     *
     * @param loadedRefRNAElement
     * @return
     */
    @Override
    public boolean acceptOne(List<RefRNAElement> loadedRefRNAElement) {
        subRNAs.clear();
        if (loadedRefRNAElement.isEmpty()) {
            return false;
        }
        RefRNAElement refRNAElement = loadedRefRNAElement.popFirst();
        subRNAs.add(refRNAElement);
        geneName = refRNAElement.getNumOfGene();
        codingGene = refRNAElement.isCodingRNA();
        int startIndex = 0, size, pointer;
        while (!loadedRefRNAElement.isEmpty()) {
            size = loadedRefRNAElement.size();
            for (pointer = startIndex; pointer < size; pointer++) {
                RefRNAElement curr = loadedRefRNAElement.fastGet(pointer);
                if (geneName.equals(curr.getNumOfGene())) {
                    subRNAs.add(curr);
                    codingGene = codingGene || curr.isCodingRNA();
                    loadedRefRNAElement.removeByIndex(pointer);
                    startIndex = pointer;
                    break;
                }
            }
            if (pointer >= size) {
                break;
            }
        }
        return true;
    }

    /**
     * write the gene attribute at the beginning of each line
     *
     * @param cache write buffer cache
     */
    @Override
    protected void writeFeatureInfo(ByteStream cache) {
        cache.write(ASCIIUtility.toASCII(geneName,Constant.CHAR_SET));
        cache.write(Constant.TAB);
        cache.write(codingGene ? Constant.CODING : Constant.NON_CODING);
        cache.write(Constant.TAB);
    }

    @Override
    public void initSampleSize(int numOfSamples) {
        super.initSampleSize(numOfSamples);
        this.rnaNumericValueList = new List<>(numOfSamples);
        int sizeOfOutput = affectedNumericConvertor.sizeOfConversionArray();
        for (int i = 0; i < numOfSamples; i++) {
            rnaNumericValueList.add(new float[sizeOfOutput]);
        }
    }


}
