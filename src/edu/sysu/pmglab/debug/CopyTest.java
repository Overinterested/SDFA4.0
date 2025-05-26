package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.sdfa.command.PedBasedSDFSelectionProgram;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-21 03:23
 * @description
 */
public class CopyTest {
    public static void main(String[] args) throws IOException {
        String cml = "select -f /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/test_sdf/sdf/test.ped " +
                "-d /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/test_sdf/sdf --count " +
                "-o /Users/wenjiepeng/Desktop/SDFA_4.0/UKB";
        PedBasedSDFSelectionProgram.main(cml.split(" "));
    }
}
