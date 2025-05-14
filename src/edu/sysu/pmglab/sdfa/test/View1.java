package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.ccf.viewer.CCFViewer;
import edu.sysu.pmglab.ccf.viewer.CCFViewerReader;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-03-05 13:44
 * @description
 */
public class View1 {
    public static void main(String[] args) throws IOException {
        new CCFViewer(new CCFViewerReader(
                "/Users/wenjiepeng/Desktop/tmp/ukb/sdf/sample.vcf.gz.sdf"
        ));
    }
}
