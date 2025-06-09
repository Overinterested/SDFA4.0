package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.container.intervaltree.inttree.IntIntervalTree;
import edu.sysu.pmglab.container.list.List;

/**
 * @author Wenjie Peng
 * @create 2025-06-06 03:33
 * @description
 */
public class Test {
    public static void main(String[] args) {
        IntIntervalTree.Builder<Integer> integerBuilder = new IntIntervalTree.Builder<>();
        integerBuilder.add(0,10,1);
        IntIntervalTree<Integer> build = integerBuilder.build();
        List<Integer> overlaps = build.getOverlaps(1, 1);
        System.out.println(overlaps.size());
    }
}
