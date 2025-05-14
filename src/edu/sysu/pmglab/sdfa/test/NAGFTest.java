package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.sdfa.nagf.NAGFProgram;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-10-29 02:15
 * @description
 */
@FinishTest
public class NAGFTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        // population
//        String command = "--population-vcf --gene-level --rna-batch 500 -t 4 " +
//                "-dir /Users/wenjiepeng/Desktop/SDFA/ukbb_disease/F32 " +
//                "-o /Users/wenjiepeng/Desktop/SDFA/ukbb_disease/F32/res " +
//                "--genome-file /Users/wenjiepeng/Desktop/SDFA3.0/nagf/analyze/Exophthalmos_Glaucoma/sniffles/sdf_4.0_analyze/refGene_hg38_kggseq_v2.txt.gz.ccf";
//        NAGFProgram.main(command.split(" "));
        // individual
        String command = "--multiple-vcf --gene-level --rna-batch 500 -t 1 " +
                "-dir /Users/wenjiepeng/Desktop/SDFA3.0/nagf/analyze/Glaucoma_Normal/sniffles2/drop_null_gty_AND_DV_7/all_sdf " +
                "-o /Users/wenjiepeng/Desktop/SDFA3.0/nagf/analyze/Glaucoma_Normal/sniffles2/drop_null_gty_AND_DV_7/res " +
                "--genome-file /Users/wenjiepeng/Desktop/SDFA3.0/nagf/analyze/Exophthalmos_Glaucoma/sniffles/sdf_4.0_analyze/refGene_hg38_kggseq_v2.txt.gz.ccf";
        NAGFProgram.main(command.split(" "));
    }
}