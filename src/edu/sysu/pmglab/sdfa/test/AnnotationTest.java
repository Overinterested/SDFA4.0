package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.sdfa.command.AnnotationProgram;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-10-29 02:14
 * @description
 */
@FinishTest
public class AnnotationTest {
    public static void main(String[] args) throws IOException {
        int threads = 4;
        File outputDir = new File("/Users/wenjiepeng/Desktop/SDFA3.0/annotation");
        File inputDir = new File("/Users/wenjiepeng/Desktop/SDFA3.0/vcf");
        LiveFile config = LiveFile.of("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/config.txt");
        AnnotationProgram.main(("-t 4 -dir /Users/wenjiepeng/Desktop/SDFA3.0/vcf " +
                "-o /Users/wenjiepeng/Desktop/SDFA3.0/annotation " +
                "--config /Users/wenjiepeng/Desktop/SDFA3.0/annotation/config.txt").split(" "));
    }
}
