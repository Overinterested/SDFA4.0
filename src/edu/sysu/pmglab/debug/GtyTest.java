package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.gtb.genome.genotype.container.SparseGenotypes;

/**
 * @author Wenjie Peng
 * @create 2025-05-05 09:28
 * @description
 */
public class GtyTest {
    public static void main(String[] args) {
        SparseGenotypes genotypes = new SparseGenotypes(1);
        genotypes.set(0, new Bytes("1/1").toGenotype());
        int a = 1;
    }
}
