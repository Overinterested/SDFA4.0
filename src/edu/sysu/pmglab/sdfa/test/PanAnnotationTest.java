package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.easytools.r.RConnectionPool;
import edu.sysu.pmglab.sdfa.nagf.analyze.gene.MultiThreadAdaptThresholdRegression;
import org.rosuda.REngine.REngineException;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-24 07:10
 * @description
 */
public class PanAnnotationTest {

    public static void main(String[] args) throws REngineException, IOException, InterruptedException {

        String pedFile = "/Users/pwj/Documents/SDFA_4.0/ukb/G30/overlapped_G30.ped";
        for (int i = 0; i < 4; i++) {
            RConnectionPool.addConnectionWithLibraries("localhost", 9611+i, "chngpt");
        }

        MultiThreadAdaptThresholdRegression.getInstance().setZeroAFFilter(0.9f).setThread(4);
        MultiThreadAdaptThresholdRegression.analyze(
                "/Users/pwj/Documents/SDFA_4.0/ukb/G30/gene_numeric_output.txt",
                "/Users/pwj/Documents/SDFA_4.0/ukb/G30/res.txt",
                pedFile,
                "gene"
        );
    }
}
