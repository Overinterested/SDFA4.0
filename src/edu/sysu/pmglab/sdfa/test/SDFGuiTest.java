package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.sdfa.command.GUIProgram;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-10-29 02:15
 * @description
 */
@FinishTest
public class SDFGuiTest {
    public static void main(String[] args) throws IOException {
        GUIProgram.main(("-f " +
                "/Users/wenjiepeng/Desktop/tmp/ukb/sdf/sample.vcf.sdf"
        ).split(" "));
    }
}
