package edu.sysu.pmglab.sdfa.annotation.source.record;

import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.List;

/**
 * @author Wenjie Peng
 * @create 2024-09-07 07:37
 * @description
 */
public class SourceIntervalRecord implements SourceRecord {
    int indexOfFile;
    int indexOfContig;
    List<Object> record;
    IntInterval coordinate;


    @Override
    public int getIndexOfFile() {
        return indexOfFile;
    }

    @Override
    public int getIndexOfContig() {
        return indexOfContig;
    }

    @Override
    public SourceIntervalRecord setIndexOfFile(int indexOfFile) {
        this.indexOfFile = indexOfFile;
        return this;
    }

    @Override
    public IntInterval getInterval() {
        return coordinate;
    }

    @Override
    public int getLen() {
        return coordinate.end() - coordinate.start();
    }

    public static SourceIntervalRecord loadCoordinate(IRecord record) {
        SourceIntervalRecord returns = new SourceIntervalRecord();
        returns.indexOfContig = record.get(0);
        returns.coordinate = new IntInterval(record.get(1), record.get(2));
        return returns;
    }

    public static SourceIntervalRecord loadRecord(IRecord record, int startIndex) {
        List<Object> items = new List<>(record.size() - startIndex);
        for (int i = startIndex; i < record.size(); i++) {
            items.set(i, record.get(i));
        }
        return new SourceIntervalRecord().setRecord(items);
    }

    public SourceIntervalRecord setRecord(List<Object> record) {
        this.record = record;
        return this;
    }

    public List<Object> getRecord() {
        return record;
    }

    public static SourceIntervalRecord load(IRecord record){
        SourceIntervalRecord returns = new SourceIntervalRecord();
        returns.indexOfContig = record.get(0);
        returns.coordinate = new IntInterval(record.get(1), record.get(2));
        List<Object> properties = new List<>();
        for (int i = 3; i < record.size(); i++) {
            properties.add(record.get(i));
        }
        returns.record = properties;
        return returns;
    }
}
