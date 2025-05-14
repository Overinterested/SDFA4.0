package edu.sysu.pmglab.sdfa.annotation.output.convertor;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;

/**
 * @author Wenjie Peng
 * @create 2024-09-09 02:36
 * @description
 */
public class UniqueValueCountOutputConvertor implements OutputConvertor {
    IntList counts = new IntList();
    ByteStream cache = new ByteStream();
    LinkedSet<Object> keys = new LinkedSet<>();

    @Override
    public Bytes output(List<IRecord> relatedSourceRecords, int columnRelatedIndex) {
        keys.clear();
        cache.clear();
        counts.clear();
        for (IRecord record : relatedSourceRecords) {
            Object value = record.get(columnRelatedIndex);
            int index = keys.indexOf(value);
            if (index == -1) {
                counts.add(1);
                keys.add(value);
            } else {
                counts.fastSet(index, counts.fastGet(index) + 1);
            }
        }
        int size = keys.size();
        for (int i = 0; i < size; i++) {
            cache.write(ASCIIUtility.toASCII(keys.valueOf(i).toString(),Constant.CHAR_SET));
            cache.write(Constant.LEFT_BRACE);
            cache.write(ASCIIUtility.toASCII(counts.get(i)));
            cache.write(Constant.RIGHT_BRACE);
            if (i != size - 1) {
                cache.write(Constant.SEMICOLON);
            }
        }
        return cache.toBytes();
    }

    @Override
    public Bytes output(List<IRecord> relatedSourceRecords, IntList columnRelatedIndexes) {
        return output(relatedSourceRecords, columnRelatedIndexes.fastGet(0));
    }
}
