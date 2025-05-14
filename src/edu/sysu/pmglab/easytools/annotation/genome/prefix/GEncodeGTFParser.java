package edu.sysu.pmglab.easytools.annotation.genome.prefix;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.indexable.IndexableSet;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.gtb.genome.coordinate.Chromosome;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.io.writer.WriterStream;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2024-10-30 20:01
 * @description
 */
public class GEncodeGTFParser {
    File gtfFile;
    File outputKggFile;
    private static final byte[] ENSG = "ENSG".getBytes();
    HashSet<Chromosome> storedContigName;
    private final static BytesSplitter semicolonSplit = new BytesSplitter(Constant.SEMICOLON);

    private static ByteStream cache = new ByteStream();
    private static final Bytes EXIST = new Bytes(new byte[]{Constant.PERIOD});

    private static final List<Bytes> geneIDTagSet = new List<>(new Bytes[]{
            new Bytes("gene_id")
    });
    private static final List<Bytes> geneNameTagSet = new List<>(new Bytes[]{
            new Bytes("gene_name")
    });
    private static final List<Bytes> geneTypeTagSet = new List<>(new Bytes[]{
            new Bytes("gene_type")
    });
    private static final List<Bytes> transcriptNameTagSet = new List<>(new Bytes[]{
            new Bytes("transcript_id")
    });
    private static final List<Bytes> hgncTagSet = new List<>(new Bytes[]{
            new Bytes("hgnc_id")
    });

    private static final DynamicIndexableMap<Bytes, Bytes> kvMapInInfo = new DynamicIndexableMap<>();

    static Bytes CMPL_FLAG = new Bytes("cmpl");
    static Bytes UNKNOWN_FLAG = new Bytes("unk");
    static Bytes INCMPL_FLAG = new Bytes("incmpl");
    static IndexableSet<Bytes> indexableTypeSet = new LinkedSet<>(new Bytes[]{
            new Bytes("gene"),
            new Bytes("transcript"),
            new Bytes("start_codon"),
            new Bytes("CDS"),
            new Bytes("exon"),
            new Bytes("stop_codon")
    });

