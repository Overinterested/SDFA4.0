package edu.sysu.pmglab.sdfa.annotation.output.convertor;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;

/**
 * @author Wenjie Peng
 * @create 2024-09-09 01:32
 * @description
 */
public class ConcatOutputConvertor implements OutputConvertor {
    final byte[] separator;
    byte lineSeparator = Constant.SEMICOLON;
    ByteStream cache = new ByteStream();
    ByteStream oneRecordCache = new ByteStream();

    public ConcatOutputConvertor(String separator) {
        this.separator = separator.getBytes();
    }

    public ConcatOutputConvertor(Bytes separator) {
        this.separator = separator.bytes();
    }

    public ConcatOutputConvertor(byte separator) {
        this.separator = new byte[]{separator};
    }

    @Override
    public Bytes output(List<IRecord> relatedSourceRecords, int columnRelatedIndex) {
        cache.clear();
        int size = relatedSourceRecords.size();
        for (int i = 0; i < size; i++) {
            cache.write(ASCIIUtility.toASCII(relatedSourceRecords.get(i).get(columnRelatedIndex).toString(),Constant.CHAR_SET));
            if (i != size - 1) {
                cache.write(lineSeparator);
            }
        }
        return cache.toBytes();
    }

    @Override
    public Bytes output(List<IRecord> relatedSourceRecords, IntList columnRelatedIndexes) {
        cache.clear();
        oneRecordCache.clear();
        int size = relatedSourceRecords.size();
        int numOfIndexes = columnRelatedIndexes.size();
        for (int i = 0; i < size; i++) {
            IRecord record = relatedSourceRecords.get(i);
            for (int j = 0; j < numOfIndexes; j++) {
                oneRecordCache.write(ASCIIUtility.toASCII(record.get(columnRelatedIndexes.get(j)).toString(),Constant.CHAR_SET));
                if (j != numOfIndexes-1){
                    oneRecordCache.write(separator);
                }
            }
            cache.write(oneRecordCache.toBytes());
            if (i != size - 1) {
                cache.write(lineSeparator);
            }
        }
        return cache.toBytes();
    }
}
