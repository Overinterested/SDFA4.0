package edu.sysu.pmglab.easytools.annotation.genome;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.container.list.LongList;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.gtb.genome.coordinate.Chromosome;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.io.writer.WriterStream;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2024-11-04 00:35
 * @description
 */
public class KggFileSeqFiller {
    private Chromosome currContig;
    private boolean finish = false;
    private Chromosome nextChr = null;
    private ByteStream cache = new ByteStream();
    private LongList contigSize = new LongList();
    static BytesSplitter tabSplitter = new BytesSplitter(Constant.TAB);
    static byte[] unassigned = "unassigned_transcript".getBytes();
    String seqPath;
    String kggFile;
    String resFile;

    public void submit() throws IOException {
        ByteStream cache = new ByteStream();
        ByteStream seqCache = new ByteStream();
        ReaderStream seqReader = LiveFile.of(seqPath).openAsText();
        ReaderStream kggReader = LiveFile.of(kggFile).openAsText();
        WriterStream kggWriter = new WriterStream(new File(resFile), WriterStream.Option.DEFAULT);
        collectOneContig(seqReader, seqCache);
        ByteStream exonSeq = new ByteStream();
        HashMap<Chromosome, List<Bytes>> unmatchedKGGRecords = new HashMap<>();
        int index = 0;
        boolean inverseStrand = false;
        Chromosome currChromosome = null;
        Chromosome chromosome = currContig;
        int codingStart = -1, codingEnd = -1;
        IntList exonStart = null, exonEnd = null;
        while (kggReader.readline(cache) != -1) {
            index++;
            Bytes line = cache.toBytes();
            tabSplitter.init(line);
            int colCount = 0;
            boolean invalid = false;
            while (tabSplitter.hasNext()) {
                Bytes item = tabSplitter.next();
                switch (colCount++) {
                    case 1:
                        if (item.startsWith(unassigned)) invalid = true;
                        break;
                    case 2:
                        String contigName = item.toString();
                        currChromosome = Chromosome.get(contigName);
                        if (currChromosome.equals(Chromosome.UNKNOWN)) {
                            currChromosome = Chromosome.get(contigName);
                        }
                        if (!currChromosome.equals(chromosome)) {
                            List<Bytes> unmatchedRecordsInContig = unmatchedKGGRecords.get(currChromosome);
                            if (unmatchedRecordsInContig == null) {
                                unmatchedRecordsInContig = new List<>();
                                unmatchedKGGRecords.put(currChromosome, unmatchedRecordsInContig);
                            }
                            unmatchedRecordsInContig.add(cache.toBytes().detach());
                            invalid = true;
                        }
                        break;
                    case 3:
                        inverseStrand = item.toString().equals("-");
                        break;
                    case 6:
                        codingStart = item.toInt();
                        break;
                    case 7:
                        codingEnd = item.toInt();
                        break;
                    case 9:
                        exonStart = parse(item);
                        break;
                    case 10:
                        exonEnd = parse(item);
                        break;
                    default:
                        break;
                }
                if (invalid) {
                    break;
                }
            }
            if (invalid) {
                cache.clear();
                exonSeq.clear();
                continue;
            }
            int exonSize = exonEnd.size();
            if (inverseStrand) {
                boolean utr3Appender = exonStart.fastGet(0) > codingStart;
                for (int i = exonSize - 1; i >= 0; i--) {
                    int tmpPos = exonEnd.fastGet(i);
                    int tmpEnd = exonStart.fastGet(i);
                    for (int j = tmpPos - 1; j >= tmpEnd; j--) {
                        byte seq = seqCache.toBytes().byteAt(j);
                        switch (seq) {
                            case Constant.A:
                            case Constant.a:
                                seq = Constant.T;
                                break;
                            case Constant.C:
                            case Constant.c:
                                seq = Constant.G;
                                break;
                            case Constant.G:
                            case Constant.g:
                                seq = Constant.C;
                                break;
                            case Constant.T:
                            case Constant.t:
                                seq = Constant.A;
                                break;
                        }
                        exonSeq.write(seq);
                    }
                    if (i == 0 && utr3Appender) {
                        int firstStart = exonStart.fastGet(0);
                        for (int j = 0; j < 3; j++) {
                            byte seq = seqCache.toBytes().byteAt(firstStart + j);
                            switch (seq) {
                                case Constant.A:
                                case Constant.a:
                                    seq = Constant.T;
                                    break;
                                case Constant.C:
                                case Constant.c:
                                    seq = Constant.G;
                                    break;
                                case Constant.G:
                                case Constant.g:
                                    seq = Constant.C;
                                    break;
                                case Constant.T:
                                case Constant.t:
                                    seq = Constant.A;
                                    break;
                            }
                            exonSeq.write(seq);
                        }
                    }
                }
            } else {
                boolean utr5Appender = exonEnd.fastGet(exonSize - 1) < codingEnd;
                for (int i = 0; i < exonSize; i++) {
                    int tmpPos = exonStart.fastGet(i);
                    int tmpEnd = exonEnd.fastGet(i);
                    for (int j = tmpPos; j < tmpEnd; j++) {
                        byte seq = seqCache.toBytes().byteAt(j);
                        exonSeq.write(seq);
                    }
                    if (i == exonSize - 1 && utr5Appender) {
                        for (int j = 0; j < 3; j++) {
                            exonSeq.write(seqCache.toBytes().byteAt(tmpEnd + j));
                        }
                    }
                }
            }
            cache.write(exonSeq.toBytes());
            cache.write(Constant.TAB);
            cache.write(Constant.PERIOD);
            cache.write(Constant.TAB);
            cache.write(Constant.PERIOD);
            cache.write(Constant.NEWLINE);
            kggWriter.write(cache.toBytes());
            cache.clear();
            exonSeq.clear();
        }
        cache.clear();
        while (collectOneContig(seqReader, seqCache)) {
            chromosome = currContig;
            List<Bytes> unmatchedRecords = unmatchedKGGRecords.get(chromosome);
            if (unmatchedRecords == null || unmatchedRecords.isEmpty()) {
                seqCache.clear();
                continue;
            }
            for (Bytes line : unmatchedRecords) {
                tabSplitter.init(line);
                int count1 = 0;
                while(tabSplitter.hasNext()){
                    Bytes item = tabSplitter.next();
                    switch (count1++) {
                        case 3:
                            inverseStrand = item.toString().equals("-");
                            break;
                        case 6:
                            codingStart = item.toInt();
                            break;
                        case 7:
                            codingEnd = item.toInt();
                            break;
                        case 9:
                            exonStart = parse(item);
                            break;
                        case 10:
                            exonEnd = parse(item);
                            break;
                        default:
                            break;
                    }
                }
                int exonSize = exonEnd.size();
                if (inverseStrand) {
                    boolean utr3Appender = exonStart.fastGet(0) > codingStart;
//                    for (int i = 0; i < exonSize; i++) {
                    for (int i = exonSize - 1; i >= 0; i--) {
                        int tmpPos = exonEnd.fastGet(i);
                        int tmpEnd = exonStart.fastGet(i);
                        for (int j = tmpPos - 1; j >= tmpEnd; j--) {
                            byte seq = seqCache.toBytes().byteAt(j);
                            switch (seq) {
                                case Constant.A:
                                case Constant.a:
                                    seq = Constant.T;
                                    break;
                                case Constant.C:
                                case Constant.c:
                                    seq = Constant.G;
                                    break;
                                case Constant.G:
                                case Constant.g:
                                    seq = Constant.C;
                                    break;
                                case Constant.T:
                                case Constant.t:
                                    seq = Constant.A;
                                    break;
                            }
                            exonSeq.write(seq);
                        }
                        if (i == 0 && utr3Appender && false) {
                            int firstStart = exonStart.fastGet(0);
                            for (int j = 1; j <= 3; j++) {
                                byte seq = seqCache.toBytes().byteAt(firstStart - j);
                                switch (seq) {
                                    case Constant.A:
                                    case Constant.a:
                                        seq = Constant.T;
                                        break;
                                    case Constant.C:
                                    case Constant.c:
                                        seq = Constant.G;
                                        break;
                                    case Constant.G:
                                    case Constant.g:
                                        seq = Constant.C;
                                        break;
                                    case Constant.T:
                                    case Constant.t:
                                        seq = Constant.A;
                                        break;
                                }
                                exonSeq.write(seq);
                            }
                        }
                    }
                } else {
                    boolean utr5Appender = exonEnd.fastGet(exonSize - 1) < codingEnd;
                    for (int i = 0; i < exonSize; i++) {
                        int tmpPos = exonStart.fastGet(i);
                        int tmpEnd = exonEnd.fastGet(i);
                        for (int j = tmpPos; j < tmpEnd; j++) {
                            byte seq = seqCache.toBytes().byteAt(j);
                            exonSeq.write(seq);
                        }
                        if (i == exonSize - 1 && utr5Appender) {
                            for (int j = 0; j < 3; j++) {
                                exonSeq.write(seqCache.toBytes().byteAt(tmpEnd + j));
                            }
                        }
                    }
                }
                cache.write(line);
                cache.write(exonSeq.toBytes());
                cache.write(Constant.TAB);
                cache.write(Constant.PERIOD);
                cache.write(Constant.TAB);
                cache.write(Constant.PERIOD);
                cache.write(Constant.NEWLINE);
                kggWriter.write(cache.toBytes());
                exonSeq.clear();
                cache.clear();
            }
        }
        seqReader.close();
        kggReader.close();
        kggWriter.close();
    }

