package edu.sysu.pmglab.test;

import edu.sysu.pmglab.sdfa.command.SDFAProgram;
import edu.sysu.pmglab.test.process.Finish;
import edu.sysu.pmglab.test.process.ToDo;
import org.rosuda.REngine.REngineException;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-09 18:01
 * @description
 */
@ToDo("Test the VCF files from different calling type")
public class VCF2SDFTest {
    public static void main(String[] args) throws IOException, REngineException, InterruptedException {
        String vcf2sdfCML = "vcf2sdf -d /Users/wenjiepeng/Desktop/SDFA_4.0/data/Exophthalmos/cuteSV/vcf " +
                "-o /Users/wenjiepeng/Desktop/SDFA_4.0/data/Exophthalmos/cuteSV/sdf/raw -t 4";
        SDFAProgram.main(vcf2sdfCML.split(" "));
    }
}
