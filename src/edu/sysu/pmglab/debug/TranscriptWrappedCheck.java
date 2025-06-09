package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRNARecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Wenjie Peng
 * @create 2025-06-03 08:08
 * @description
 */
public class TranscriptWrappedCheck {
    public static void main(String[] args) throws IOException {
        HashMap<String, long[]> genePointerList = new HashMap<>();
        String file = "/Users/wenjiepeng/Desktop/SDFA_4.0/UKB/sub_phenotypes/GRCh38_latest_genomic.gtf._kggseq_version.txt.ccf";
        CCFReader reader = new CCFReader(file);
        IRecord record = reader.getRecord();
        while (reader.read(record)) {
            SourceRNARecord item = SourceRNARecord.load(record);
            String nameOfGene = item.getNameOfGene();
            long[] range = genePointerList.get(nameOfGene);
            if (range == null) {
                range = new long[]{reader.tell(), reader.tell()};
                genePointerList.put(nameOfGene, range);
            } else {
                range[1] = reader.tell();
            }
        }
        reader.close();
        List<TInterval> list = new ArrayList<>();
        for (long[] value : genePointerList.values()) {
            list.add(new TInterval(value));
        }
        list.sort(TInterval::compareTo);
        // check more records
        long maxAddedRecords = -1;
        for (int i = 0; i < list.size(); i++) {
            TInterval var1 = list.get(i);
            long end = var1.range[1];
            long tmpAddedRecords = var1.range[1] - var1.range[0];
            for (int j = i + 1; j < list.size(); j++) {
                TInterval var2 = list.get(j);
                if (var2.range[0] > end) {
                    tmpAddedRecords += (end - var1.range[1]);
                    break;
                }
                end = var2.range[1];
            }
            maxAddedRecords = Math.max(maxAddedRecords, tmpAddedRecords);
        }
        System.out.println(maxAddedRecords);
    }

    static class TInterval implements Comparable<TInterval> {
        long[] range;

        public TInterval(long[] range) {
            this.range = range;
        }

        @Override
        public int compareTo(TInterval o) {
            int status = Long.compare(range[0], o.range[0]);
            return status == 0 ? Long.compare(range[1], o.range[1]) : status;
        }
    }
}
