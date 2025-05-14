package edu.sysu.pmglab.sdfa.annotation.output;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.container.circularqueue.ReusableCircularQueue;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.sdfa.annotation.output.frame.GenomeSourceOutputFrame;
import edu.sysu.pmglab.sdfa.annotation.source.GenomeSource;
import edu.sysu.pmglab.sdfa.annotation.source.SourceMeta;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRNARecord;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-09-18 08:10
 * @description
 */
public class GenomeSourceOutput implements SourceOutput {
    protected LiveFile file;
    protected SourceMeta meta;
    protected CCFReader reader;
    protected int[] currentRange;
    protected int[] needLoadPointerRange;
    protected GenomeSourceOutputFrame frame;
    protected ReusableCircularQueue<SourceRNARecord> reusableCircularQueue;
    protected static final Bytes emptyAnnotationResult = new Bytes(".");
    protected static final Bytes header = new Bytes("Range");

    public GenomeSourceOutput() {

    }

    public static GenomeSourceOutput of(GenomeSource source) throws IOException {
        GenomeSourceOutput sourceOutput = new GenomeSourceOutput();
        sourceOutput.setFile(source.getFile());
        sourceOutput.setMeta(source.getSourceMeta());
        sourceOutput.setReusableCircularQueue(new ReusableCircularQueue<SourceRNARecord>() {
            @Override
            protected void reuseElement(SourceRNARecord element) {
                element.clear();
            }
        });
        sourceOutput.setReader(new CCFReader(sourceOutput.file));
        sourceOutput.setFrame(new GenomeSourceOutputFrame(source.getSourceMeta()));
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
            reusableCircularQueue.enqueue(SourceRNARecord.load(record));
        }
        reader.close();
        return true;
    }

    /**
     * expand file pointer interval which needs to be loaded for annotation
     * @param startPointer
     * @param endPointer
     */
    @Override
    public void expand(int startPointer, int endPointer) {
        if (this.needLoadPointerRange == null) {
            needLoadPointerRange = new int[]{startPointer, endPointer};
            return;
        }
        needLoadPointerRange[1] = Math.max(needLoadPointerRange[1], endPointer);
    }

    /**
     * load records to map the pointers which must be loaded for annotation
     * @throws IOException
     */
    @Override
    public void mapPointer() throws IOException {
        if (needLoadPointerRange == null) {
            return;
        }
        if (currentRange == null || needLoadPointerRange[0] > currentRange[1]) {
            // all loaded data will be dropped
            reader = new CCFReader(reader.getReaderOption());
            reader.limit(needLoadPointerRange[0], needLoadPointerRange[1] + 1);
            reusableCircularQueue.clear();
            int numOfNeedLoadedRecords = needLoadPointerRange[1] - needLoadPointerRange[0] + 1;
            for (int i = 0; i < numOfNeedLoadedRecords; i++) {
                reusableCircularQueue.enqueue(SourceRNARecord.load(reader.read()));
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
                reusableCircularQueue.enqueue(SourceRNARecord.load(reader.read()));
            }
            reader.close();
        }
        currentRange[0] = needLoadPointerRange[0];
        currentRange[1] = needLoadPointerRange[1];
        needLoadPointerRange = null;
    }

    public int numOfNeededRecords() {
        if (needLoadPointerRange == null) {
            return 0;
        }
        return needLoadPointerRange[1] - needLoadPointerRange[0] + 1;
    }

    /**
     * write the annotation results for sdsv
     * @param sdsv
     * @param cache
     * @param startPointer
     * @param endPointer
     * @return
     */
    @Override
    public boolean writeAnnotation(ISDSV sdsv, ByteStream cache, int startPointer, int endPointer) {
        if (startPointer == -1) {
            cache.write(emptyAnnotationResult);
            return false;
        }
        int numOfDropRecords = startPointer - currentRange[0];
        int numOfRelatedRecords = endPointer - startPointer + 1;
        List<SourceRNARecord> records = new List<>(numOfRelatedRecords);
        for (int i = 0; i < numOfDropRecords; i++) {
            reusableCircularQueue.dequeue();
            currentRange[0] = currentRange[0] + 1;
        }
        for (int i = 0; i < numOfRelatedRecords; i++) {
            SourceRNARecord record = reusableCircularQueue.fastGet(i);
            records.add(record);
        }
        frame.write(sdsv, records, 0, numOfRelatedRecords - 1, cache);
        return true;
    }

    public Bytes getEmptyAnnotationResult() {
        return emptyAnnotationResult;
    }

    public GenomeSourceOutput setFile(LiveFile file) {
        this.file = file;
        return this;
    }

    public GenomeSourceOutput setMeta(SourceMeta meta) {
        this.meta = meta;
        return this;
    }

    public GenomeSourceOutput setReader(CCFReader reader) {
        this.reader = reader;
        return this;
    }

    public GenomeSourceOutput setFrame(GenomeSourceOutputFrame frame) {
        this.frame = frame;
        return this;
    }

    public GenomeSourceOutput setReusableCircularQueue(ReusableCircularQueue<SourceRNARecord> reusableCircularQueue) {
        this.reusableCircularQueue = reusableCircularQueue;
        return this;
    }

    @Override
    public Bytes getHeader() {
        return header;
    }
}

