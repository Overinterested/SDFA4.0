package edu.sysu.pmglab.test;

import edu.sysu.pmglab.sdfa.command.SDFAProgram;
import edu.sysu.pmglab.test.process.Finish;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-08 09:29
 * @description
 */
@Finish
public class ConcatTest {
    public static void main(String[] args) throws IOException {
        String concatCML = "concat -d /Users/wenjiepeng/Desktop/SDFA_4.0/test/vcf -o /Users/wenjiepeng/Desktop/SDFA_4.0/test/concat/res";
        SDFAProgram.main(concatCML.split(" "));
    }
}
