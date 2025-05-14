package edu.sysu.pmglab.sdfa.annotation.source;

import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.sdfa.annotation.AnnotateTask;
import edu.sysu.pmglab.sdfa.annotation.output.frame.IntervalSourceOutputFrame;
import edu.sysu.pmglab.sdfa.annotation.output.frame.SourceOutputFrame;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRecord;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.SimpleSDSVForAnnotation;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Wenjie Peng
 * @create 2024-09-07 07:12
 * @description
 */
public interface Source extends AnnotateTask {
    IntList NULL_ANNOTATION = IntList.wrap(-1, -1);

    int getSourceID();

    LiveFile getFile();

    SourceMeta getSourceMeta();

    SourceOutputFrame getSourceOutputFrame();

    void buildIntervalTree() throws IOException;

    BiFunction<ISDSV, IRecord, Boolean> getFurtherOverlap();

    Source setFurtherOverlap(BiFunction<ISDSV, IRecord, Boolean> furtherOverlapFunc);

    List<SourceRecord> getOverlappedSourceRecords(SimpleSDSVForAnnotation sdsv);

    boolean furtherRelated(ISDSV sdsv, IRecord record);

    Function<IRecord, SourceRecord> getRecordToSourceFunction();

    Source setSourceID(int sourceID);

    Source setFrame(IntervalSourceOutputFrame frame);
}
