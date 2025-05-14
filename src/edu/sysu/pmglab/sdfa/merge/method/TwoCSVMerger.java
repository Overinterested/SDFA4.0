package edu.sysu.pmglab.sdfa.merge.method;

import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.csv.ComplexSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.util.HashMap;

/**
 * @author Wenjie Peng
 * @create 2024-09-26 00:33
 * @description
 */
public interface TwoCSVMerger {
    int CSV_COORDINATE_DEFAULT_BIAS = 1000;
    HashMap<String, TwoCSVMerger> complexSVMergeCache = new HashMap<>();
    TwoCSVMerger CSV_DEFAULT_MERGER = (var1, var2) -> {
        List<ISDSV> svs1 = var1.getSVs();
        List<ISDSV> svs2 = var2.getSVs();
        int size1 = svs1.size();
        int size2 = svs2.size();
        if (size1 != size2) {
            return false;
        }
        boolean merge;
        for (int i = 0; i < size1; i++) {
            ISDSV sdsv1 = svs1.fastGet(i);
            ISDSV sdsv2 = svs2.fastGet(i);
            if (sdsv1.getType() != sdsv2.getType()){
                return false;
            }
            if (sdsv1.getContigIndex() != sdsv2.getContigIndex()) {
                return false;
            }
            IntInterval coordinate1 = sdsv1.getCoordinateInterval();
            IntInterval coordinate2 = sdsv2.getCoordinateInterval();
            merge = Math.abs(coordinate1.start() - coordinate2.start()) <= CSV_COORDINATE_DEFAULT_BIAS &&
                    Math.abs(coordinate1.end() - coordinate2.end()) <= CSV_COORDINATE_DEFAULT_BIAS;
            if (!merge) {
                return false;
            }
        }
        return true;
    };

    boolean merge(ComplexSV var1, ComplexSV var2);

    static void addCSVMerge(String name, TwoCSVMerger twoCsvMerger) {
        complexSVMergeCache.put(name, twoCsvMerger);
    }

    static TwoCSVMerger getByDefault(String nameOfCSVType) {
        TwoCSVMerger twoCsvMerger = complexSVMergeCache.get(nameOfCSVType);
        return twoCsvMerger == null ? CSV_DEFAULT_MERGER : twoCsvMerger;
    }
}
