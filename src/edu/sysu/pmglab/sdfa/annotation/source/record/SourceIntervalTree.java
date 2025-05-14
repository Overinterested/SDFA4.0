package edu.sysu.pmglab.sdfa.annotation.source.record;

import edu.sysu.pmglab.container.intervaltree.inttree.IntIntervalTree;

import java.util.HashMap;

/**
 * @author Wenjie Peng
 * @create 2024-09-07 18:32
 * @description
 */
public class SourceIntervalTree {
    boolean init = false;
    boolean built = false;
    private HashMap<String, IntIntervalTree<SourceRecord>> chrIntervalTree;
    HashMap<String, IntIntervalTree.Builder<SourceRecord>> chrIntervalBuilder;

    public void update(String contig, SourceRecord record) {
        if (built) {
            return;
        }
        if (!init) {
            chrIntervalBuilder = new HashMap<>();
            init = true;
        }
        IntIntervalTree.Builder<SourceRecord> sourceRecordBuilder = chrIntervalBuilder.get(contig);
        if (sourceRecordBuilder == null) {
            sourceRecordBuilder = new IntIntervalTree.Builder<>();
            chrIntervalBuilder.put(contig, sourceRecordBuilder);
        }
        sourceRecordBuilder.add(record.getInterval(), record);
    }

    public HashMap<String, IntIntervalTree<SourceRecord>> getChrIntervalTree() {
        return chrIntervalTree;
    }

    public void asUnmodified() {
        if (chrIntervalTree == null) {
            chrIntervalTree = new HashMap<>();
            for (String contigName : chrIntervalBuilder.keySet()) {
                IntIntervalTree.Builder<SourceRecord> sourceRecordBuilder = chrIntervalBuilder.get(contigName);
                if (sourceRecordBuilder != null) {
                    IntIntervalTree<SourceRecord> intervalTree = sourceRecordBuilder.build();
                    if (intervalTree.size() != 0){
                        chrIntervalTree.put(contigName, intervalTree);
                    }
                }
            }
        }
    }

    public HashMap<String, IntIntervalTree.Builder<SourceRecord>> getChrIntervalBuilder() {
        return chrIntervalBuilder;
    }
}
