package edu.sysu.pmglab.easytools.annotation.genome;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
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
public class RefSeqGTFParser {
    String gtfFile;
    File outputKggFile;
    HashSet<Chromosome> storedContigName;
    static BytesSplitter tabSplit = new BytesSplitter((byte) '\t');
    static BytesSplitter colonSplit = new BytesSplitter((byte) ':');
    static BytesSplitter semicolonSplit = new BytesSplitter((byte) ';');
    static BytesSplitter blankSplit = new BytesSplitter(Constant.BLANK);
    public static Bytes CMPL_FLAG = new Bytes("cmpl");
    public static Bytes UNKNOWN_FLAG = new Bytes("unk");
    public static Bytes INCMPL_FLAG = new Bytes("incmpl");
    public static byte[] HGNC_BYTES = "HGNC".getBytes();
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
        ByteStream outputCache = new ByteStream();
        LiveFile liveFile = LiveFile.of(gtfFile);
        ReaderStream readerStream = liveFile.openAsText();
        WriterStream writerStream = new WriterStream(new File(outputKggFile.toString()), WriterStream.Option.DEFAULT);

        int count = 0;
        Bytes geneName;
        Bytes contigName;
        Bytes transcriptName = null;
        Bytes idOfRNAHGNC;
        List<Bytes> infoItems;
        while (readerStream.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            if (line.byteAt(0) == Constant.NUMBER_SIGN) {
                cache.clear();
                continue;
            }
            break;
        }
        int indexOfHGNC = -1;
        KggSeqTranscriptRecord record = new KggSeqTranscriptRecord();
        List<KggSeqTranscriptRecord> list = new List<>();
        boolean startGene = true;
        boolean startRNA = true;
        boolean firstRNAInGene = true;
        Bytes hgncBytes = null;
        do {
            hgncBytes = null;
            Bytes line = cache.toBytes();
            if (line.byteAt(0) == Constant.NUMBER_SIGN) {
                cache.clear();
                continue;
            }

            List<Bytes> split = new List<>();
            Iterator<Bytes> iterator = line.split((byte) '\t');
            while (iterator.hasNext()) split.add(iterator.next().detach());

            Bytes type = split.fastGet(2).detach();
            int index = indexableTypeSet.indexOf(type);
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
                    contigName = split.fastGet(0).detach();
                    String contigStringName = contigName.toString();
                    Chromosome chromosome = Chromosome.get(contigStringName);
                    if (!chromosome.equals(Chromosome.UNKNOWN)) {
                        indexOfContig = chromosome.getIndex();
                        contigName = new Bytes(chromosome.getName());
                    } else {
                        chromosome = Chromosome.get(contigStringName);
                        indexOfContig = chromosome.getIndex();
                        contigName = new Bytes(chromosome.getName());
                    }
                    Bytes info = split.get(8);
                    blankSplit.init(info);
                    // ignore
                    blankSplit.next();
                    // gene name
                    Bytes geneNameAtrr = blankSplit.next();
                    Iterator<Bytes> attrSplit = geneNameAtrr.split((byte) 34);
                    attrSplit.next();
                    geneName = attrSplit.next().detach();
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
                    int pos = split.fastGet(3).toInt();
                    int end = split.fastGet(4).toInt();
                    byte strand = split.fastGet(6).startsWith(Constant.ADD) ? (byte) 0 : (byte) 1;
                    // region info column: parse `transcriptName` and `hgncBytes`
                    blankSplit.init(split.fastGet(8));
                    int infoCount = 0;
                    while (blankSplit.hasNext()) {
                        Bytes item = blankSplit.next();
                        switch (infoCount++) {
                            case 3:
                                semicolonSplit.init(item);
                                Iterator<Bytes> iterator1 = semicolonSplit.next().split((byte) 34);
                                // ignore
                                iterator1.next();
                                // transcript name
                                transcriptName = iterator1.next().detach();
                                if (transcriptName != null) {
                                    int indexOfVersion = transcriptName.indexOf(Constant.PERIOD);
                                    if (indexOfVersion != -1) {
                                        transcriptName = transcriptName.subBytes(0, indexOfVersion).detach();
                                    }
                                }
                                break;
                            default:
                                if (item.startsWith(HGNC_BYTES)) {
                                    hgncBytes = item.detach();
                                }
                                break;
                        }
                    }
                    // endregion
                    if (transcriptName != null) {
                        try {
                            if (hgncBytes == null) {
                                idOfRNAHGNC = new Bytes("-1");
                            } else {
                                colonSplit.init(hgncBytes);
                                // ignore
                                colonSplit.next();
                                colonSplit.next();
                                // idOfRNAHGNC
                                idOfRNAHGNC = colonSplit.next().split((byte) 34).next().detach();
                            }
                            record.setPos(pos).setEnd(end).setTranscriptName(transcriptName).setIdOfHGNC(idOfRNAHGNC.toInt()).setStrand(strand);
                        } catch (Exception e) {
                            System.out.println(count);
                            System.out.println(cache.toBytes());
                            colonSplit.init(hgncBytes);
                            // ignore
                            colonSplit.next();
                            colonSplit.next();
                            // idOfRNAHGNC
                            idOfRNAHGNC = colonSplit.next().split((byte) 34).next().detach();
                            record.setPos(pos).setEnd(end).setTranscriptName(transcriptName).setIdOfHGNC(idOfRNAHGNC.toInt());
                        }
                    }
                    break;
                case 2:
                    // start_codon
                    record.startCodon(true);
                    break;
                case 3:
                    // cds
                    record.setCodingPos(split.fastGet(3).toInt());
                    record.setCodingEnd(split.fastGet(4).toInt());
                    break;
                case 4:
                    // exon
                    int exonStart = split.fastGet(3).toInt();
                    int exonEnd = split.fastGet(4).toInt();
                    record.addExon(exonStart, exonEnd);
                    break;
                case 5:
                    // stop_codon
                    int stopCodonStart = split.fastGet(3).toInt();
                    int stopCodonEnd = split.fastGet(4).toInt();
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
        boolean startCodon = false;
        boolean endCodon = false;

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

        public KggSeqTranscriptRecord updateCodingEnd(int coding) {
            this.codingPos = Math.min(coding, codingPos);
            this.codingEnd = Math.max(codingEnd, coding);
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
            cache.write('\t');
            cache.write(transcriptName);
            cache.write('\t');
            cache.write(contigName);
            cache.write('\t');
            cache.write(strand == (byte) 0 ? Constant.ADD : Constant.MINUS);
            cache.write('\t');
            cache.write(ASCIIUtility.toASCII(pos - 1));
            cache.write('\t');
            cache.write(ASCIIUtility.toASCII(end));
            cache.write('\t');
            cache.write(ASCIIUtility.toASCII(codingPos == Integer.MAX_VALUE ? end : codingPos - 1));
            cache.write('\t');
            cache.write(ASCIIUtility.toASCII(codingEnd == Integer.MIN_VALUE ? end : codingEnd));
            cache.write('\t');
            cache.write(ASCIIUtility.toASCII(exonStartPos.size()));
            cache.write('\t');
            exonStartPos.sort();
            writeMultiInt(cache, exonStartPos);
            cache.write('\t');
            exonEndPos.sort();
            writeMultiInt(cache, exonEndPos);
            cache.write('\t');
            cache.write(Constant.ZERO);
            cache.write('\t');
            cache.write(geneName);
            cache.write('\t');
            cache.write(codingPos == Integer.MAX_VALUE ? UNKNOWN_FLAG : (startCodon ? CMPL_FLAG : INCMPL_FLAG));
            cache.write('\t');
            cache.write(codingEnd == Integer.MIN_VALUE ? UNKNOWN_FLAG : (endCodon ? CMPL_FLAG : INCMPL_FLAG));
            cache.write('\t');
            writeMultiInt(cache, exonStartPos.size());
            cache.write('\t');
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

        public KggSeqTranscriptRecord startCodon(boolean startCodon) {
            this.startCodon = startCodon;
            return this;
        }

        public KggSeqTranscriptRecord endCodon(boolean endCodon) {
            this.endCodon = endCodon;
            return this;
        }
    }

    public RefSeqGTFParser setGtfFile(Object gtfFile) {
        this.gtfFile = gtfFile.toString();
        return this;
    }

    public RefSeqGTFParser setOutputKggFile(Object outputKggFile) {
        this.outputKggFile = new File(outputKggFile.toString());
        return this;
    }

    public File getOutputKggFile() {
        return outputKggFile;
    }

    public static void setIndexableTypeSet(IndexableSet<Bytes> indexableTypeSet) {
        RefSeqGTFParser.indexableTypeSet = indexableTypeSet;
    }

    public RefSeqGTFParser setStoredContigName(HashSet<Chromosome> storedContigName) {
        this.storedContigName = storedContigName;
        return this;
    }
}
