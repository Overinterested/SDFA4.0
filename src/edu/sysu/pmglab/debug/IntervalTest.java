package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.record.BoxRecord;
import edu.sysu.pmglab.ccf.toolkit.Sorter;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.container.interval.IntInterval;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-09 08:20
 * @description
 */
public class IntervalTest {
    public static void main(String[] args) throws IOException {
        CCFWriter instance = CCFWriter.setOutput("/Users/wenjiepeng/Desktop/SDFA_4.0/test/writer/1.ccf")
                .addField("A@1", FieldType.int32List)
                .instance();
        BoxRecord record = instance.getRecord();
        // 1
        record.set(0, new int[]{1,2});
        instance.write(record);
        // 2
        record.set(0, new int[]{0,2});
        instance.write(record);
        instance.close();

        Sorter.setInput("/Users/wenjiepeng/Desktop/SDFA_4.0/test/writer/1.ccf")
                .getTagFrom(r -> r.get(0))
                .getValueFrom(r -> new IntInterval(r.get(1), r.get(2)));
    }
}
