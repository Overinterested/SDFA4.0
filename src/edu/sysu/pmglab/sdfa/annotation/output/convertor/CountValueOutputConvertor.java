package edu.sysu.pmglab.sdfa.annotation.output.convertor;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;

/**
 * @author Wenjie Peng
 * @create 2024-09-09 02:28
 * @description
 */
public class CountValueOutputConvertor implements OutputConvertor {
    ByteStream cache = new ByteStream();
    @Override
    public Bytes output(List<IRecord> relatedSourceRecords, int columnRelatedIndex) {
        cache.clear();
        cache.write(ASCIIUtility.toASCII(relatedSourceRecords.size()));
        return cache.toBytes();
    }

    @Override
    public Bytes output(List<IRecord> relatedSourceRecords, IntList columnRelatedIndexes) {
        cache.clear();
        cache.write(ASCIIUtility.toASCII(relatedSourceRecords.size()));
        return cache.toBytes();
    }
}
