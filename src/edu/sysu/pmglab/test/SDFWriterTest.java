package edu.sysu.pmglab.test;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.sdfa.SDFWriter;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import edu.sysu.pmglab.test.process.Finish;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-09 18:41
 * @description
 */
@Finish
public class SDFWriterTest {
    public static void main(String[] args) throws IOException, InterruptedException {
//        new CCFViewer(new SDFViewerReader("/Users/wenjiepeng/Desktop/tmp/yg/1.sdf"));
//        Thread.sleep(10000);
        String[] names = new String[1000];
        for (int i = 0; i < 1000; i++) {
            names[i] = String.valueOf(i);
        }
        SDFWriter writer1 = SDFWriter.SDFWriterBuild.of(new File("/Users/wenjiepeng/Desktop/tmp/yg/1.sdf"))
                .addFormat("GT")
                .addFormat("AD")
                .addInfoKeys("PRECISE", "READS_SUPPORT")
                .addIndividuals(names)
                .build();
        SDFWriter.SDFWriterRecord item = writer1.getTemplateSV();
        item.setInfo("PRECISE", "true")
//                .setInfo("READS_SUPPORT", "3")
                .setAlt(new Bytes("ACGAGGGCCCCAA"))
                .setChrName("chr1")
                .setType(SVTypeSign.getByName("DEL"))
                .setID(new Bytes("ID_0"))
                .setRef(new Bytes("ACGAGGGCCCCAA"))
                .setPos(1000)
                .setEnd(2000)
                .setLength(1000)
                .setQuality(new Bytes("."))
                .setFilter(new Bytes("PASS"));
        for (int i = 0; i < 1000; i++) {
            item.addInitFormatAttrs(i, "1/1;3,2");
        }
        writer1.write(item);
        writer1.write(item.setPos(999));
        writer1.write(item.setPos(1001));
        writer1.close();
    }
}
