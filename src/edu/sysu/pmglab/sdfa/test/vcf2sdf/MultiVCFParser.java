package edu.sysu.pmglab.sdfa.test.vcf2sdf;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.runtimecompiler.JavaCompiler;
import edu.sysu.pmglab.sdfa.command.VCF2SDFProgram;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-11-28 19:55
 * @description
 */
public class MultiVCFParser {
    public static void main(String[] args) throws IOException {
        JavaCompiler.importClass(Bytes.class);
        String vcfDir = "/Users/wenjiepeng/Desktop/tmp/ukb";
        String outputDir = "/Users/wenjiepeng/Desktop/tmp/ukb";
        String multiParserCommand = "-dir " + vcfDir + " " +
                "-t 1 " +
                "--filter-gty-null " +
//                "--filter-gty GQ (!(value.equals(\".\")))&&Integer.parseInt((String)value)>10 " +
//                "--filter-gty DV (!(value.equals(\".\")))&&Integer.parseInt((String)value)>=7 " +
//                "--filter-size 20,100000 " +
//                "--filter-info sv.get(\"SUPPORT\")!=null&&((Bytes)sv.get(\"SUPPORT\")).toInt()>5 " +
                "-o " + outputDir;
//        VCF2SDFProgram.main(singleParserCommand.split(" "));
        VCF2SDFProgram.main(multiParserCommand.split(" "));
    }
}
