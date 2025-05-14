package edu.sysu.pmglab.sdfa.merge.method;

import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.csv.ComplexSV;

import java.util.HashMap;

/**
 * @author Wenjie Peng
 * @create 2024-10-07 02:53
 * @description
 */
public interface MultiCSVMerger {
    HashMap<String, MultiCSVMerger> multiCSVMergerCache = new HashMap<>();
    MultiCSVMerger DEFAULT_MULTI_ORDER_CSV_MERGER = (curr, canBeMergedSDSVList) -> {
        TwoCSVMerger twoCSVMerger = TwoCSVMerger.getByDefault(curr.getNameOfType());
        return twoCSVMerger.merge(curr, canBeMergedSDSVList.fastGet(0));
    };

    boolean merge(ComplexSV curr, List<ComplexSV> canBeMergedSDSVList);

    static boolean add(String nameOfMerger, MultiCSVMerger merger) {
        MultiCSVMerger multiSSVMerger = multiCSVMergerCache.get(nameOfMerger);
        if (multiSSVMerger == null) {
            multiCSVMergerCache.put(nameOfMerger, merger);
            return true;
        } else {
            return false;
        }
    }

    static MultiCSVMerger getByDefault(String nameOfMerger) {
        return multiCSVMergerCache.getOrDefault(nameOfMerger, DEFAULT_MULTI_ORDER_CSV_MERGER);
    }
}
