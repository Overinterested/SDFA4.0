package edu.sysu.pmglab.sdfa.merge.method;

import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.util.HashMap;

/**
 * @author Wenjie Peng
 * @create 2024-10-07 02:05
 * @description
 */
public interface MultiSSVMerger {

    HashMap<String, MultiSSVMerger> multiSSVMergerCache = new HashMap<>();
    MultiSSVMerger DEFAULT_MULTI_ORDER_SSV_MERGER = (curr, canBeMergedSDSVList) -> {
        TwoSSVMerger twoSsvMerger = TwoSSVMerger.getByDefault(curr.getNameOfType());
        return twoSsvMerger.merge(curr, canBeMergedSDSVList.fastGet(0));
    };

    boolean merge(ISDSV curr, List<ISDSV> canBeMergedSDSVList);

    static boolean add(String nameOfMerger, MultiSSVMerger merger) {
        MultiSSVMerger multiSSVMerger = multiSSVMergerCache.get(nameOfMerger);
        if (multiSSVMerger == null) {
            multiSSVMergerCache.put(nameOfMerger, merger);
            return true;
        } else {
            return false;
        }
    }

    static MultiSSVMerger getByDefault(String nameOfMerger) {
        return multiSSVMergerCache.getOrDefault(nameOfMerger, DEFAULT_MULTI_ORDER_SSV_MERGER);
    }
}

