package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.toolkit.filter.IFilter;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.intervaltree.inttree.IntIntervalTree;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2025-06-06 03:45
 * @description
 */
public class DenovoSV {
    static SVContig contig = SVContig.init();

    public static void main(String[] args) throws IOException {
        int INS_DISTANCE = 50;
        File dir = new File("/Users/wenjiepeng/Desktop/SDFA_4.0/simulation/bed");
        List<File> files = FileUtils.listFiles(dir, file -> file.getName().endsWith(".bed.gz"));
        ByteStream cache = new ByteStream();
        HashSet<MergedSVCompare.RefSV> refSVHashSet = new HashSet<>();
        IntIntervalTree.Builder<MergedSVCompare.RefSV> builder = new IntIntervalTree.Builder();
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
                MergedSVCompare.RefSV ref = new MergedSVCompare.RefSV(index, type, pos, end);
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
        IntIntervalTree<MergedSVCompare.RefSV> tree = builder.build();

        files = FileUtils.listFiles(
                "/Users/wenjiepeng/Desktop/SDFA_4.0/simulation/vcf",
                file -> file.getName().endsWith(".vcf")
        );
        List<Bytes> lineItems = new List<>(8);
        int denovoCount = 0;
        TObjectIntHashMap<SVTypeSign> allTypes = new TObjectIntHashMap<>(5, 0.05f, 0);
        TObjectIntHashMap<SVTypeSign> true1 = new TObjectIntHashMap<>(5, 0.05f, 0);
        TObjectIntHashMap<SVTypeSign> denova = new TObjectIntHashMap<>(5, 0.05f, 0);
        for (File file : files) {
            cache = new ByteStream();
            ReaderStream readerStream = LiveFile.of(file).openAsText();
            while (readerStream.readline(cache) != -1) {
                Bytes line = cache.toBytes();
                if (line.startsWith(Constant.NUMBER_SIGN)) {
                    cache.clear();
                    continue;
                }
                parseTo(line, lineItems);
                int index = contig.getContigIndexByName(lineItems.fastGet(0).toString());
                int pos = lineItems.fastGet(1).toInt();
                int end = getAttr(lineItems.get(7), "END").toInt();
                end = Math.abs(end);
                SVTypeSign type = SVTypeSign.getByName(getAttr(lineItems.fastGet(7), "SVTYPE").toString());
                allTypes.put(type, allTypes.get(type) + 1);
                List<MergedSVCompare.RefSV> overlaps = tree.getOverlaps(pos, end);
                HashSet<MergedSVCompare.RefSV> contains = new HashSet<>();
                for (int i = 0; i < overlaps.size(); i++) {
                    MergedSVCompare.RefSV ref = overlaps.fastGet(i);
                    if (ref.index == index && ref.type == type) {
                        if (type == SVTypeSign.getByName("INS")) {
                            ref.pos = ref.pos + INS_DISTANCE;
                            ref.end = ref.end - INS_DISTANCE;
                        }
                        if (Math.abs(ref.pos - pos) <= INS_DISTANCE && Math.abs(ref.end - end) <= INS_DISTANCE) {
                            if (contains.contains(ref)) {
                                continue;
                            } else {
                                contains.add(ref);
                                if (type == SVTypeSign.getByName("INS")){
                                    int a = 1;
                                }
                                true1.put(ref.type, true1.get(ref.type) + 1);
                            }
                            continue;
                        } else {
                            denova.put(ref.type, denova.get(ref.type) + 1);
                            denovoCount++;
                        }
                    }
                }
                cache.clear();
            }
            readerStream.close();
        }
        System.out.println(denovoCount);
        System.out.println(denova);
        System.out.println(allTypes);
        System.out.println(true1);

    }

    public static void parseTo(Bytes line, List<Bytes> lineItems) {
        lineItems.clear();
        Iterator<Bytes> iterator = line.split(Constant.TAB);
        for (int i = 0; i < 8; i++) {
            lineItems.add(iterator.next().detach());
        }
    }

    public static Bytes getAttr(Bytes info, String prefix) {
        Bytes bytes = new Bytes(prefix);
        Iterator<Bytes> iterator = info.split(Constant.SEMICOLON);
        while (iterator.hasNext()) {
            Bytes item = iterator.next();
            if (item.startsWith(bytes)) {
                Iterator<Bytes> iterator1 = item.split(Constant.EQUAL);
                iterator1.next();
                return iterator1.next().detach();
            }
        }
        return new Bytes(0);
    }
}
