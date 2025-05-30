package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.sdfa.command.SDFAProgram;
import org.rosuda.REngine.REngineException;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-28 15:05
 * @description
 */
public class SDFRelatedGeneSearch {
    public static void main(String[] args) throws REngineException, IOException, InterruptedException {
        String cml = "identify -t 10 -d /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/sub_phenotypes/G30/annotation " +
                "-o /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/sub_phenotypes/G30 " +
                "-f /Users/wenjiepeng/Desktop/SDFA_4.0/UKB/sub_phenotypes/GRCh38_latest_genomic.gtf._kggseq_version.txt.ccf " +
                "-gn IRAK2 " +
                "-cn chr3";
        SDFAProgram.main(cml.split(" "));
    }
}
