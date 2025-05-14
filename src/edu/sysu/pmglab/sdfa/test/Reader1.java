package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.ccf.meta.CCFMeta;
import edu.sysu.pmglab.sdfa.SDFReader;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-03-05 13:36
 * @description
 */
public class Reader1 {
    public static void main(String[] args) throws IOException {
        SDFReader reader = new SDFReader("/Users/wenjiepeng/Desktop/SDFA3.0/nagf/data/Glaucoma/sniffles2/drop_null_gty_AND_precise_AND_max_size_1e6/sdf_4.0/sdf/DM19A0555-1.vcf.gz.sdf");
        CCFMeta meta = reader.getReaderOption().getSDFTable().getMeta();
        int i = 1;
    }
}
