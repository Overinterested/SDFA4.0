package edu.sysu.pmglab.sdfa.annotation;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.executor.ITask;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRecord;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SingleFileSDSVManager;

import java.util.Iterator;
import java.util.Set;

/**
 * @author Wenjie Peng
 * @create 2024-09-07 18:48
 * @description
 */
public interface AnnotateTask {
    default List<ITask> annotate(SingleFileSDSVManager fileSDSVManager) {
        SVContig contig = fileSDSVManager.getReader().getReaderOption().getSDFTable().getContig();
        Set<String> contigs = contig.support();
        Iterator<String> iterator = contigs.iterator();
        List<ITask> tasks = new List<>(contigs.size());
        for (int i = 0; i < contigs.size(); i++) {
            String contigName = iterator.next();
            List<ISDSV> svs = fileSDSVManager.getSVsByContig(contigName);
            if (svs != null && !svs.isEmpty() && contain(contigName)) {
                tasks.add((
                        (status, context) -> {
                            annotateContig(svs, contigName);
                        }
                ));
            }
        }
        return tasks;
    }

    boolean contain(String contigName);

    void annotateContig(List<ISDSV> svs, String contig);

    Bytes encode(List<SourceRecord> relatedSourceRecords);
}
