package edu.sysu.pmglab.test;

import edu.sysu.pmglab.sdfa.command.SDFAProgram;
import edu.sysu.pmglab.test.process.Finish;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-06 10:07
 * @description
 */
@Finish
public class AnnotationTest {
    public static void main(String[] args) throws IOException {
        String annotationCML = "annotate --config /Users/wenjiepeng/Desktop/SDFA_4.0/test/annotation/data/config.txt " +
                "-t 4 -d /Users/wenjiepeng/Desktop/SDFA_4.0/test/vcf " +
                "-o /Users/wenjiepeng/Desktop/SDFA_4.0/test/annotation/res";
        SDFAProgram.main(annotationCML.split(" "));
    }
}
