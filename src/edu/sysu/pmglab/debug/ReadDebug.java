package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFTable;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.ReaderOption;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.record.BoxRecord;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.SDFReaderOption;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-07 06:14
 * @description
 */
public class ReadDebug {
    public static void main(String[] args) throws IOException {
        FieldGroupMetas metas = new FieldGroupMetas()
                // 6 items
                .addField("A@1", FieldType.varInt32)
                .addField("A@2", FieldType.int32List)
                .addField("B@1", FieldType.bytecode)
                .addField("B@2", FieldType.float8List)
                .addField("C@1", FieldType.bytecodeList)
                .addField("D@1", FieldType.varInt32);
        String file = "/Users/wenjiepeng/Desktop/SDFA_4.0/test/annotation/res/sdf/1.ccf";
        CCFWriter instance = CCFWriter.setOutput("/Users/wenjiepeng/Desktop/SDFA_4.0/test/annotation/res/sdf/1.ccf").addFields(metas).instance();
        BoxRecord record = instance.getRecord();
        record.set(0, 1);
        record.set(1, IntList.wrap(new int[2]));
        instance.write(record);
        instance.close();
        CCFTable.gc();
        CCFReader reader = new CCFReader(new ReaderOption(file, metas.getField(1)));
        reader.close();

        reader = new CCFReader(new ReaderOption(new CCFTable(file, SDFReaderOption.DATA_LOADER), metas));
        IRecord record1 = reader.getRecord();
        record1.set(5, IntList.wrap(new int[2]));
        int a = 1;
    }
}
