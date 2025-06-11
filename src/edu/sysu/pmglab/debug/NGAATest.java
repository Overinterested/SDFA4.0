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
                "--case-dir /Users/wenjiepeng/Desktop/SDFA_4.0/data/Exophthalmos/cuteSV/sdf/raw/sdf " +
                "--control-dir /Users/wenjiepeng/Desktop/SDFA_4.0/data/normal/sdf/raw/sdf " +
                "-acd /Users/wenjiepeng/Desktop/SDFA_4.0/data/analysis/raw/Exophthalmos_normal " +
                "-o /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/test_sdf " +
                "--genome-file /Users/wenjiepeng/Desktop/SDFA_4.0/data/GRCh38_latest_genomic.gtf._kggseq_version.txt.ccf";
        NAGFProgram.main(s.split(" "));
    }
}
