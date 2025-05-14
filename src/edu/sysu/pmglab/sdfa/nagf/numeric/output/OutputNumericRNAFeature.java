package edu.sysu.pmglab.sdfa.nagf.numeric.output;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.RNAAffectedCalculator;
import edu.sysu.pmglab.sdfa.nagf.reference.RefRNAElement;
import gnu.trove.set.TIntSet;

/**
 * @author Wenjie Peng
 * @create 2024-11-13 23:45
 * @description
 */
public class OutputNumericRNAFeature extends AbstractOutputNumericFeature {
    RefRNAElement refRNAElement;

    @Override
    public boolean acceptOne(List<RefRNAElement> loadedRefRNAElement) {
        if (loadedRefRNAElement.isEmpty()) {
            return false;
        }
        refRNAElement = loadedRefRNAElement.popFirst();
        return true;
    }

    /**
     * check whether the rna has related sdsv and calculate numeric annotation when existing
     *
     * @return true means there are sdsv(s) related about this rna and calculate the numeric annotation, and false means the inversion
     */
    public boolean calcForPopulationVCF() {
        TIntSet updateSampleIndex = refRNAElement.getUpdatedIndex();
        if (updateSampleIndex.isEmpty()) {
            return false;
        }
        refRNAElement.updateNumericValuesForPopulation(RNAAffectedCalculator.getByDefault(rnaAffectedCalculatorName), numericValueListInOutputLine);
        updateSampleIndex.clear();
        return true;
    }

    /**
     * check whether the rna has related sdsv and calculate numeric annotation when existing
     *
     * @return true means there are sdsv(s) related about this rna and calculate the numeric annotation, and false means the inversion
     */
    public boolean calcForMultiVCF() {
        TIntSet updateSampleIndex = refRNAElement.getUpdatedIndex();
        if (updateSampleIndex.isEmpty()) {
            return false;
        }
        refRNAElement.updateNumericValuesForMultiVCF(RNAAffectedCalculator.getByDefault(rnaAffectedCalculatorName), numericValueListInOutputLine);
        updateSampleIndex.clear();
        return true;
    }

    @Override
    protected void writeFeatureInfo(ByteStream cache) {
        cache.write(ASCIIUtility.toASCII(refRNAElement.getNumOfGene(),Constant.CHAR_SET));
        cache.write(Constant.TAB);
        cache.write(ASCIIUtility.toASCII(refRNAElement.getNameOfRNA(),Constant.CHAR_SET));
        cache.write(Constant.TAB);
        cache.write(refRNAElement.isCodingRNA() ? Constant.CODING : Constant.NON_CODING);
        cache.write(Constant.TAB);
    }
}
