package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-04-16 06:40
 * @description
 */
public class SDFReaderTest {
    public static void main(String[] args) throws IOException {
        SDFReader reader = new SDFReader("/Users/wenjiepeng/Desktop/SDFA_test/merge/res/sdf/HG01928_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf.sdf");
        ISDSV read = reader.read();
        int a = 1;
    }
}
