package edu.sysu.pmglab.sdfa.nagf.simulation;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.io.writer.WriterStream;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

/**
 * @author Wenjie Peng
 * @create 2024-12-29 02:08
 * @description
 */
public class ExonProducer {
    public static void main(String[] args) throws IOException {
        CCFReader reader = new CCFReader("/Users/wenjiepeng/Desktop/SDFA3.0/nagf/analyze/kggseq_refGene_hg38_v2.ccf");
        HashSet<String> geneNameSet = new HashSet<>();
        TIntIntHashMap rnaCountMap = new TIntIntHashMap();
        IRecord record = reader.getRecord();
        WriterStream writerStream1 = new WriterStream(
                new File("/Users/wenjiepeng/Desktop/SDFA3.0/nagf/simulation/kggseq_refGene_hg38_v2_exon_positions.txt"),
                WriterStream.Option.DEFAULT
        );
        writerStream1.write(ASCIIUtility.toASCII("Starts\tEnds\n",Constant.CHAR_SET));
        IntList position = new IntList();
        IntList end = new IntList();
        while (reader.read(record)) {
            int chrIndex = record.get(0);
            if (chrIndex >= 23) {
                continue;
            }
            String geneName = record.get(1).toString();
            if (geneNameSet.contains(geneName)) {
                continue;
            }
            int numOfRNA = record.get(2);
            if (numOfRNA == 1) {
                position.clear();
                end.clear();
                IntList tmpExons = record.get(8);
                int size = tmpExons.size();
                for (int i = 0; i < size; i+=2) {
                    position.add(tmpExons.fastGet(i));
                    end.add(tmpExons.fastGet(i+1));
                }
                writerStream1.write(ASCIIUtility.toASCII(position.toString(","),Constant.CHAR_SET));
                writerStream1.write(Constant.TAB);
                writerStream1.write(ASCIIUtility.toASCII(end.toString(","),Constant.CHAR_SET));
                writerStream1.write(Constant.NEWLINE);
            }
            boolean contains = rnaCountMap.contains(numOfRNA);
            if (contains) {
                rnaCountMap.put(numOfRNA, rnaCountMap.get(numOfRNA) + 1);
            } else {
                rnaCountMap.put(numOfRNA, 1);
            }
        }
        ByteStream cache = new ByteStream();
        WriterStream writerStream = new WriterStream(
                new File("/Users/wenjiepeng/Desktop/SDFA3.0/nagf/simulation/kggseq_refGene_hg38_v2_rna_count.txt"),
                WriterStream.Option.DEFAULT
        );
        cache.write(ASCIIUtility.toASCII("RNACount\tGeneNum\n",Constant.CHAR_SET));
        int[] keys = rnaCountMap.keys();
        for (int key : keys) {
            int value = rnaCountMap.get(key);
            cache.write(ASCIIUtility.toASCII(key));
            cache.write(Constant.TAB);
            cache.write(ASCIIUtility.toASCII(value));
            cache.write(Constant.NEWLINE);
        }
        writerStream.write(cache.toBytes());
        writerStream.close();
        int a = 1;
    }
}