    public void submit() throws IOException {
        ByteStream cache = new ByteStream();
        LiveFile liveFile = LiveFile.of(gtfFile.toString());
        ReaderStream readerStream = liveFile.openAsText();
        ByteStream outputCache = new ByteStream();
        WriterStream writerStream = new WriterStream(new File(outputKggFile.toString()), WriterStream.Option.DEFAULT);

        int count = 0;
        Bytes geneName;
        Bytes contigName;
        Bytes transcriptName;
        while (readerStream.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            if (line.byteAt(0) == Constant.NUMBER_SIGN) {
                cache.clear();
                continue;
            }
            break;
        }
        KggSeqTranscriptRecord record = new KggSeqTranscriptRecord();
        List<KggSeqTranscriptRecord> list = new List<>();
        boolean startGene = true;
        boolean startRNA = true;
        boolean firstRNAInGene = true;
        BytesSplitter tabSplitter = new BytesSplitter(Constant.TAB);
        do {
            Bytes line = cache.toBytes();
            if (line.byteAt(0) == Constant.NUMBER_SIGN) {
                cache.clear();
                continue;
            }

            tabSplitter.init(line);
            // 1 th column: contig name
            contigName = tabSplitter.next().detach();
            // ignore
            tabSplitter.next();
            // 3 th column: genome region type
            int index = indexableTypeSet.indexOf(tabSplitter.next());
            switch (index) {
                case 0:
                    // gene
                    if (!startGene) {
                        if (!record.exonStartPos.isEmpty()) {
                            list.add(record);
                            record = record.retainContigAndGene();
                        }
                    }
                    startGene = false;
                    firstRNAInGene = true;
                    int indexOfContig;
                    String contigStringName = contigName.toString();
                    Chromosome chromosome = Chromosome.get(contigStringName);
                    if (!chromosome.equals(Chromosome.UNKNOWN)) {
                        indexOfContig = chromosome.getIndex();
                    } else {
                        chromosome = Chromosome.get(contigStringName);
                        indexOfContig = chromosome.getIndex();
                    }
                    contigName = new Bytes(chromosome.getName());
                    for (int i = 3; i < 8; i++) {
                        tabSplitter.next();
                    }
                    Bytes info = tabSplitter.next();
                    parse(info);
                    geneName = getGeneName();
                    record.setContigName(contigName).setGeneName(geneName).indexOfContig(indexOfContig);
                    break;
                case 1:
                    // rna
                    if (!startRNA && !firstRNAInGene) {
                        list.add(record);
                        record = record.retainContigAndGene();
                    }
                    startRNA = false;
                    firstRNAInGene = false;
                    // four
                    int pos = tabSplitter.next().toInt();
                    // five
                    int end = tabSplitter.next().toInt();
                    tabSplitter.next();
                    byte strand = tabSplitter.next().startsWith(Constant.ADD) ? (byte) 0 : (byte) 1;
                    tabSplitter.next();
                    parse(tabSplitter.next());
                    transcriptName = kvMapInInfo.get(transcriptNameTagSet.fastGet(0)).detach();
                    int indexOfHGNC = -1;
                    indexOfHGNC = idOfHGNC();
                    try {
                        record.setPos(pos).setEnd(end).setTranscriptName(transcriptName).setIdOfHGNC(indexOfHGNC).setStrand(strand);
                    } catch (Exception e) {
                        System.out.println(count);
                        System.out.println(cache.toBytes());
                        record.setPos(pos).setEnd(end).setTranscriptName(transcriptName).setIdOfHGNC(-1);
                    }
                    break;
                case 2:
                    // start_codon
                    record.setCodingPos(tabSplitter.next().toInt());
                    record.setCodingEnd(tabSplitter.next().toInt());
                    record.startCodon(true);
                    break;
                case 3:
                    // cds
                    record.setCodingPos(tabSplitter.next().toInt());
                    record.setCodingEnd(tabSplitter.next().toInt());
                    break;
                case 4:
                    // exon
                    int exonStart = tabSplitter.next().toInt();
                    int exonEnd = tabSplitter.next().toInt();
                    record.addExon(exonStart, exonEnd);
                    break;
                case 5:
                    // stop_codon
                    int stopCodonStart = tabSplitter.next().toInt();
                    int stopCodonEnd = tabSplitter.next().toInt();
                    record.updateCodingEnd(stopCodonStart);
                    record.updateCodingEnd(stopCodonEnd);
                    record.endCodon(true);
                    break;
                default:
                    // error
                    break;
            }
            count++;
            cache.clear();
        } while (readerStream.readline(cache) != -1);
        list.sort(KggSeqTranscriptRecord::compareTo);
        for (int i = 0; i < list.size(); i++) {
            KggSeqTranscriptRecord tmp = list.fastGet(i);
            Chromosome chromosome = Chromosome.get(tmp.contigName.toString());
            if (!storedContigName.contains(chromosome)) {
                continue;
            }
            list.fastGet(i).writeToCache(outputCache);
            writerStream.write(outputCache.toBytes());
            outputCache.clear();
        }
        readerStream.close();
        writerStream.close();
    }

    /**
     * NCBI's id:   0 column
     * transcriptName: 1st column(note:0 column is index whose mean is unknown)
     * chr: 2nd column
     * strand: 3rd column. it depends on the order of UTR5 and UTR3
     * transcriptStartIndex: 4th
     * transcriptEndIndex: 5th
     * cdsStart: 6th
     * cdsEnd: 7th
     * exonsNum: 8th
     * exonsStart: 9th (it is an array)
     * exonsEnd: 10th (it is an array)
     * score: 11th(function is unknown)
     * geneName: 12th
     * cdsStartStatus:  13th [none,unknown,incomplete,complete]
     * none - no CDS specified
     * unk - unknown if CDS start/end is complete
     * incmpl - CDS start/end is incomplete
     * cmpl - CDS start/end is complete
     * cdsEndStatus: 14th like above
     * exonFrame: 15th [-1,0,1,2] (here we use `.` to replace)
     * -1: the exon is all in UTR region, excluding transcript
     * n = {0,1,2}: when the related exon is joining in transcript, it needs to take latter exon's n's base pair,
     * which can consist correct reading coding frame
     * (note: the latter exon is same with transcript strand, meaning UTR5's exon)
     * seq: 16 th
     */
    static class KggSeqTranscriptRecord implements Comparable<KggSeqTranscriptRecord> {
        int indexOfContig;
        int idOfHGNC;
        Bytes transcriptName;
        Bytes contigName;
        // 0 : +, 1 : -
        byte strand;
        int pos;
        int end;
        // if noncoding, it's equal end pos
        int codingPos = Integer.MAX_VALUE;
        int codingEnd = Integer.MIN_VALUE;
        int exonSize;
        IntList exonStartPos = new IntList();
        IntList exonEndPos = new IntList();
        Bytes geneName;
        boolean startCodon = false, endCodon = false;

        public KggSeqTranscriptRecord() {
        }

