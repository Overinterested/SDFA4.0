package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.sdfa.command.PedBasedSDFSelectionProgram;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.IOException;
import java.util.Random;

/**
 * @author Wenjie Peng
 * @create 2025-05-21 03:23
 * @description
 */
public class CopyTest {
    public static void main(String[] args) throws IOException {
        TIntSet a = new TIntHashSet();
        Random random = new Random(1);
        for (int i = 0; i < 10; i++) {
            a.add(random.nextInt(1000));
        }
        a.add(1);
        a.add(213);
        a.add(141);
        TIntIterator iterator = a.iterator();
        while (iterator.hasNext()){
            int next = iterator.next();
            System.out.println(next);
        }
    }
}
