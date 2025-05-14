package edu.sysu.pmglab.sdfa.annotation.output;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFTable;
import edu.sysu.pmglab.ccf.ReaderOption;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.container.circularqueue.ReusableRecordCircularQueue;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.sdfa.annotation.output.frame.IntervalSourceOutputFrame;
import edu.sysu.pmglab.sdfa.annotation.source.Source;
import edu.sysu.pmglab.sdfa.annotation.source.SourceMeta;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.io.IOException;
import java.util.function.BiFunction;

/**
 * @author Wenjie Peng
 * @create 2024-09-10 01:20
 * @description
 */
public class IntervalSourceOutput implements SourceOutput {
    LiveFile file;
    SourceMeta meta;
    CCFReader reader;
    int[] currentRange;
    int[] needLoadPointerRange;
    IntervalSourceOutputFrame frame;
    private Bytes emptyAnnotationResult;
    ReusableRecordCircularQueue reusableCircularQueue;
    BiFunction<ISDSV, IRecord, Boolean> furtherOverlap;

    List<IRecord> tmpRecordList = new List<>();

    protected IntervalSourceOutput() {

    }

    public static IntervalSourceOutput of(Source source) throws IOException {
        IntervalSourceOutput sourceOutput = new IntervalSourceOutput();
        sourceOutput.file = source.getFile();
        sourceOutput.meta = source.getSourceMeta();
        sourceOutput.furtherOverlap = source.getFurtherOverlap();
        sourceOutput.frame = ((IntervalSourceOutputFrame) source.getSourceOutputFrame()).optimize();
        sourceOutput.emptyAnnotationResult = sourceOutput.frame.buildEmptyAnnotation();
        sourceOutput.reusableCircularQueue = new ReusableRecordCircularQueue();
        IntList pureLoadIndexes = sourceOutput.frame.getPureLoadIndexes();
        CCFTable table = new CCFTable(source.getFile());
        ReaderOption readerOption = new ReaderOption(table);
        FieldGroupMetas fields = new FieldGroupMetas(table.getAllFields());

        FieldGroupMetas loadFields = new FieldGroupMetas();
        for (int i = 0; i < pureLoadIndexes.size(); i++) {
            loadFields.addFields(fields.getField((pureLoadIndexes.fastGet(i))));
        }
        readerOption = new ReaderOption(new CCFTable(source.getFile()), loadFields);
        for (int i = 0; i < pureLoadIndexes.size(); i++) {
            readerOption.addFields(fields.getField(pureLoadIndexes.get(i)).fullName());
        }
        sourceOutput.reader = new CCFReader(readerOption);
        sourceOutput.reader.close();
        return sourceOutput;
    }

    public boolean accept(IntInterval range) throws IOException {
        int startPointer = range.start();
        if (startPointer == -1) {
            return false;
        }
        int endPointer = range.end();
        reader = new CCFReader(reader.getReaderOption());
        if (startPointer >= reader.tell()) {
            reusableCircularQueue.clear();
            reader.seek(startPointer);
        }
        reader.limit(range.start(), endPointer + 1);
        IRecord record;
        while ((record = reader.read()) != null) {
            reusableCircularQueue.enqueue(record);
        }
        return true;
    }

    public boolean writeAnnotation(ISDSV sdsv, ByteStream cache) {
        int size = reusableCircularQueue.size();
        boolean existAnnotation = false;
        List<IRecord> records = new List<>(size);
        if (furtherOverlap == null) {
            existAnnotation = true;
            records = new List<>();
            for (int i = 0; i < size; i++) {
                records.add(reusableCircularQueue.fastGet(i));
            }
        } else {
            for (int i = 0; i < size; i++) {
                IRecord sourceRecord = reusableCircularQueue.fastGet(i);
                Boolean overlap = furtherOverlap.apply(sdsv, sourceRecord);
                if (overlap) {
                    existAnnotation = true;
                    records.add(sourceRecord);
                }
            }
        }
        if (existAnnotation) {
            frame.write(records, cache);
        }
        if (!existAnnotation) {
            cache.write(emptyAnnotationResult);
        }
        return existAnnotation;
    }

    public Bytes getEmptyAnnotationResult() {
        return emptyAnnotationResult;
    }


