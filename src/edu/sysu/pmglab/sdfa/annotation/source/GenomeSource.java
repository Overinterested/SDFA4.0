package edu.sysu.pmglab.sdfa.annotation.source;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFTable;
import edu.sysu.pmglab.ccf.ReaderOption;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.intervaltree.inttree.IntIntervalTree;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.sdfa.annotation.output.frame.GenomeSourceOutputFrame;
import edu.sysu.pmglab.sdfa.annotation.output.frame.IntervalSourceOutputFrame;
import edu.sysu.pmglab.sdfa.annotation.output.frame.SourceOutputFrame;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceIntervalTree;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRNARecord;
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
public class GenomeSource implements Source {
    int sourceID;
    final LiveFile file;
    final SourceMeta meta;
    GenomeSourceOutputFrame frame;
    protected SourceIntervalTree tree;
    BiFunction<ISDSV, IRecord, Boolean> furtherOverlap;


    public GenomeSource(LiveFile file, SourceMeta meta) {
        this.file = file;
        this.meta = meta;
        this.tree = new SourceIntervalTree();
    }

    @Override
    public boolean contain(String contigName) {
        return meta.contigRanges.containsKey(contigName);
    }

    @Override
    public void annotateContig(List<ISDSV> svs, String contig) {
        IntIntervalTree<SourceRecord> intervalTree = tree.getChrIntervalTree().get(contig);
        for (int i = 0; i < svs.size(); i++) {
            SimpleSDSVForAnnotation item = (SimpleSDSVForAnnotation) svs.fastGet(i);
            List<SourceRecord> overlaps = intervalTree.getOverlaps(item.getCoordinateInterval());
            int overlapSize = overlaps.size();
            if (overlapSize == 0) {
                continue;
            }
            int pointer;
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int j = 0; j < overlapSize; j++) {
                pointer = overlaps.get(j).getIndexOfFile();
                min = Math.min(pointer, min);
                max = Math.max(pointer, max);
            }
            item.updateAnnotPointer(sourceID, min, max);
        }
    }

    @Override
    public Bytes encode(List<SourceRecord> relatedSourceRecords) {
        return null;
    }

    @Override
    public LiveFile getFile() {
        return file;
    }

    @Override
    public void buildIntervalTree() throws IOException {
        this.tree = new SourceIntervalTree();
        CCFTable table = new CCFTable(file);
        CCFReader reader = new CCFReader(new ReaderOption(table, "Chr::index", "RNA::range"));
        IRecord record = reader.getRecord();
        int indexOfFile = 0;
        while (reader.read(record)) {
            SourceRNARecord sourceRNARecord = SourceRNARecord.loadCoordinateFromPartialReader(record).setIndexOfFile(indexOfFile++);
            tree.update(meta.nameOfContig(sourceRNARecord.getIndexOfContig()), sourceRNARecord);
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

    public BiFunction<ISDSV, IRecord, Boolean> getFurtherOverlap() {
        return furtherOverlap;
    }

    public GenomeSource setFurtherOverlap(BiFunction<ISDSV, IRecord, Boolean> furtherOverlap) {
        this.furtherOverlap = furtherOverlap;
        return this;
    }

    @Override
    public Function<IRecord, SourceRecord> getRecordToSourceFunction() {
        return SourceRNARecord::load;
    }

    @Override
    public int getSourceID() {
        return sourceID;
    }

    @Override
    public GenomeSource setSourceID(int sourceID) {
        this.sourceID = sourceID;
        return this;
    }

    @Override
    public Source setFrame(IntervalSourceOutputFrame frame) {
        return this;
    }

    public NAGFGenomeSource transfer() {
        NAGFGenomeSource returns = new NAGFGenomeSource(file, meta).setSourceID(sourceID);
        returns.tree = tree;
        returns.furtherOverlap = furtherOverlap;
        returns.frame = frame;
        return returns;
    }

    public static class NAGFGenomeSource extends GenomeSource {

        public NAGFGenomeSource(LiveFile file, SourceMeta meta) {
            super(file, meta);
        }

        @Override
        public NAGFGenomeSource setSourceID(int sourceID) {
            super.setSourceID(sourceID);
            return this;
        }
    }
}
