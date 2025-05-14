package edu.sysu.pmglab.sdfa.annotation.output.convertor;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;

/**
 * @author Wenjie Peng
 * @create 2024-09-09 01:21
 * @description
 */
public interface OutputConvertor {

    default Bytes output(List<IRecord> relatedSourceRecords, int columnRelatedIndex){
        return null;
    }
    default Bytes output(List<IRecord> relatedSourceRecords, IntList columnRelatedIndexes){
        return null;
    }
}
