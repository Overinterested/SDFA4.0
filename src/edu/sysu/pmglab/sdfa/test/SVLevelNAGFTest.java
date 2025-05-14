package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.sdfa.nagf.NAGFProgram;

/**
 * @author Wenjie Peng
 * @create 2024-11-15 20:02
 * @description
 */
public class SVLevelNAGFTest {
    public static void main(String[] args) throws Exception {
        String command = "--sv-mode --gene-level --rna-batch 1000 -t 4 " +
                "-dir /Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar " +
                "-o /Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar/output " +
                "--genome-file /Users/wenjiepeng/Desktop/SDFA3.0/annotation/kggseqv1.1_hg38_refGene.txt.ccf";
        NAGFProgram.main(command.split(" "));
    }
}
