package edu.sysu.pmglab.sdfa.nagf.numeric.process;

import java.util.HashMap;

/**
 * @author Wenjie Peng
 * @create 2024-09-26 03:22
 * @description
 */
public interface AffectedNumericFilter {
    HashMap<String, AffectedNumericFilter> cache = new HashMap<>();

    boolean filter(float[] affectedNumericValues);

    static AffectedNumericFilter getByDefault(String name) {
        if (name == null) {
            return null;
        }
        return cache.getOrDefault(name, null);
    }

    static void add(String name, AffectedNumericFilter filter){
        if (name == null){
            return;
        }
        cache.put(name, filter);
    }
}
