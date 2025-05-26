package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.sdfa.command.SDFAProgram;
import org.rosuda.REngine.REngineException;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-21 09:51
 * @description
 */
public class ExtractSamplesTest {
    public static void main(String[] args) throws IOException, REngineException, InterruptedException {
        String s = "extract_samples -d /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/test_sdf/sdf -o /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/test_sdf";
        SDFAProgram.main(s.split(" "));
    }
}
