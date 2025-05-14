package edu.sysu.pmglab.sdfa.annotation.preprocess;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.field.FieldMeta;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.record.Record;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.easytools.annotation.genome.prefix.RefSeqGTFParser;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.file.LocalFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.sdfa.annotation.source.GenomeSource;
import edu.sysu.pmglab.sdfa.annotation.source.SourceMeta;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

/**
 * @author Wenjie Peng
 * @create 2024-09-07 20:26
 * @description
 */
public abstract class GenomeSourceConvertor implements SourceConvertor {
    protected LiveFile file;
    protected SourceMeta meta;
    protected SVContig contig;
    protected boolean storeHeader;
    protected final File outputDir;
    protected List<Bytes> header = new List<>();
    protected TIntIntHashMap contigIndexCount = new TIntIntHashMap();
    TIntObjectHashMap<HashMap<String, Integer>> contigGeneCountMap = new TIntObjectHashMap<>();

    private static final Comparator<IRecord> comparator = (o1, o2) -> {
        int status = Integer.compare(o1.get(0), o2.get(0));
        return status == 0 ? ((IntInterval) o1.get(6)).compareTo(o2.get(6)) : status;
    };

    protected static final FieldGroupMetas GENOME_METAS = new FieldGroupMetas(List.wrap(new FieldMeta[]{
            FieldMeta.of("Chr::index", FieldType.varInt32),
            FieldMeta.of("Gene::name", FieldType.string),
            FieldMeta.of("Gene::numOfRNA", FieldType.varInt32),
            FieldMeta.of("RNA::index", FieldType.varInt32),
            FieldMeta.of("RNA::name", FieldType.string),
            FieldMeta.of("RNA::strand", FieldType.varInt32),
            FieldMeta.of("RNA::range", FieldType.intInterval),
            FieldMeta.of("RNA::codingRange", FieldType.intInterval),
            FieldMeta.of("RNA::exons", FieldType.int32List),
    }));
    private static final IRecord record = new Record(GENOME_METAS);

    public GenomeSourceConvertor(LiveFile file, File outputDir) {
        this.file = file;
        this.outputDir = outputDir;
        this.contig = SVContig.init();
    }

    protected abstract IRecord covertLineToRecord(List<Bytes> lineItems, IRecord record);

    @Override
    public GenomeSource convert() throws IOException {
        String name = file.getName();
        if (name.endsWith(".ccf")) {
            // converted file
            CCFReader reader = new CCFReader(file);
            meta = SourceMeta.load(reader.getTable().getMeta());
            reader.close();
            return new GenomeSource(file, meta);
        }
        // initial
        int indexOfFile = 0;
        ByteStream cache = new ByteStream();
        ReaderStream readerStream = file.openAsText();
        while (readerStream.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            if (line.byteAt(0) == Constant.NUMBER_SIGN) {
                // header
                if (storeHeader) {
                    header.add(cache.toBytes().detach());
                }
                indexOfFile++;
                cache.clear();
                continue;
            }
            break;
        }
        List<IRecord> records = new List<>();
        File outputFile = new File(outputDir.getPath() + File.separator + file.getName() + ".ccf");
        CCFWriter writer = CCFWriter.setOutput(outputFile).addFields(GENOME_METAS).instance();
        IRecord temRecord = writer.getRecord();
        List<Bytes> line = new List<>();
        BytesSplitter splitter = new BytesSplitter(Constant.TAB);
        do {
            line.clear();
            splitter.init(cache.toBytes());
            while (splitter.hasNext()) line.add(splitter.next().detach());
            try {
                IRecord record = covertLineToRecord(line, temRecord.clone());
                records.add(record);
                int indexOfContig = record.get(0);
                HashMap<String, Integer> geneNameCount = contigGeneCountMap.get(indexOfContig);
                if (geneNameCount == null) {
                    geneNameCount = new HashMap<>();
                    contigGeneCountMap.put(indexOfContig, geneNameCount);
                }
                String tmpGeneName = record.get(1);
                geneNameCount.merge(tmpGeneName, 1, Integer::sum);
            } catch (Exception e) {
                throw new UnsupportedEncodingException("Encounter error when parsing " + indexOfFile + "'th line");
            }
            indexOfFile++;
            cache.clear();
        } while (readerStream.readline(cache) != -1);
        readerStream.close();
        // write data
        records.sort(comparator);
        HashMap<String, Integer> rnaIndexMap = new HashMap<>();
        int contigIndex = 0;
        while (!records.isEmpty()) {
            IRecord popRecord = records.popFirst();
            int tmpContigIndex = popRecord.get(0);
            if (tmpContigIndex != contigIndex) {
                contigGeneCountMap.get(contigIndex).clear();
                rnaIndexMap.clear();
            }
            HashMap<String, Integer> geneNameCount = contigGeneCountMap.get(tmpContigIndex);
            String tmpGeneName = popRecord.get(1);
            Integer count = geneNameCount.get(tmpGeneName);
            if (count == 1) {
                geneNameCount.remove(tmpGeneName);
                popRecord.set(2, count).set(3, 0);
            } else {
                Integer curIndex = rnaIndexMap.get(tmpGeneName);
                if (curIndex == null) {
                    rnaIndexMap.put(tmpGeneName, 0);
                    popRecord.set(2, count).set(3, 0);
                    continue;
                }
                popRecord.set(2, count).set(3, curIndex);
                if (curIndex + 1 == count) {
                    geneNameCount.remove(tmpGeneName);
                    rnaIndexMap.remove(tmpGeneName);
                } else {
                    rnaIndexMap.put(tmpGeneName, curIndex + 1);
                }
            }
            writer.write(popRecord);
        }
        this.meta = new SourceMeta(contig.getContigRanges());
        List<CCFMetaItem> save = (List<CCFMetaItem>) meta.save();
        for (CCFMetaItem ccfMetaItem : save) {
            writer.addMeta(ccfMetaItem);
        }
        writer.close();
        return new GenomeSource(LiveFile.of(outputFile), meta);
    }

