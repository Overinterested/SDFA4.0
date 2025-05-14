package edu.sysu.pmglab.sdfa.nagf.numeric.process.conversion;

import edu.sysu.pmglab.container.list.FloatList;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.AffectedNumericFilter;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * @author Wenjie Peng
 * @create 2024-10-17 02:14
 * @description
 */
public class AffectedThresholdFilter implements AffectedNumericFilter {

    static int UPSTREAM_INDEX = 0;
    static int UTR5_INDEX = 1;
    static int CODING_EXON_INDEX = 2;
    static int EXON_INDEX = 3;
    static int INTRO_INDEX = 4;
    static int UTR3_INDEX = 5;
    static int DOWNSTREAM_INDEX = 6;
    static FloatList thresholdList = new FloatList();
    static final TObjectIntHashMap<String> regionIndexMap = new TObjectIntHashMap<>();

    static {
        for (int i = 0; i < 7; i++) {
            thresholdList.add(0);
        }
        regionIndexMap.put("upstream", 0);
        regionIndexMap.put("utr5", 1);
        regionIndexMap.put("coding exon", 2);
        regionIndexMap.put("exon", 3);
        regionIndexMap.put("intro", 4);
        regionIndexMap.put("utr3", 5);
        regionIndexMap.put("downstream", 6);
    }

    @Override
    public boolean filter(float[] affectedNumericValues) {
        float diff;
        for (int i = 0; i < affectedNumericValues.length; i++) {
            diff = affectedNumericValues[i] - thresholdList.fastGet(i);
            if (diff < 0) {
                affectedNumericValues[i] = 0;
            }
        }
        return true;
    }

    public static AffectedNumericFilter setRegionThreshold(String name, float threshold) {
        boolean containsKey = regionIndexMap.containsKey(name);
        if (containsKey) {
            int index = regionIndexMap.get(name);
            thresholdList.fastSet(index, threshold);
        }
        throw new UnsupportedOperationException("Please add region names from [upstream, utr5, coding exon, exon, intro, utr3, downstream].");
    }
}
