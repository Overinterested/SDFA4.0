package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.sdfa.command.SVBasedGWASProgram;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-10-30 00:54
 * @description
 */
public class GWASTest {
    public static void main(String[] args) throws IOException {
        LogBackOptions.init();
        String command = "-d /Users/wenjiepeng/Desktop/SDFA3.0/vcf " +
                "--ped-file /Users/wenjiepeng/Desktop/SDFA3.0/vcf/3_vcf.ped " +
                "-o /Users/wenjiepeng/Desktop/SDFA3.0/vcf/sdf " +
                "-t 2 " +
                "--plink-shell /Users/wenjiepeng/Desktop/SDFA3.0/vcf/plink_shell.sh";
        SVBasedGWASProgram.main(command.split(" "));
    }
}
