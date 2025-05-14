package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.indexable.IndexableSet;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2024-12-07 02:45
 * @description
 */
public class NAGI {

    public static void singleGeneMap() throws IOException {
        ByteStream cache = new ByteStream();
        ByteStream cache1 = new ByteStream();
        LiveFile file = LiveFile.of("/Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar/output/unified.annot");
        ReaderStream readerStream = file.openAsText();
        readerStream.readline(cache);
        ReaderStream readerStream1 = LiveFile.of("/Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar/output/all_phenotype.txt").openAsText();
        WriterStream writerStream = new WriterStream(new File("/Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar/output/gene_count.txt"), WriterStream.Option.DEFAULT);
        cache.clear();
        TIntIntHashMap geneCountToSVCount = new TIntIntHashMap();
        IndexableSet<Bytes> phenotypes = new LinkedSet<>();
        TIntObjectHashMap<IntList> svToPhenotypeCount = new TIntObjectHashMap<>();
        while (readerStream.readline(cache) != -1) {
            readerStream1.readline(cache1);
            List<Bytes> items = new List<>();
            Iterator<Bytes> iterator = cache.toBytes().split(Constant.TAB);
            while (iterator.hasNext()) items.add(iterator.next().detach());
            Bytes values = items.lastGet(0);
            if (values.length() == 0) {
                cache.clear();
                cache1.clear();
                continue;
            }
            iterator = values.split(Constant.SEMICOLON);
            List<Bytes> valueItem = new List<>();
            while (iterator.hasNext()) valueItem.add(iterator.next().detach());
            if (valueItem.fastLastGet(0).length() == 0) {
                valueItem = valueItem.subList(0, valueItem.size() - 1);
            }
            int size = valueItem.size();
            // phenotype count
            Bytes pheno = cache1.toBytes().detach();
            phenotypes.add(pheno);
            int indexOfPheno = phenotypes.indexOf(pheno);
            IntList intList = svToPhenotypeCount.get(size);
            if (intList == null) {
                intList = new IntList();
                svToPhenotypeCount.put(size, intList);
            }
            intList.add(indexOfPheno);
            // gene count
            if (geneCountToSVCount.containsKey(size)) {
                geneCountToSVCount.put(size, geneCountToSVCount.get(size) + 1);
            } else {
                geneCountToSVCount.put(size, 1);
            }
            cache.clear();
            cache1.clear();
        }
        for (int i : geneCountToSVCount._set) {
            cache.write(ASCIIUtility.toASCII(i));
            cache.write(Constant.TAB);
            cache.write(ASCIIUtility.toASCII(geneCountToSVCount.get(i)));
            cache.write(Constant.NEWLINE);
        }
        writerStream.write(cache.toBytes());
        writerStream.close();
        WriterStream writerStream1 = new WriterStream(
                new File("/Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar/output/gene_count_phenotype_count.txt"), WriterStream.Option.DEFAULT);
        int phenotypeSize = phenotypes.size();
        for (int i : svToPhenotypeCount._set) {
            if (svToPhenotypeCount.containsKey(i)) {
                // gene count
                cache1.write(ASCIIUtility.toASCII(i));
                cache1.write(Constant.TAB);
                IntList intList = svToPhenotypeCount.get(i);
                int[] tmpPheno = new int[phenotypeSize];
                for (int j = 0; j < intList.size(); j++) {
                    tmpPheno[intList.fastGet(j)]++;
                }
                cache1.write(ASCIIUtility.toASCII(tmpPheno[0]));
                cache1.write(Constant.TAB);
                cache1.write(ASCIIUtility.toASCII(tmpPheno[2]));
                cache1.write(Constant.NEWLINE);
            }
        }
        writerStream1.write(cache1.toBytes());
        writerStream1.close();
    }


