package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.gtb.genome.genotype.container.SparseGenotypes;
import edu.sysu.pmglab.sdfa.gwas.PEDFile;

import java.io.IOException;
import java.util.HashSet;

/**
 * @author Wenjie Peng
 * @create 2025-05-05 09:28
 * @description
 */
public class GtyTest {
    public static void main(String[] args) throws IOException {
        PEDFile load = PEDFile.load("/Users/wenjiepeng/Desktop/SDFA_4.0/UKB/sv_ped/G30_fam.ped");
        HashSet<String> allUidSet = load.getAllUidSet();
        int a = 1;
    }
}
