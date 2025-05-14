package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.easytools.r.RConnectionPool;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.sdfa.nagf.NAGFProgram;
import edu.sysu.pmglab.sdfa.nagf.analyze.gene.AdaptThresholdRegression;
import edu.sysu.pmglab.sdfa.toolkit.SDFExtract;
import org.rosuda.REngine.REngineException;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-03-15 05:34
 * @description
 */
public class NGAA1 {
    public static void main(String[] args) throws IOException, REngineException, InterruptedException {
        String inputSDF = "xx";
        String pedFile = "xx";
        String outputFile = "xx";

        // extract samples
        SDFExtract.of(inputSDF, pedFile, outputFile).setMinMAF(0.01f).submit();

        // calc pan-annotation
        String outputDir = "xxx";
        String geneFile = "xxx";
        String extractSDFDir = new File(outputFile).getParent();
        String command = "--population-vcf --gene-level --rna-batch 500 -t 1 " +
                "-dir " + extractSDFDir + " " +
                "-o " + outputDir + " " +
                "--genome-file " + geneFile;
        NAGFProgram.main(command.split(" "));
        // calc
        String host = "localhost";
        float maxZeroAF = 0.99f;
        int port = -1;

        RConnectionPool.addConnectionWithLibraries(host, port, "chngpt");
        AdaptThresholdRegression.getInstance()
                .setZeroAFFilter(maxZeroAF);
        AdaptThresholdRegression.analyze(
                FileUtils.getSubFile(outputDir, "gene_numeric_output.txt"),
                "/Users/pwj/Desktop/NGAA_latest/analysis/tmp/support_5/cuteSV2/sniffles_19_res_" + maxZeroAF + "_filter.txt",
                pedFile.toString(),
                "gene"
        );
    }
}
