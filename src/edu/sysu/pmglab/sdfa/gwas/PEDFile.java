package edu.sysu.pmglab.sdfa.gwas;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.indexable.NamedSet;
import edu.sysu.pmglab.container.list.ByteList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.sdfa.SDFReader;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2025-03-08 09:40
 * @description this file must contain the following 6 columns at least:
 * 1. Family ID
 * 2. Individual ID
 * 3. Paternal ID: Father ID
 * 4. Maternal ID: Mother ID
 * 5. Sex: 1=male;2=female;0=unknown
 * 6. Phenotype: 0=unknown;1=unaffected;2=affected
 */
public class PEDFile {
    boolean dichotomousPhenotype = false;
    NamedSet<PEDItem> items = new NamedSet<>();
    private BytesSplitter splitter = new BytesSplitter(Constant.TAB);

    private PEDFile() {

    }

    public static PEDFile load(String pedFile) throws IOException {
        PEDFile instance = new PEDFile();
        LiveFile file = LiveFile.of(pedFile);
        ReaderStream readerStream = file.openAsText();
        ByteStream cache = new ByteStream();
        boolean dichotomousPhenotype = true;
        while (readerStream.readline(cache) != -1) {
            Bytes line = cache.toBytes().trim();
            if (line.length() == 0 || line.byteAt(0) == Constant.NUMBER_SIGN) {
                cache.clear();
                continue;
            }
            PEDItem pedItem = new PEDItem(line, instance);
            if (!pedItem.phenotype.equals(Constant.ONE) && !pedItem.phenotype.equals(Constant.TWO)) {
                dichotomousPhenotype = false;
            }
            if (instance.items.contains(pedItem.iid.toString())){
                instance.items.add(pedItem);
            }else {
            instance.items.add(pedItem, pedItem.iid.toString());}
            cache.clear();
        }
        instance.dichotomousPhenotype = dichotomousPhenotype;
        return instance;
    }

    public static class PEDItem {
        Bytes fid;
        Bytes iid;
        // father id
        Bytes pid;
        // mother id
        Bytes mid;
        byte sex;
        Bytes phenotype;

        public Bytes getIid() {
            return iid;
        }

        public Bytes getPhenotype() {
            return phenotype;
        }

        private PEDItem(Bytes pedLine, PEDFile instance) {
            instance.splitter.init(pedLine);
            int count = 0;
            while (instance.splitter.hasNext()) {
                Bytes item = instance.splitter.next();
                switch (count++) {
                    case 0:
                        fid = item.detach();
                        break;
                    case 1:
                        iid = item.detach();
                        break;
                    case 2:
                        pid = item.detach();
                        break;
                    case 3:
                        mid = item.detach();
                        break;
                    case 4:
                        sex = item.detach().toByte();
                        break;
                    case 5:
                        phenotype = item.detach();
                        break;
                    default:
                        break;
                }
            }
        }

        public void writeTo(ByteStream cache) {
            cache.write(fid);
            cache.write(Constant.TAB);
            cache.write(iid);
            cache.write(Constant.TAB);
            cache.write(pid);
            cache.write(Constant.TAB);
            cache.write(mid);
            cache.write(Constant.TAB);
            cache.putByte(sex);
            cache.write(Constant.TAB);
            cache.write(phenotype);
            cache.write(Constant.NEWLINE);
        }

    }

    public int size() {
        return items.size();
    }

    public Bytes getUIDByIndex(int index) {
        return items.valueOf(index).iid;
    }

    public PEDItem valueOf(int index) {
        return items.valueOf(index);
    }

    public boolean exist(String iid) {
        return items.valueOf(iid) == null;
    }

    public static class PEDEasyProducer {

