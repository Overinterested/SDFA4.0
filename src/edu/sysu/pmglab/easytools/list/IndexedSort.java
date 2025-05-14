package edu.sysu.pmglab.easytools.list;

import java.util.Comparator;

/**
 * @author Wenjie Peng
 * @create 2025-05-06 06:38
 * @description
 */
public class IndexedSort {
    int[] sort;
    int[] rawOrder;

    public static IndexedSort sort(int[] array){
        IndexedSort result = new IndexedSort();
        int n = array.length;
        Pair[] pairs = new Pair[n];
        for (int i = 0; i < n; i++) {
            pairs[i] = new Pair(array[i], i);
        }
        java.util.Arrays.sort(pairs, Comparator.comparingInt(a -> a.value));
        result.sort = new int[n];
        result.rawOrder = new int[n];
        for (int i = 0; i < n; i++) {
            result.sort[i] = pairs[i].value;
            result.rawOrder[i] = pairs[i].index;
        }
        return result;
    }

    private static class Pair{
        int value;
        int index;

        Pair(int value, int index) {
            this.value = value;
            this.index = index;
        }
    }
}
