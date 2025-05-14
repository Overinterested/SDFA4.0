package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.sdfa.command.SDFExtractProgram;

/**
 * @author Wenjie Peng
 * @create 2025-03-12 06:47
 * @description
 */
public class ExtractTest {
    public static void main(String[] args) {
        String extractParams = "--ped /Users/wenjiepeng/Desktop/SDFA/ukbb_disease/F32_fam.ped " +
                "-d /Users/wenjiepeng/Desktop/SDFA/ukbb_disease/concat "+
                "-o /Users/wenjiepeng/Desktop/SDFA/ukbb_disease/F32 " +
                "--min-maf 0.05";
        SDFExtractProgram.main(extractParams.split(" "));
    }
}