    boolean collectOneContig(ReaderStream fs, ByteStream seqCache) throws IOException {
        if (finish) {
            return false;
        }
        if (nextChr != null) {
            currContig = nextChr;
            nextChr = null;
        }
        seqCache.clear();
        long size = 0;
        boolean res = false;
        while (fs.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            if (line.startsWith(Constant.NUMBER_SIGN)) {
                cache.clear();
                continue;
            }
            if (line.byteAt(0) == 62) {
                Bytes headLine = line.detach();
                Iterator<Bytes> iterator = headLine.split(Constant.BLANK).next().split(Constant.GREATER_THAN_SIGN);
                // ignore
                iterator.next();
                Bytes contig = iterator.next().detach();
                Chromosome chromosome = Chromosome.get(contig.toString());
                if (chromosome == Chromosome.UNKNOWN) {
                    chromosome = Chromosome.get(contig.toString());
                }
                if (currContig == null) {
                    currContig = chromosome;
                    cache.clear();
                    continue;
                } else {
                    // find next chromosome
                    nextChr = chromosome;
                    cache.clear();
                    break;
                }
            }
            size += line.length();
            seqCache.write(line.detach());
            cache.clear();
            res = true;
        }
        if (!res) {
            finish = true;
            currContig = nextChr;
            nextChr = null;
        }
        if (nextChr == null && finish) {
            return false;
        }
        this.contigSize.add(size);
        return res;
    }

    static IntList parse(Bytes encodeIntList) {
        IntList res = new IntList();
        Iterator<Bytes> iterator = encodeIntList.split(Constant.COMMA);
        while (iterator.hasNext()){
            Bytes item = iterator.next().trim();
            if (item.length() == 0) {
                continue;
            }
            res.add(item.toInt());
        }
        return res;
    }

    public KggFileSeqFiller setSeqPath(String seqPath) {
        this.seqPath = seqPath;
        return this;
    }

    public KggFileSeqFiller setKggFile(String kggFile) {
        this.kggFile = kggFile;
        return this;
    }

    public KggFileSeqFiller setResFile(String resFile) {
        this.resFile = resFile;
        return this;
    }
}