    public static void singleGeneFilter() throws IOException {
        Bytes fullZeros = new Bytes("0,0,0,0,0,0,0");
        LiveFile file = LiveFile.of("/Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar/output/unified.annot");
        ReaderStream readerStream = file.openAsText();
        WriterStream writerStream = new WriterStream(new File("/Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar/output/single_gene.txt"), WriterStream.Option.DEFAULT);
        ByteStream cache = new ByteStream();
        int count = 0;
        while (readerStream.readline(cache) != -1) {
            if (cache.toBytes().byteAt(0) == Constant.NUMBER_SIGN) {
                cache.clear();
                continue;
            }
            List<Bytes> items = new List<>();
            Iterator<Bytes> iterator = cache.toBytes().split(Constant.TAB);
            while (iterator.hasNext()) items.add(iterator.next().detach());

            Bytes values = items.fastGet(6);
            if (values.length() == 0) {
                cache.clear();
                continue;
            }
            List<Bytes> split = new List<>();
            Iterator<Bytes> iterator1 = values.split(Constant.SEMICOLON);
            while (iterator1.hasNext()) split.add(iterator1.next().detach());

            if (split.size() == 2) {
                iterator1 = split.fastGet(0).split(Constant.COLON);
                List<Bytes> split1 = new List<>();
                while (iterator1.hasNext()) split1.add(iterator1.next().detach());

                count++;
                if (split1.fastGet(1).endsWith(fullZeros)) {
                    count--;
                    cache.clear();
                    continue;
                }
                writerStream.write(cache.toBytes());
                writerStream.write(Constant.NEWLINE);
            }
            cache.clear();
        }
        readerStream.close();
        writerStream.close();
        System.out.println(count);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        singleGeneMap();
//        singleGeneFilter();
        System.out.println(1);
        Thread.sleep(100000000);
        SVContig contig = SVContig.init();
        ByteStream cache = new ByteStream();
        TIntObjectHashMap<List<SVElement>> chrSVMap = new TIntObjectHashMap<>();
        LiveFile file = LiveFile.of("/Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar/output/unified.annot");
        ReaderStream readerStream = file.openAsText();
        readerStream.readline(cache);
        cache.clear();
        while (readerStream.readline(cache) != -1) {
            List<Bytes> items = new List<>();
            Iterator<Bytes> iterator = cache.toBytes().split(Constant.TAB);
            while (iterator.hasNext()) items.add(iterator.next().detach());

            int index = contig.getContigIndexByName(items.fastGet(0).toString());
            List<SVElement> svElements = chrSVMap.get(index);
            if (svElements == null) {
                svElements = new List<>();
                chrSVMap.put(index, svElements);
            }
            svElements.add(SVElement.parse(items));
            cache.clear();
        }
        readerStream.close();
        WriterStream phenotype = new WriterStream(new File("/Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar/output/all_phenotype.txt"), WriterStream.Option.DEFAULT);
        CCFReader reader = new CCFReader("/Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar/supporting_variants_for_nstd102.txt.sdf");
        IRecord record = reader.getRecord();
        while (reader.read(record)) {
            IntList coordinate = record.get(0);
//            SVTypeSign typeSign = SVTypeSign.getByName((Bytes) record.get(2));
            List<SVElement> svElements = chrSVMap.get(coordinate.fastGet(0));
            if (svElements == null || svElements.isEmpty()) {
                continue;
            }
            for (int i = 0; i < svElements.size(); i++) {
                SVElement svElement = svElements.fastGet(i);
                if (svElement.pos == coordinate.fastGet(1) && svElement.end == coordinate.fastGet(2)) {
                    List<Bytes> infos = record.get(10);
                    phenotype.write(infos.fastGet(3));
                    phenotype.write(Constant.NEWLINE);
                    break;
                }
            }
        }
        reader.close();
        phenotype.close();
    }

    static class SVElement {
        int pos;
        int end;
        SVTypeSign typeSign;

        public SVElement(int pos, int end, SVTypeSign typeSign) {
            this.pos = pos;
            this.end = end;
            this.typeSign = typeSign;
        }

        public static SVElement parse(List<Bytes> items) {
            int pos = items.fastGet(1).toInt();
            int end = items.fastGet(2).toInt();
            SVTypeSign svTypeSign = SVTypeSign.getByName(items.fastGet(3));
            return new SVElement(pos, end, svTypeSign);
        }
    }
}
