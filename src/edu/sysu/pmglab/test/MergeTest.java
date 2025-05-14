package edu.sysu.pmglab.test;

import edu.sysu.pmglab.sdfa.command.SDFAProgram;
import edu.sysu.pmglab.test.process.Finish;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-08 07:38
 * @description
 */
@Finish
public class MergeTest {
    public static void main(String[] args) throws IOException {
        String mergeCML = "merge -t 4 " +
                "-d /Users/wenjiepeng/Desktop/SDFA_4.0/test/vcf " +
                "-o /Users/wenjiepeng/Desktop/SDFA_4.0/test/merge";
        SDFAProgram.main(mergeCML.split(" "));
    }
}
