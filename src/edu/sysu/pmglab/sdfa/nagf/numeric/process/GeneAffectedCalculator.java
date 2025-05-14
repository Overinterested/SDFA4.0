package edu.sysu.pmglab.sdfa.nagf.numeric.process;

import java.util.HashMap;

/**
 * @author Wenjie Peng
 * @create 2024-09-26 02:08
 * @description
 */
public interface GeneAffectedCalculator {
    HashMap<String, GeneAffectedCalculator> cache = new HashMap<>();

    // get max numeric value from sub rna numeric values
    GeneAffectedCalculator DEFAULT_CALCULATOR = (currGeneAffectedNumericValues, toBeMergedSubRNAAffectedNumericValues) -> {
        for (int j = 0; j < currGeneAffectedNumericValues.length; j++) {
            currGeneAffectedNumericValues[j] = Math.max(currGeneAffectedNumericValues[j], toBeMergedSubRNAAffectedNumericValues[j]);
        }
    };

    void merge(float[] currGeneAffectedNumericValues, float[] toBeMergedSubRNAAffectedNumericValues);

    static GeneAffectedCalculator getByDefault(String name) {
        if (name == null) {
            return DEFAULT_CALCULATOR;
        }
        return cache.getOrDefault(name, DEFAULT_CALCULATOR);
    }
}
