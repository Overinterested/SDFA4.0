package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.record.BoxRecord;
import edu.sysu.pmglab.container.list.IntList;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-03-17 04:32
 * @description
 */
public class ModifiedChr {
    public static void main(String[] args) throws IOException {
        for (int i = 1; i < 23; i++) {
            String file = "/Volumes/数据卷/sdfa4.0/ukbb/concat/all/chr"+i+"_concat_modified.sdf";
            CCFReader reader = new CCFReader(file);
            CCFWriter writer = CCFWriter.setOutput(new File(file)).addFields( reader.getAllFields()).instance();
            BoxRecord record = reader.getRecord();
            IntList tmp = new IntList(3);
            while(reader.read(record)){
                IntList coordinate = record.get(0);
                tmp.set(0, i-1);
                tmp.set(1, coordinate.fastGet(1));
                tmp.set(2, coordinate.fastGet(2));
                record.set(0, tmp);
                writer.write(record);
            }
            writer.addMeta(reader.getReaderOption().getTable().getMeta());
            writer.close();
        }
    }
}
