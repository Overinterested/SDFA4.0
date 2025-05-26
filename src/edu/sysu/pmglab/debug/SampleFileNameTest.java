package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.sdfa.command.SDFSampleNameFileNameMapProgram;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-22 05:34
 * @description
 */
public class SampleFileNameTest {
    public static void main(String[] args) throws IOException {
        String s = "sample_file_map -d /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/test_sdf -o /Users/wenjiepeng/Desktop/SDFA_4.0/UKB";
        SDFSampleNameFileNameMapProgram.main(s.split(" "));
    }
}
