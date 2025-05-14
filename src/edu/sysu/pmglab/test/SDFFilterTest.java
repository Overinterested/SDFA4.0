package edu.sysu.pmglab.test;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.sdfa.command.SDFAProgram;
import edu.sysu.pmglab.sdfa.test.SDFGuiTest;
import edu.sysu.pmglab.test.process.Finish;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-09 17:54
 * @description
 */
@Finish
public class SDFFilterTest {
    public static void main(String[] args) throws IOException {
        // see raw SDF:
        SDFAProgram.main("gui -f /Users/wenjiepeng/Desktop/SDFA_4.0/test/vcf2sdf/simple_with_no_type/sdf/HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf.sdf".split(" "));
//        DynamicIndexableMap
        String[] filterCML = new String[]{
                "filter", "-d", "/Users/wenjiepeng/Desktop/SDFA_4.0/test/filter/data", "-o", "/Users/wenjiepeng/Desktop/SDFA_4.0/test/filter/res",
                "--filter-sv", "PRECISE", "value!=null",
                "--filter-gty", "GQ", "(int)value>=100"
        };
        SDFAProgram.main(filterCML);
        SDFAProgram.main("gui -f /Users/wenjiepeng/Desktop/SDFA_4.0/test/filter/res/HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf.sdf".split(" "));
    }
}
