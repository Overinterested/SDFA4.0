package edu.sysu.pmglab.sdfa.merge.method;

import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.util.HashMap;

/**
 * @author Wenjie Peng
 * @create 2024-09-26 00:28
 * @description
 */
public interface TwoSSVMerger {
    HashMap<String, TwoSSVMerger> simpleSVMergeCache = new HashMap<>();

    int SSD_DEFAULT_COORDINATE_BIAS = 1000;
    TwoSSVMerger SSD_DEFAULT_MERGER = (var1, var2) -> {
        IntInterval coordinate1 = var1.getCoordinateInterval();
        IntInterval coordinate2 = var2.getCoordinateInterval();
        return Math.abs(coordinate1.start() - coordinate2.start()) <= SSD_DEFAULT_COORDINATE_BIAS &&
                Math.abs(coordinate1.end() - coordinate2.end()) <= SSD_DEFAULT_COORDINATE_BIAS;
    };

    /**
     * here var1 and var2 are in the same chromosome.
     *
     * @param var1 simple sv 1
     * @param var2 simple sv 2
     * @return whether the two can be merged
     */
    boolean merge(ISDSV var1, ISDSV var2);

    static void addSSVMerge(String name, TwoSSVMerger twoSsvMerger) {
        simpleSVMergeCache.put(name, twoSsvMerger);
    }

    static TwoSSVMerger getByDefault(String typeOfSSV) {
        TwoSSVMerger twoSsvMerger = simpleSVMergeCache.get(typeOfSSV);
        return twoSsvMerger == null ? SSD_DEFAULT_MERGER : twoSsvMerger;
    }
}
