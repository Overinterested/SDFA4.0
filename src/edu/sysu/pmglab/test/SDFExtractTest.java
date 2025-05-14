package edu.sysu.pmglab.test;

import edu.sysu.pmglab.sdfa.command.SDFAProgram;
import edu.sysu.pmglab.sdfa.gwas.PEDFile;
import edu.sysu.pmglab.test.process.Finish;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-09 17:51
 * @description
 */
@Finish
public class SDFExtractTest {
    public static void main(String[] args) throws IOException {
        String extractCML = "extract";
//                "-d /Users/wenjiepeng/Desktop/SDFA_4.0/test/extract/data/sdf " +
//                "-o /Users/wenjiepeng/Desktop/SDFA_4.0/test/extract/data " +
//                "--ped /Users/wenjiepeng/Desktop/SDFA_4.0/test/extract/data/ped.ped";
        SDFAProgram.main(extractCML.split(" "));
    }
}
