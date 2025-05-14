package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.viewer.CCFViewer;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.toolkit.SDFViewerReader;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-05 15:50
 * @description
 */
public class GuiTest {
    public static void main(String[] args) throws IOException {
        String file = "/Users/wenjiepeng/Desktop/SDFA_4.0/test/annotation/res/sdf/HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf.sdf";
        SDFReader sdfReader = new SDFReader(file);
        IRecord record = sdfReader.readRecord();
        ISDSV read = sdfReader.read();
        ByteStream cache = new ByteStream();
        Bytes vcfRecord = sdfReader.read().toVCFRecord(cache);
        System.out.println(vcfRecord);
        new CCFViewer(new SDFViewerReader(file));
    }
}