    @Override
    public void expand(int startPointer, int endPointer) {
        if (this.needLoadPointerRange == null) {
            needLoadPointerRange = new int[]{startPointer, endPointer};
            return;
        }
        needLoadPointerRange[1] = Math.max(needLoadPointerRange[1], endPointer);
    }

    @Override
    public void mapPointer() throws IOException {
        if (needLoadPointerRange == null) {
            return;
        }
        IRecord record = reader.getRecord();
        if (currentRange == null || needLoadPointerRange[0] > currentRange[1]) {
            // all loaded data will be dropped
            reader = new CCFReader(reader.getReaderOption());
            reader.limit(needLoadPointerRange[0], needLoadPointerRange[1] + 1);
            reusableCircularQueue.clear();
            int numOfNeedLoadedRecords = needLoadPointerRange[1] - needLoadPointerRange[0] + 1;
            for (int i = 0; i < numOfNeedLoadedRecords; i++) {
                reader.read(record);
                reusableCircularQueue.enqueue(record);
            }
            reader.close();
            if (currentRange == null) {
                currentRange = new int[2];
            }
            currentRange[0] = needLoadPointerRange[0];
            currentRange[1] = needLoadPointerRange[1];
            needLoadPointerRange = null;
            return;
        }
        if (needLoadPointerRange[1] <= currentRange[1]) {
            // loaded data can be reused
            // first drop unused loaded data
            for (int i = 0; i < needLoadPointerRange[0] - currentRange[0]; i++) {
                reusableCircularQueue.dequeue();
            }
        } else {
            // first drop partial loaded data
            for (int i = 0; i < needLoadPointerRange[0] - currentRange[0]; i++) {
                reusableCircularQueue.dequeue();
            }
            // load new data
            reader = new CCFReader(reader.getReaderOption());
            reader.limit(currentRange[1] + 1, needLoadPointerRange[1] + 1);
            int numOfNeedLoadedSize = needLoadPointerRange[1] - currentRange[1];
            for (int i = 0; i < numOfNeedLoadedSize; i++) {
                reader.read(record);
                reusableCircularQueue.enqueue(record);
            }
            reader.close();
            currentRange[1] = needLoadPointerRange[1];
        }
        currentRange[0] = needLoadPointerRange[0];
        needLoadPointerRange = null;
    }

    public int numOfNeededRecords() {
        if (needLoadPointerRange == null) {
            return currentRange == null ? 0 : currentRange[1] - currentRange[0] + 1;
        }
        if (currentRange == null) {
            return needLoadPointerRange[1] - needLoadPointerRange[0] + 1;
        }
        return Math.max(needLoadPointerRange[1], currentRange[1]) - needLoadPointerRange[0] + 1;
    }

    @Override
    public boolean writeAnnotation(ISDSV sdsv, ByteStream cache, int startPointer, int endPointer) {
        if (startPointer == -1) {
            cache.write(emptyAnnotationResult);
            return false;
        }
        int numOfDropRecords = startPointer - currentRange[0];
        int numOfRelatedRecords = endPointer - startPointer + 1;
        tmpRecordList.clear();
        if (furtherOverlap == null) {
            for (int i = 0; i < numOfDropRecords; i++) {
                reusableCircularQueue.dequeue();
                currentRange[0] = currentRange[0] + 1;
            }
            for (int i = 0; i < numOfRelatedRecords; i++) {
                tmpRecordList.add(reusableCircularQueue.fastGet(i));
            }
        } else {
            for (int i = 0; i < numOfDropRecords; i++) {
                reusableCircularQueue.dequeue();
                currentRange[0] = currentRange[0] + 1;
            }
            for (int i = 0; i < numOfRelatedRecords; i++) {
                IRecord sourceRecord = reusableCircularQueue.fastGet(i);
                Boolean overlap = furtherOverlap.apply(sdsv, sourceRecord);
                if (overlap) {
                    tmpRecordList.add(sourceRecord);
                }
            }
        }
        if (!tmpRecordList.isEmpty()) {
            frame.write(tmpRecordList, cache);
            return true;
        } else {
            cache.write(emptyAnnotationResult);
            return false;
        }
    }

    @Override
    public Bytes getHeader() {
        return frame.getHeader();
    }
}

