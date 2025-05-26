package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.sdfa.nagf.NAGFProgram;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-20 08:31
 * @description
 */
public class NGAATest {
    public static void main(String[] args) throws IOException {
        String s = "--multiple-vcf --gene-level -t 10 " +
                "-d /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/test_sdf/sdf " +
                "-o /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/test_sdf " +
                "--genome-file /Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/refGene/GRCh38_latest_genomic.gtf.gz";
        NAGFProgram.main(s.split(" "));
    }
}
