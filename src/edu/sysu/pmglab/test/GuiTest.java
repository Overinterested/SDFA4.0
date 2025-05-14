package edu.sysu.pmglab.test;

import edu.sysu.pmglab.sdfa.command.SDFAProgram;
import edu.sysu.pmglab.test.process.Finish;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-08 08:00
 * @description
 */
@Finish
public class GuiTest {
    public static void main(String[] args) throws IOException {
        SDFAProgram.main(
                (
                        "gui -f " +
                                "/Users/wenjiepeng/Desktop/SDFA_4.0/test/vcf2sdf/simple_with_no_type/sdf/HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf.sdf"
                )
                        .split(" "));
    }
}
