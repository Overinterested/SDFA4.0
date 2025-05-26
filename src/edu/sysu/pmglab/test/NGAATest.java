package edu.sysu.pmglab.test;

import edu.sysu.pmglab.sdfa.nagf.NAGFProgram;
import edu.sysu.pmglab.test.process.ToDo;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-09 18:42
 * @description
 */
public class NGAATest {
    public static void main(String[] args) throws IOException {
        System.setProperty("ccf.buffer.size", "1");
        String cml = "-d /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/G30/sdf -o /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/G30/result " +
                "--multiple-vcf -t 4 --genome-file /Users/wenjiepeng/Desktop/SDFA_4.0/test/annotation/res/GRCh38_latest_genomic.gtf._kggseq_version.txt.ccf";
        NAGFProgram.main(cml.split(" "));
    }
}
