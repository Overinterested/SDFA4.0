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
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceIntervalTree;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRecord;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceSVRecord;
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
public class SVSource implements Source {
    int sourceID;
    final LiveFile file;
    final SourceMeta meta;
    private SourceIntervalTree tree;
    IntervalSourceOutputFrame frame;
    private BiFunction<ISDSV, IRecord, Boolean> furtherOverlap;

    public SVSource(LiveFile file, SourceMeta meta) {
        this.file = file;
        this.meta = meta;
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
        FieldMeta lenField = table.getField(3);
        FieldMeta typeField = table.getField(4);
        CCFReader reader = new CCFReader(
                new ReaderOption(
                        table, contigField.fullName(),
                        posField.fullName(), endField.fullName(),
                        lenField.fullName(), typeField.fullName()
                )
        );
        IRecord record = reader.getRecord();
        int indexOfFile = 0;
        while (reader.read(record)) {
            SourceSVRecord svRecord = SourceSVRecord.loadCoordinate(record).setIndexOfFile(indexOfFile++);
            tree.update(meta.nameOfContig(svRecord.getIndexOfContig()), svRecord);
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
    public boolean contain(String contigName) {
        return meta.contigRanges.containsKey(contigName);
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
            if (overlaps.isEmpty()){
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

    public BiFunction<ISDSV, IRecord, Boolean> getFurtherOverlap() {
        return furtherOverlap;
    }

    public SVSource setFurtherOverlap(BiFunction<ISDSV, IRecord, Boolean> furtherOverlap) {
        this.furtherOverlap = furtherOverlap;
        return this;
    }

    @Override
    public Function<IRecord, SourceRecord> getRecordToSourceFunction() {
        return SourceSVRecord::load;
    }

    @Override
    public int getSourceID() {
        return sourceID;
    }

    @Override
    public SVSource setSourceID(int sourceID) {
        this.sourceID = sourceID;
        return this;
    }

    @Override
    public Source setFrame(IntervalSourceOutputFrame frame) {
        this.frame = frame;
        return this;
    }
}
