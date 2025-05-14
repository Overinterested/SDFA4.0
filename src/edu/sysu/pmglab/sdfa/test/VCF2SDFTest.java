package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.sdfa.command.SDFAProgram;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-10-29 02:15
 * @description
 */
@FinishTest
public class VCF2SDFTest {
    public static void main(String[] args) throws IOException {
        String multiParserCommand = "vcf2sdf -dir " +
                "/Users/wenjiepeng/Desktop/SDFA3.0/test/vcf2sdf/UKBB " +
                "--filter-gty-null --filter-size 20,1000000 " +
//                "--filter-info sv.get(\"PRECISE\")!=null " +
                "-o /Users/wenjiepeng/Desktop/SDFA3.0/test/vcf2sdf/UKBB";

        SDFAProgram.main(multiParserCommand.split(" "));

//        String ukbbParser = "vcf2sdf -dir " +
//                "/Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/private/UKBB/chr1 " +
//                "-t 2 "+
//                "-o /Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/private/UKBB/chr1_sdf";

//        SDFAProgram.main(ukbbParser.split(" "));
    }

}
