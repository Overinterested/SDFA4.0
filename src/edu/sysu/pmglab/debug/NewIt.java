package edu.sysu.pmglab.debug;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Random;

/**
 * @author Wenjie Peng
 * @create 2025-06-16 09:41
 * @description
 */
public class NewIt {
    public static void main(String[] args) {
        TIntSet a = new TIntHashSet(100);
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            a.add(random.nextInt(1000));
        }

    }
}
