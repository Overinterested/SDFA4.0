package edu.sysu.pmglab.debug;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.toolkit.filter.IFilter;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.interval.Interval;
import edu.sysu.pmglab.container.intervaltree.generics.IntervalTree;
import edu.sysu.pmglab.container.intervaltree.inttree.IntIntervalTree;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.io.IOException;
import java.sql.Ref;
import java.util.*;

/**
 * @author Wenjie Peng
 * @create 2025-06-05 14:44
 * @description
 */
public class MergedSVCompare {
    static SVContig contig = SVContig.init();

    // "/Users/wenjiepeng/Desktop/SDFA_4.0/simulation/truvari_sv.txt"
    public static void main(String[] args) throws IOException {
        int INS_DISTANCE = 1000;
        File dir = new File("/Users/wenjiepeng/Desktop/SDFA_4.0/simulation/bed");
        List<File> files = FileUtils.listFiles(dir, file -> file.getName().endsWith(".bed.gz"));
        ByteStream cache = new ByteStream();
        HashSet<RefSV> refSVHashSet = new HashSet<>();
        IntIntervalTree.Builder<RefSV> builder = new IntIntervalTree.Builder();
        for (int i = 0; i < files.size(); i++) {
            ReaderStream readerStream = LiveFile.of(files.fastGet(i)).openAsText();
            while (readerStream.readline(cache) != -1) {
                Bytes line = cache.toBytes();
                if (line.startsWith(Constant.NUMBER_SIGN)) {
                    cache.clear();
                    continue;
                }
                Iterator<Bytes> iterator = line.split(Constant.TAB);
                int index = contig.getContigIndexByName(iterator.next().toString());
                int pos = iterator.next().toInt();
                int end = iterator.next().toInt();
                SVTypeSign type = SVTypeSign.getByName(iterator.next());
                RefSV ref = new RefSV(index, type, pos, end);
                if (ref.type == SVTypeSign.getByName("INS")) {
                    ref.pos = pos - INS_DISTANCE;
                    ref.end = end + INS_DISTANCE;
                }
                if (refSVHashSet.contains(ref)) {
                    cache.clear();
                    continue;
                } else {
                    refSVHashSet.add(ref);
                    builder.add(new IntInterval(pos, end), ref);
                }
                cache.clear();
            }
            readerStream.close();
        }
        TObjectIntHashMap<SVTypeSign> allSampleTrueSV = new TObjectIntHashMap<>(5, 0.05f, 0);
        Iterator<RefSV> iterator1 = refSVHashSet.iterator();
        IntIntervalTree<RefSV> tree = builder.build();
        while (iterator1.hasNext()) {
            RefSV next = iterator1.next();
            allSampleTrueSV.put(next.type, allSampleTrueSV.get(next.type) + 1);
        }
        System.out.println(allSampleTrueSV);
        List<String> type = new List<>();
        type.add("truvari");
        type.add("sdfa");
        type.add("svimmer");
        type.add("jasmine");
        type.add("survivor");
        LogBackOptions.init();
        Logger logger = LogBackOptions.getRootLogger();
        for (int j = 0; j < type.size(); j++) {
            String s = type.fastGet(j);
            String vcf = "/Users/wenjiepeng/Desktop/SDFA_4.0/simulation/" + s + "_sv.txt";
            ReaderStream reader = LiveFile.of(vcf).openAsText();
            int count = 0;
            int lines = 0;
            logger.info(s);
            HashSet<RefSV> matched = new HashSet<>();
            TObjectIntHashMap<SVTypeSign> allTrueMergedSVInMergedTool = new TObjectIntHashMap<>(5, 0.05f, 0);
            TObjectIntHashMap<SVTypeSign> allSVInMergedTools = new TObjectIntHashMap<>(5, 0.05f, 0);
            while (reader.readline(cache) != -1) {
                Bytes line = cache.toBytes();
                Iterator<Bytes> iterator = line.split(Constant.TAB);
                iterator.next();
                lines++;
                int index = contig.getContigIndexByName(iterator.next().toString());
                int pos = iterator.next().toInt();
                int end = -1;
                try{
                end = iterator.next().toInt();}catch (Exception e){
                    int a = 1;
                }
                SVTypeSign type1 = SVTypeSign.getByName(iterator.next().toString());
                if (type1 == SVTypeSign.getByName("DUP")){
                    type1 = SVTypeSign.getByName("DUP:TANDEM");
                }
                allSVInMergedTools.put(type1, allSVInMergedTools.get(type1)+1);
                List<RefSV> overlaps = tree.getOverlaps(pos, end);
                boolean flag = false;
                for (int i = 0; i < overlaps.size(); i++) {
                    RefSV refSV = overlaps.fastGet(i);
                    if (refSV.index == index && refSV.type == type1) {
                        if (matched.contains(refSV)) {
                            continue;
                        } else {
                            matched.add(refSV);
                        }
                        if (refSV.type == SVTypeSign.getByName("INS")) {
                            refSV.pos = refSV.pos + INS_DISTANCE;
                            refSV.end = refSV.end - INS_DISTANCE;
                        }
                        if (refSV.type == SVTypeSign.getByName("INS")){
                            if (Math.abs(refSV.pos - pos) <= INS_DISTANCE && Math.abs(refSV.end - end) <= INS_DISTANCE) {
                                count++;
                                int tmpCount = allTrueMergedSVInMergedTool.get(refSV.type);
                                allTrueMergedSVInMergedTool.put(refSV.type, tmpCount + 1);
                                flag = true;
                                refSV.pos = refSV.pos - INS_DISTANCE;
                                refSV.end = refSV.end + INS_DISTANCE;
                                break;
                            }
                            refSV.pos = refSV.pos - INS_DISTANCE;
                            refSV.end = refSV.end + INS_DISTANCE;
                            continue;
                        }else {
                            int a= 1;
                        }
                        if (Math.abs(refSV.pos - pos) <= 50 && Math.abs(refSV.end - end) <= 50) {
                            count++;
                            int tmpCount = allTrueMergedSVInMergedTool.get(refSV.type);
                            allTrueMergedSVInMergedTool.put(refSV.type, tmpCount + 1);
                            flag = true;
                            break;
                        }
                    }

                }
                if (!flag && type1 == SVTypeSign.getByName("INS")){
                    int a = 1;
                }
                cache.clear();
            }
            reader.close();
            System.out.println("All precision:\t"+type.fastGet(j) + "\t" + (float) count / lines);
            System.out.println("All true merged count:\t"+allTrueMergedSVInMergedTool.toString());
            System.out.println("All merged count:\t"+allSVInMergedTools.toString());
            Set<SVTypeSign> svTypeSigns = allSampleTrueSV.keySet();
            Iterator<SVTypeSign> iterator = svTypeSigns.iterator();
            while (iterator.hasNext()){
                SVTypeSign next = iterator.next();
                int i = allTrueMergedSVInMergedTool.get(next);
                int i1 = allSVInMergedTools.get(next);
                System.out.print(next.toString()+"\t:"+((float)i/i1)+";\t");
            }
            System.out.println();
        }
    }

    static class RefSV {
        int index;
        SVTypeSign type;
        int pos;
        int end;

        public RefSV(int index, SVTypeSign type, int pos, int end) {
            this.index = index;
            this.type = type;
            this.pos = pos;
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RefSV refSV = (RefSV) o;
            return index == refSV.index && pos == refSV.pos && end == refSV.end && Objects.equals(type, refSV.type);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new int[]{index, type.getIndex(), pos, end});
        }
    }
}
