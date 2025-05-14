package edu.sysu.pmglab.sdfa.annotation.source;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFTable;
import edu.sysu.pmglab.ccf.ReaderOption;
import edu.sysu.pmglab.ccf.field.FieldMeta;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.intervaltree.inttree.IntIntervalTree;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.sdfa.annotation.output.frame.IntervalSourceOutputFrame;
import edu.sysu.pmglab.sdfa.annotation.output.frame.SourceOutputFrame;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceIntervalRecord;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceIntervalTree;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRecord;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.SimpleSDSVForAnnotation;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Wenjie Peng
 * @create 2024-09-07 08:55
 * @description
 */
public class IntervalSource implements Source {
    int sourceID;
    final LiveFile file;
    final SourceMeta meta;
    private SourceIntervalTree tree;
    IntervalSourceOutputFrame frame;
    BiFunction<ISDSV, IRecord, Boolean> furtherOverlap;

    public IntervalSource(LiveFile file, SourceMeta meta) {
        this.file = file;
        this.meta = meta;
        this.tree = new SourceIntervalTree();
    }

    @Override
    public LiveFile getFile() {
        return file;
    }


    @Override
    public void buildIntervalTree() throws IOException {
        this.tree = new SourceIntervalTree();
        CCFTable table = new CCFTable(file);
        FieldMeta contigField = table.getField(0);
        FieldMeta posField = table.getField(1);
        FieldMeta endField = table.getField(2);
        CCFReader reader = new CCFReader(new ReaderOption(table, contigField.fullName(), posField.fullName(), endField.fullName()));
        IRecord record = reader.getRecord();
        int indexOfFile = 0;
        while (reader.read(record)) {
            SourceIntervalRecord sourceIntervalRecord = SourceIntervalRecord.loadCoordinate(record).setIndexOfFile(indexOfFile++);
            tree.update(meta.nameOfContig(sourceIntervalRecord.getIndexOfContig()), sourceIntervalRecord);
        }
        reader.close();
        tree.asUnmodified();
    }

    @Override
    public SourceMeta getSourceMeta() {
        return meta;
    }

    @Override
    public SourceOutputFrame getSourceOutputFrame() {
        return frame;
    }

    @Override
    public boolean contain(String contigName) {
        return tree.getChrIntervalTree().containsKey(contigName);
    }

    @Override
    public void annotateContig(List<ISDSV> svs, String contig) {
        IntIntervalTree<SourceRecord> intervalTree = tree.getChrIntervalTree().get(contig);
        int min, max;
        int size = svs.size();
        for (int i = 0; i < size; i++) {
            min = Integer.MAX_VALUE;
            max = Integer.MIN_VALUE;
            SimpleSDSVForAnnotation item = (SimpleSDSVForAnnotation) svs.fastGet(i);
            List<SourceRecord> overlaps = intervalTree.getOverlaps(item.getCoordinateInterval());
            if (overlaps.isEmpty()) {
                continue;
            }
            for (int j = 0; j < overlaps.size(); j++) {
                int indexOfFile = overlaps.get(j).getIndexOfFile();
                min = Math.min(min, indexOfFile);
                max = Math.max(max, indexOfFile);
            }
            item.updateAnnotPointer(sourceID, min, max);
        }
    }

    @Override
    public Bytes encode(List<SourceRecord> relatedSourceRecords) {
        return null;
    }

    @Override
    public List<SourceRecord> getOverlappedSourceRecords(SimpleSDSVForAnnotation sdsv) {
        return null;
    }

    @Override
    public boolean furtherRelated(ISDSV sdsv, IRecord sourceRecord) {
        if (furtherOverlap == null) {
            return true;
        }
        return furtherOverlap.apply(sdsv, sourceRecord);
    }

    @Override
    public Function<IRecord, SourceRecord> getRecordToSourceFunction() {
        return SourceIntervalRecord::load;
    }

    public BiFunction<ISDSV, IRecord, Boolean> getFurtherOverlap() {
        return furtherOverlap;
    }

    public IntervalSource setFurtherOverlap(BiFunction<ISDSV, IRecord, Boolean> furtherOverlap) {
        this.furtherOverlap = furtherOverlap;
        return this;
    }

    @Override
    public int getSourceID() {
        return sourceID;
    }

    @Override
    public IntervalSource setSourceID(int sourceID) {
        this.sourceID = sourceID;
        return this;
    }

    public IntervalSource setFrame(IntervalSourceOutputFrame frame) {
        this.frame = frame;
        return this;
    }
}