        public KggSeqTranscriptRecord setIdOfHGNC(int idOfHGNC) {
            this.idOfHGNC = idOfHGNC;
            return this;
        }

        public KggSeqTranscriptRecord setTranscriptName(Bytes transcriptName) {
            this.transcriptName = transcriptName;
            return this;
        }

        public KggSeqTranscriptRecord setContigName(Bytes contigName) {
            this.contigName = contigName;
            return this;
        }

        public KggSeqTranscriptRecord updateCodingEnd(int coding) {
            this.codingPos = Math.min(coding, codingPos);
            this.codingEnd = Math.max(codingEnd, coding);
            return this;
        }

        public KggSeqTranscriptRecord setStrand(byte strand) {
            this.strand = strand;
            return this;
        }

        public KggSeqTranscriptRecord setPos(int pos) {
            this.pos = pos;
            return this;
        }

        public KggSeqTranscriptRecord setEnd(int end) {
            this.end = end;
            return this;
        }

        public KggSeqTranscriptRecord setCodingPos(int codingPos) {
            this.codingPos = Math.min(codingPos, this.codingPos);
            return this;
        }

        public KggSeqTranscriptRecord setCodingEnd(int codingEnd) {
            this.codingEnd = Math.max(codingEnd, this.codingEnd);
            return this;
        }

        public KggSeqTranscriptRecord setExonSize(int exonSize) {
            this.exonSize = exonSize;
            return this;
        }


        public KggSeqTranscriptRecord setGeneName(Bytes geneName) {
            this.geneName = geneName;
            return this;
        }

        public void addExon(int start, int end) {
            exonStartPos.add(start - 1);
            exonEndPos.add(end);
        }

        public void writeToCache(ByteStream cache) {
            cache.write(ASCIIUtility.toASCII(idOfHGNC));
            cache.write(Constant.TAB);
            cache.write(transcriptName);
            cache.write(Constant.TAB);
            cache.write(contigName);
            cache.write(Constant.TAB);
            cache.write(strand == (byte) 0 ? Constant.ADD : Constant.MINUS);
            cache.write(Constant.TAB);
            cache.write(ASCIIUtility.toASCII(pos - 1));
            cache.write(Constant.TAB);
            cache.write(ASCIIUtility.toASCII(end));
            cache.write(Constant.TAB);
            cache.write(ASCIIUtility.toASCII(codingPos == Integer.MAX_VALUE ? end : codingPos - 1));
            cache.write(Constant.TAB);
            cache.write(ASCIIUtility.toASCII(codingEnd == Integer.MIN_VALUE ? end : codingEnd));
            cache.write(Constant.TAB);
            cache.write(ASCIIUtility.toASCII(exonStartPos.size()));
            cache.write(Constant.TAB);
            exonStartPos.sort();
            writeMultiInt(cache, exonStartPos);
            cache.write(Constant.TAB);
            exonEndPos.sort();
            writeMultiInt(cache, exonEndPos);
            cache.write(Constant.TAB);
            cache.write(ASCIIUtility.toASCII(Constant.ZERO));
            cache.write(Constant.TAB);
            cache.write(geneName);
            cache.write(Constant.TAB);
            cache.write(codingPos == Integer.MAX_VALUE ? UNKNOWN_FLAG : startCodon ? CMPL_FLAG : INCMPL_FLAG);
            cache.write(Constant.TAB);
            cache.write(codingEnd == Integer.MIN_VALUE ? UNKNOWN_FLAG : endCodon ? CMPL_FLAG : INCMPL_FLAG);
            cache.write(Constant.TAB);
            writeMultiInt(cache, exonStartPos.size());
            cache.write(Constant.TAB);
            cache.write(Constant.NEWLINE);
        }

        void writeMultiInt(ByteStream cache, IntList list) {
            if (list.isEmpty()) {
                throw new UnsupportedOperationException("No exons.");
            } else {
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    cache.write(ASCIIUtility.toASCII(list.fastGet(i)));
                    cache.write(Constant.COMMA);
                }
            }
        }

        void writeMultiInt(ByteStream cache, int size) {
            for (int i = 0; i < size; i++) {
                cache.write(Constant.PERIOD);
                cache.write(Constant.COMMA);
            }
        }

        public void clear() {
            codingPos = Integer.MAX_VALUE;
            codingEnd = Integer.MIN_VALUE;
            exonStartPos.clear();
            exonEndPos.clear();
        }

        public KggSeqTranscriptRecord retainContigAndGene() {
            return new KggSeqTranscriptRecord().indexOfContig(indexOfContig)
                    .setContigName(contigName)
                    .setGeneName(geneName);
        }