        public static File produce(List<Bytes> iid, ByteList phenotype, String outputFile) throws IOException {
            ByteStream cache = new ByteStream();
            File res = new File(outputFile);
            WriterStream writerStream = new WriterStream(res, WriterStream.Option.DEFAULT);
            if (iid.size() != phenotype.size()) {
                throw new UnsupportedEncodingException("Sample size doesn't match phenotype size");
            }
            for (int i = 0; i < iid.size(); i++) {
                // fid
                cache.write(Constant.PERIOD);
                cache.write(Constant.TAB);
                // iid
                cache.write(iid.fastGet(i));
                cache.write(Constant.TAB);
                // pid
                cache.write(Constant.PERIOD);
                cache.write(Constant.TAB);
                // mid
                cache.write(Constant.PERIOD);
                cache.write(Constant.TAB);
                // sex
                cache.write(Constant.PERIOD);
                cache.write(Constant.TAB);
                // phenotype
                cache.writeChar(phenotype.fastGet(i));
                cache.write(Constant.NEWLINE);
                writerStream.write(cache.toBytes());
                cache.clear();
            }
            cache.clear();
            writerStream.close();
            return res;
        }

        public static File produceWithNGAAHeaderAndPEDFile(String ngaaFile, String pedFile, String outputFile) throws IOException {
            ReaderStream readerStream = LiveFile.of(ngaaFile).openAsText();
            Bytes header = readerStream.readline();
            boolean needCheck = true;
            List<Bytes> iids = new List<>();

            Iterator<Bytes> iterator = header.split(Constant.TAB);
            while (iterator.hasNext()) {
                Bytes item = iterator.next();
                if (needCheck) {
                    String string = item.toString();
                    if (string.startsWith("IsCoding")) {
                        needCheck = false;
                    }
                    continue;
                }
                iids.add(item.detach());
            }
            readerStream.close();
            PEDFile load = PEDFile.load(pedFile);
            NamedSet<PEDItem> items = load.items;
            WriterStream writerStream = new WriterStream(new File(outputFile), WriterStream.Option.DEFAULT);
            for (int i = 0; i < iids.size(); i++) {
                Bytes tmpIID = iids.fastGet(i);
                for (PEDItem item : items) {
                    if (tmpIID.toString().contains(item.iid.toString())){
                        writerStream.write(ASCIIUtility.toASCII(item.toString(),Constant.CHAR_SET));
                        break;
                    }
                }
            }
            writerStream.close();
            return new File(outputFile);
        }

        public static File produceByNGAAHeader(String ngaaFile, String phenotypeFile, String outputFile) throws IOException {
            ReaderStream readerStream = LiveFile.of(ngaaFile).openAsText();
            Bytes header = readerStream.readline();
            boolean needCheck = true;
            List<Bytes> iids = new List<>();
            Iterator<Bytes> iterator = header.split(Constant.TAB);
            while (iterator.hasNext()) {
                Bytes item = iterator.next();
                if (needCheck) {
                    String string = item.toString();
                    if (string.startsWith("IsCoding")) {
                        needCheck = false;
                    }
                    continue;
                }
                iids.add(item.detach());
            }
            readerStream.close();

            ByteList phenotypes = new ByteList();
            readerStream = LiveFile.of(phenotypeFile).openAsText();
            ByteStream cache = new ByteStream();
            while (readerStream.readline(cache) != -1) {
                phenotypes.add(cache.toBytes().toByte());
                cache.clear();
            }
            readerStream.close();
            return produce(iids, phenotypes, outputFile);
        }
    }

    public void rebuild(String sdfFile, String outputFile) throws IOException {
        SDFReader sdfReader = new SDFReader(sdfFile);
        LinkedSet<String> individuals = sdfReader.getReaderOption().getSDFTable().getIndividuals();
        sdfReader.close();
        ByteStream cache = new ByteStream();
        WriterStream writerStream = new WriterStream(new File(outputFile), WriterStream.Option.DEFAULT);
        for (PEDItem item : items) {
            if (individuals.indexOf(item.iid.toString()) != -1) {
                item.writeTo(cache);
                writerStream.write(cache.toBytes());
                cache.clear();
            }
        }
        writerStream.close();
    }
}