    public GenomeSourceConvertor storeHeader(boolean storeHeader) {
        this.storeHeader = storeHeader;
        return this;
    }

    public static class KGGGenomeSourceConvertor extends GenomeSourceConvertor {
        boolean gtfFile = false;

        private KGGGenomeSourceConvertor(LiveFile file, File outputDir) {
            super(file, outputDir);
        }

        public static KGGGenomeSourceConvertor of(LiveFile file, File outputDir) {
            return new KGGGenomeSourceConvertor(file, outputDir);
        }

        @Override
        public IRecord covertLineToRecord(List<Bytes> lineItems, IRecord record) {
            int indexOfContig = contig.getContigIndexByName(lineItems.get(2).toString());

            contigIndexCount.put(indexOfContig, contigIndexCount.get(indexOfContig) + 1);
            String geneName = lineItems.get(12).toString();
            String rnaName = lineItems.get(1).toString();
            byte strand = lineItems.get(3).startsWith(Constant.ADD) ? (byte) 0 : (byte) 1;
            IntInterval range = new IntInterval(lineItems.get(4).toInt(), lineItems.get(5).toInt());
            IntInterval codingRange = new IntInterval(lineItems.get(6).toInt(), lineItems.get(7).toInt());
            // decode exons positions
            IntList exonsStartList = new IntList();
            Iterator<Bytes> exonSplit1 = lineItems.get(9).trim().split(Constant.COMMA);
            while (exonSplit1.hasNext()) {
                Bytes item = exonSplit1.next().trim();
                if (item.length() == 0) {
                    continue;
                }
                exonsStartList.add(item.toInt());
            }

            IntList exonsEndList = new IntList();
            Iterator<Bytes> exonSplit2 = lineItems.get(10).trim().split(Constant.COMMA);
            while (exonSplit2.hasNext()) {
                Bytes item = exonSplit2.next().trim();
                if (item.length() == 0) {
                    continue;
                }
                exonsEndList.add(item.toInt());
            }
            // finish decode
            int[] exonsStart = exonsStartList.toArray();
            int[] exonsEnd = exonsEndList.toArray();
            IntList exons = new IntList(2 * exonsStart.length);
            for (int i = 0; i < 2 * exonsStart.length; i++) {
                if (i % 2 == 0) {
                    exons.set(i, exonsStart[i / 2]);
                } else {
                    exons.set(i, exonsEnd[i / 2]);
                }
            }
            contig.countContigByIndex(indexOfContig);
            return record.set(0, indexOfContig)
                    .set(1, geneName)
                    .set(2, -1)
                    .set(3, -1)
                    .set(4, rnaName)
                    .set(5, strand)
                    .set(6, range)
                    .set(7, codingRange)
                    .set(8, exons);
        }

        @Override
        public GenomeSource convert() throws IOException {
            if (gtfFile) {
                String tmpKggFile = FileUtils.getSubFile(outputDir.toString(), UUID.randomUUID() + ".txt");
                RefSeqGTFParser refSeqGTFParser = new RefSeqGTFParser()
                        .setGtfFile(file)
                        .setOutputKggFile(tmpKggFile);
                refSeqGTFParser.submit();
                String kggVersionFile = FileUtils.changeExtension(file.getPath(), "_kggseq_version.txt", FileUtils.getExtension(file.getPath()));
                new File(tmpKggFile).renameTo(new File(kggVersionFile));
                this.file = LiveFile.of(kggVersionFile);
            }
            return super.convert();
        }

        public KGGGenomeSourceConvertor isGTF() {
            this.gtfFile = true;
            return this;
        }
    }

    public static void main(String[] args) throws IOException {
        new KGGGenomeSourceConvertor(
                LiveFile.of("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/refGene/refGene_hg38_kggseq_v2.txt.gz"),
                new File("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/refGene/tmp")
        ).convert();
    }

}
