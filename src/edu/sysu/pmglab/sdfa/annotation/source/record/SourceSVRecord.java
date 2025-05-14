package edu.sysu.pmglab.sdfa.annotation.source.record;

import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.List;

/**
 * @author Wenjie Peng
 * @create 2024-09-07 07:38
 * @description
 */
public class SourceSVRecord extends SourceIntervalRecord {
    int len;
    int type;

    @Override
    public int getLen() {
        return len;
    }

    @Override
    public int typeOfRecordInSV() {
        return type;
    }

    public static SourceSVRecord loadCoordinate(IRecord record){
        SourceSVRecord returns = new SourceSVRecord();
        returns.indexOfContig = record.get(0);
        returns.coordinate = new IntInterval(record.get(1), record.get(2));
        returns.len = record.get(3);
        returns.type = record.get(4);
        return returns;
    }

    public static SourceSVRecord loadRecord(IRecord record, int startIndex) {
        List<Object> items = new List<>(record.size() - startIndex);
        for (int i = startIndex; i < record.size(); i++) {
            items.set(i, record.get(i));
        }
        return new SourceSVRecord().setRecord(items);
    }
    public static SourceSVRecord load(IRecord record){
        SourceSVRecord returns = new SourceSVRecord();
        returns.indexOfContig = record.get(0);
        returns.coordinate = new IntInterval(record.get(1), record.get(2));
        returns.type = record.get(3);
        returns.len = record.get(4);
        List<Object> properties = new List<>();
        for (int i = 5; i < record.size(); i++) {
            properties.add(record.get(i));
        }
        returns.record = properties;
        return returns;
    }
    public SourceSVRecord setRecord(List<Object> record) {
        this.record = record;
        return this;
    }
    @Override
    public SourceSVRecord setIndexOfFile(int indexOfFile) {
        this.indexOfFile = indexOfFile;
        return this;
    }
}
