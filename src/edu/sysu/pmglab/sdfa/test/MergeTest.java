package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.sdfa.command.MergeProgram;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-10-29 02:14
 * @description
 */
@FinishTest
public class MergeTest {
    public static void main(String[] args) throws IOException {
        MergeProgram.main(("-t 1 -dir /Users/wenjiepeng/Desktop/SDFA_test/merge/test_files " +
                "-o /Users/wenjiepeng/Desktop/SDFA_test/merge/res").split(" "));
    }
}
