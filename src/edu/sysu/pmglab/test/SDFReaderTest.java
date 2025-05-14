package edu.sysu.pmglab.test;

import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.test.process.Finish;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-10 08:47
 * @description
 */
@Finish
public class SDFReaderTest {
    public static void main(String[] args) throws IOException {
        CCFReader reader = new CCFReader("/Users/wenjiepeng/Desktop/SDFA_4.0/test/vcf2sdf/simple_with_no_type/sdf/HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf.sdf");
        SDFReader sdfReader = new SDFReader("/Users/wenjiepeng/Desktop/SDFA_4.0/test/vcf2sdf/simple_with_no_type/sdf/HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf.sdf");
        int a = 1;
    }
}