        public KggSeqTranscriptRecord indexOfContig(int indexOfContig) {
            this.indexOfContig = indexOfContig;
            return this;
        }

        public KggSeqTranscriptRecord startCodon(boolean startCodon) {
            this.startCodon = startCodon;
            return this;
        }

        public KggSeqTranscriptRecord endCodon(boolean endCodon) {
            this.endCodon = endCodon;
            return this;
        }

        @Override
        public int compareTo(KggSeqTranscriptRecord o) {
            int status = Integer.compare(indexOfContig, o.indexOfContig);
            if (status == 0) {
                status = Integer.compare(pos, o.pos);
                if (status == 0) {
                    status = Integer.compare(end, o.end);
                    if (status == 0) {
                        status = Integer.compare(exonStartPos.fastGet(0), o.exonStartPos.fastGet(0));
                    }
                }
            }
            return status;
        }
    }


    public GEncodeGTFParser setGtfFile(Object gtfFile) {
        this.gtfFile = new File(gtfFile.toString());
        return this;
    }

    public GEncodeGTFParser setOutputKggFile(Object outputKggFile) {
        this.outputKggFile = new File(outputKggFile.toString());
        return this;
    }

    public File getOutputKggFile() {
        return outputKggFile;
    }

    public static void setIndexableTypeSet(IndexableSet<Bytes> indexableTypeSet) {
        GEncodeGTFParser.indexableTypeSet = indexableTypeSet;
    }

    public GEncodeGTFParser setStoredContigName(HashSet<Chromosome> storedContigName) {
        this.storedContigName = storedContigName;
        return this;
    }

    public static void main(String[] args) throws IOException {
        Chromosome.get("chrMT").addAlias("NC_012920.1", "MT", "chrM");
        GEncodeGTFParser gEncodeGTFParser = new GEncodeGTFParser().setGtfFile("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/GEncode/gencode.v47.annotation.gtf.gz")
                .setOutputKggFile("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/GEncode/kggseq.txt")
                .setStoredContigName(new HashSet<>(Chromosome.values()));
        gEncodeGTFParser.submit();
    }


    private static void parse(Bytes info) {
        kvMapInInfo.clear();
        semicolonSplit.init(info);
        List<Bytes> attrList = new List<>();
        BytesSplitter blankSplitter = new BytesSplitter(Constant.BLANK);
        while(semicolonSplit.hasNext()){
            Bytes item = semicolonSplit.next().trim();
            if (item.length() == 0) {
                continue;
            }
            attrList.clear();
            blankSplitter.init(item);
            while (blankSplitter.hasNext()){
                attrList.add(blankSplitter.next().detach());
            }
            int size = attrList.size();
            switch (size) {
                case 0:
                    continue;
                case 1:
                    kvMapInInfo.put(attrList.fastGet(0), EXIST);
                    break;
                case 2:
                    Bytes v = attrList.fastGet(1);
                    if (v.startsWith((byte) 34)) {
                        Iterator<Bytes> iterator = v.split((byte) 34);
                        iterator.next();
                        v = iterator.next().detach();
                    }
                    kvMapInInfo.put(attrList.fastGet(0), v);
                    break;
                default:
                    throw new UnsupportedOperationException(attrList.fastGet(0) + " has multiple blanks to split");
            }
        }
    }

    private static Bytes getGeneName() {
        cache.clear();
        Bytes geneID = kvMapInInfo.get(geneIDTagSet.fastGet(0));
        Bytes geneType = kvMapInInfo.get(geneTypeTagSet.fastGet(0));
        Bytes geneNameID = kvMapInInfo.get(geneNameTagSet.fastGet(0));

        assert geneID != null;
        assert geneType != null;
        assert geneNameID != null;

        cache.write(geneID);
        cache.write(Constant.SEMICOLON);
        if (geneNameID.startsWith(ENSG)) {
            cache.write(Constant.PERIOD);
        } else {
            cache.write(geneNameID);
        }
        cache.write(Constant.SEMICOLON);
        cache.write(geneType);
        return cache.toBytes().detach();
    }

    private static int idOfHGNC() {
        Bytes hgncValue = kvMapInInfo.get(hgncTagSet.fastGet(0));
        if (hgncValue != null) {
            Iterator<Bytes> iterator = hgncValue.split(Constant.COLON);
            // ignore first
            iterator.next();
            return iterator.next().toInt();
        }
        return -1;
    }

    public GEncodeGTFParser setGtfFile(File gtfFile) {
        this.gtfFile = gtfFile;
        return this;
    }
}
