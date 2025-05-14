package edu.sysu.pmglab.easytools.container.seq;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.gtb.genome.coordinate.Chromosome;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2023-04-12 20:16
 * @description
 */
public class ProcessForGCContent {
    File file;
    public static final byte[] LEFTBOOK = ">".getBytes();
    HashMap<Bytes, String> contigMap = new HashMap<>();
    public static HashMap<Chromosome, List<BlockForGC>> gcBlocks = new HashMap<>();
    public static HashMap<Chromosome, List<BlockForSeq>> seqBlocks = new HashMap<>();

    static {
        for (Chromosome chrIndex : Chromosome.values()) {
            gcBlocks.put(chrIndex, new List<>());
            seqBlocks.put(chrIndex, new List<>());
        }
    }

    public ProcessForGCContent() {
    }


    public ProcessForGCContent setFile(Object file) {
        this.file = new File(file.toString());
        return this;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ProcessForGCContent processForGCContent = new ProcessForGCContent();
        processForGCContent.setFile("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/GRCh38_latest_genomic.fna.gz");
        processForGCContent.parseForGCContent();
        processForGCContent.encode();
        CCFReader reader = new CCFReader("/Users/wenjiepeng/Desktop/SV/SVAnnot/AnnotationFile/Annotations_Human/BreakpointsAnnotations/GCcontent/GRCh37/compressedSeq.ccf");
        IRecord record;
//        ProcessForGCContent processForGCContent = new ProcessForGCContent();
        while ((record = reader.read()) != null) {
            gcBlocks.get((Chromosome) record.get(0)).add(BlockForGC.decode(record));
        }
        HashMap<Chromosome, List<BlockForGC>> gcBlocks = ProcessForGCContent.gcBlocks;
        Thread.sleep(100000);
    }

    public void parseForGCContent() throws IOException {
        ReaderStream fs = LiveFile.of(file).openAsText();
        ByteStream cache = new ByteStream();
        Chromosome preChr = Chromosome.get("chr1");
        Chromosome currChr = null;
        String tmpLine;
        BlockForGC block = new BlockForGC();
        int signal = 0;
        int count = 0;
        char b;
        long l = System.currentTimeMillis();
        while (fs.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            if (line.startsWith(Constant.NUMBER_SIGN)) {
                cache.clear();
                continue;
            }
            if (line.startsWith(LEFTBOOK)) {
                List<Bytes> split = new List<>();
                Iterator<Bytes> iterator = cache.toBytes().split(Constant.BLANK);
                while (iterator.hasNext()) split.add(iterator.next().detach());

                Bytes rawContigName = split.fastGet(0).subBytes(1);
                currChr = Chromosome.get(rawContigName.toString());
                contigMap.put(rawContigName, currChr.getName());
                if (currChr.equals(Chromosome.UNKNOWN)) {
                    cache.clear();
                    break;
                }
                cache.clear();
                continue;
            }
            if (!preChr.equals(currChr)) {
                block.end();
                gcBlocks.get(preChr).add(block);
                block = new BlockForGC();
                preChr = currChr;
            }
            tmpLine = line.toString();
            for (int i = 0; i < tmpLine.length(); i++) {
                b = tmpLine.charAt(i);
                signal = (b == 'c' || b == 'g' || b == 'C' || b == 'G') ? 1 : 0;
                count++;
                if (!block.add(signal)) {
                    gcBlocks.get(preChr).add(block);
                    block = new BlockForGC();
                    block.add(signal);
                }
            }
            cache.clear();
        }
        System.out.println(System.currentTimeMillis() - l);
        fs.close();
    }

    public void parseForSeq() throws IOException {
        ReaderStream fs = LiveFile.of(file).openAsText();
        ByteStream cache = new ByteStream();
        Chromosome preChr = Chromosome.get("chr1");
        Chromosome currChr = null;
        String tmpLine;
        BlockForSeq block = new BlockForSeq();
        int signal = 0;
        int count = 0;
        char b;
        long l = System.currentTimeMillis();
        loop:
        while (fs.readline(cache) != -1) {
            if (cache.toBytes().startsWith(LEFTBOOK)) {
                Bytes byteCode = cache.toBytes().subBytes(1);
                currChr = Chromosome.get(byteCode.toString());
                if (currChr == null) {
                    cache.clear();
                    break;
                }
                cache.clear();
                continue;
            }
            if (preChr.equals(currChr)) {
                block.end();
                seqBlocks.get(preChr).add(block);
                block = new BlockForSeq();
                count = 0;
                preChr = currChr;
            }
            tmpLine = cache.toBytes().toString();
            for (int i = 0; i < tmpLine.length(); i++) {
                b = tmpLine.charAt(i);
                if (b == 'N') {
                    continue;
                }
                switch (b) {
                    case 'a':
                        signal = 0;
                        break;
                    case 'A':
                        signal = 0;
                        break;
                    case 't':
                        signal = 1;
                        break;
                    case 'T':
                        signal = 1;
                        break;
                    case 'c':
                        signal = 2;
                        break;
                    case 'C':
                        signal = 2;
                        break;
                    case 'g':
                        signal = 3;
                        break;
                    case 'G':
                        signal = 4;
                        break;
                    default:
                        continue loop;
                }
                count++;
                if (!block.add(signal)) {
                    seqBlocks.get(preChr).add(block);
                    block = new BlockForSeq();
                    block.start = count;
                    block.add(signal);
                }
            }
            cache.clear();
        }
        System.out.println(System.currentTimeMillis() - l);
        fs.close();
    }

    public void encode() throws IOException {
        File file = new File("/Users/wenjiepeng/Desktop/SV/SVAnnot/AnnotationFile/Annotations_Human/BreakpointsAnnotations/GCcontent/GRCh37/compressedSeq.ccf");
        CCFWriter writer = CCFWriter.setOutput(file)
                .addField("Chr", FieldType.chromosome)
                .addField("Full", FieldType.bool)
                .addField("EndSeqNum", FieldType.varInt32)
                .addField("CompressList", FieldType.int32List).instance();
        IRecord record = writer.getRecord();
        List<BlockForGC> tmp;
        for (Chromosome chromosome : Chromosome.values()) {
            if ((tmp = gcBlocks.get(chromosome)) == null || tmp.size() == 0) {
                continue;
            }
            for (int i = 0; i < tmp.size(); i++) {
                writer.write(tmp.get(i).encode(record, chromosome));
            }
        }
        writer.close();

    }

    public float calculateInterval(byte chr, int start, int end) {
        int count = 0;
        int startBlockIndex = start >>> 17;
        int endBlockIndex = end >>> 17;
        if (startBlockIndex == endBlockIndex) {
            BlockForGC block = gcBlocks.get(chr).get(startBlockIndex);
            int startIndexIntInBlock = (start - startBlockIndex << 17) >>> 5;
            int endIndexIntInBlock = (end - startBlockIndex << 17) >>> 5;
            int startIndexInInt = (start - startBlockIndex << 17) % 32;
            int endIndexInInt = (end - startBlockIndex << 17) % 32;
            count += bitCount(block.compress.get(startIndexIntInBlock) << (startIndexInInt - 1) >>> (startIndexInInt - 1));
            count += bitCount(block.compress.get(endIndexIntInBlock) >>> (32 - 1 - endIndexInInt) << (32 - 1 - endIndexInInt));
            for (int i = startIndexInInt + 1; i < endIndexIntInBlock; i++) {
                count += bitCount(block.compress.get(i));
            }
            return count;
        } else {
            return 0;
        }
    }

    public float calculateBreakPoint(Chromosome chr, int pos, int relativeDis) {
        List<BlockForGC> blockForGC = gcBlocks.get(chr);
        // use bit to finish
        int blockIndexI = pos >>> 17;
        int blcokStartI = blockIndexI << 17;
        int indexInBlockI = (pos - blcokStartI) >>> 5;
        int indexInInt = (pos - blcokStartI) % 32;
        int leftSize = 32 - indexInInt;
        int leftLoad = relativeDis - (leftSize - 1);
        int needInt = relativeDis / 32 + 1;
        int leftGCCount = countLeftGC(blockForGC, blockIndexI, indexInBlockI, indexInInt, relativeDis);
        int rightGCCount = countRightGC(blockForGC, blockIndexI, indexInBlockI, indexInInt, relativeDis);
        return (leftGCCount + rightGCCount) / ((float) 2 * relativeDis);
    }

    public void test2() throws IOException {
        CCFReader reader = new CCFReader("/Users/wenjiepeng/Desktop/SV/SVAnnot/AnnotationFile/Annotations_Human/BreakpointsAnnotations/GCcontent/GRCh37/compressedSeq.ccf");
        IRecord record = reader.getRecord();
        ProcessForGCContent processForGCContent = new ProcessForGCContent();
        while (reader.read(record)) {
            gcBlocks.get(record.get(0)).add(BlockForGC.decode(record));
        }
//        processForGCContent.parseForGCContent();
        float v = processForGCContent.calculateBreakPoint(Chromosome.get("1"), 2 * 4096 * 32 - 11, 100);
    }

    private static int bitCount(int i) {
        i = i - ((i >>> 1) & 0x55555555);
        i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
        i = (i + (i >>> 4)) & 0x0f0f0f0f;
        i = i + (i >>> 8);
        i = i + (i >>> 16);
        return i & 0x3f;
    }

    private int countLeftGC(List<BlockForGC> arrays, int blockIndex, int indexInBlock, int indexInInt, int relative) {
        int count = 0;
        BlockForGC block = arrays.get(blockIndex);
        int leftSize = indexInInt - 1;
        // calculate current value
        count += bitCount(block.compress.get(indexInBlock) >>> (32 - leftSize) << (32 - leftSize));
        int leftIntNum = (relative - leftSize) / 32;

        if (relative - leftSize - leftIntNum * 32 != 0) {
            leftIntNum++;
        }

        if (indexInBlock >= leftIntNum) {
            // left block meets need
            int extraInt = 0;
            for (int compressValue : block.compress.subList(indexInBlock - leftIntNum, leftIntNum).toArray()) {
                count += bitCount(compressValue);
                extraInt++;
            }
            int needCountRight = relative - leftSize - extraInt * 32;
            if (needCountRight != 0) {
                int finalInt = block.compress.get(indexInBlock - leftIntNum - 1);
                count += bitCount(finalInt << (32 - needCountRight) >>> (32 - needCountRight));
            }
        } else {
            // need load last block

            int leftBlockIntNum = leftIntNum - indexInBlock;
            int needCountRight = relative - leftSize - indexInBlock * 32;
            for (int compressValue : block.compress.subList(0, indexInBlock).toArray()) {
                count += bitCount(compressValue);
            }
            if (blockIndex == 0) {
                // this is first block
                return count;
            }

            block = arrays.get(blockIndex - 1);
            for (int i = 0; i < leftBlockIntNum - 1; i++) {
                count += bitCount(block.compress.get(4095 - i));
                needCountRight -= 32;
            }
            int finalInt = block.compress.get(4096 - leftBlockIntNum);
            count += bitCount(finalInt << (32 - needCountRight) >> (32 - needCountRight));
        }
        return count;
    }

    private int countRightGC(List<BlockForGC> arrays, int blockIndex, int indexInBlock, int indexInInt, int relative) {
        int count = 0;
        BlockForGC block = arrays.get(blockIndex);
        int rightSize = 32 - indexInInt - 1;
        count += bitCount(block.compress.get(indexInBlock) << rightSize >>> rightSize);
        int rightIntNum = (relative - rightSize) / 32;
        if (rightIntNum - rightSize - rightIntNum * 32 != 0) {
            rightIntNum++;
        }
        if (block.compress.size() - indexInBlock - 1 >= rightIntNum) {
            int extra = 0;
            // right block meets need
            for (int i = 1; i < rightIntNum; i++) {
                count += bitCount(block.compress.get(indexInBlock + i));
                extra++;
            }
            int needCountLeft = relative - rightSize - extra * 32;
            if (needCountLeft != 0) {
                int finalInt = block.compress.get(indexInBlock + rightIntNum);
                count += bitCount(finalInt >>> (32 - needCountLeft) << (32 - needCountLeft));
            }
            return count;
        } else {
            // need load sequential block
            int rightBlockIntNum = rightIntNum - (block.compress.size() - 1 - indexInBlock);
            if (blockIndex != arrays.size() - 1) {
                // whether it's last block
                int needCountLeft = relative - rightSize;
                for (int compressValue : block.compress.subList(indexInBlock + 1, 4095 - indexInBlock).toArray()) {
                    count += bitCount(compressValue);
                    needCountLeft -= 32;
                }
                block = arrays.get(blockIndex + 1);
                for (int i = 0; i < rightBlockIntNum - 1; i++) {
                    count += bitCount(block.compress.get(i));
                    needCountLeft -= 32;
                }
                int finalInt = block.compress.get(rightBlockIntNum - 1);
                count += bitCount(finalInt >>> needCountLeft << needCountLeft);
                return count;
            } else {
                // is the last block
                int lastIntIndex = block.compress.size() - 1;
                int needCountLeft = relative - rightSize;
                for (int i = indexInBlock; i < lastIntIndex; i++) {
                    count += bitCount(block.compress.get(i));
                    needCountLeft -= 32;
                }
                if (block.num >= needCountLeft) {
                    count += bitCount(block.compress.get(lastIntIndex));
                } else {
                    count += bitCount(block.compress.get(lastIntIndex) >>> (block.num - needCountLeft) << (block.num - needCountLeft));
                }
                return count;
            }
        }
    }
}

class BlockForSeq {
    int start = 0;
    int point = 0;
    int currValue = 0;
    int num = 0;
    IntList compress = new IntList(4096);

    public boolean isFull() {
        return point == 4096;
    }

    public void end() {
        compress.add(currValue);
    }

    public boolean add(int i) {
        if (isFull()) {
            return false;
        }

        if (num == 16) {
            compress.add(currValue);
            point++;
            if (isFull()) {
                return false;
            }
            currValue = 0;
            num = 0;
        }
        currValue += i;
        currValue = currValue << 2;
        num++;
        return true;
    }
}

class BlockForGC {
    int num = 0;
    int point = 0;
    int currValue = 0;
    IntList compress = new IntList(4096);

    public boolean isFull() {
        return point == 4096;
    }

    public boolean add(int i) {
        if (isFull()) {
            return false;
        }

        if (num == 32) {
            compress.add(currValue);
            point++;
            if (isFull()) {
                return false;
            }
            currValue = 0;
            num = 0;
        }
        currValue += i;
        currValue = currValue << 1;
        num++;
        return true;
    }

    public void end() {
        compress.add(currValue);
    }

    public IRecord encode(IRecord record, Chromosome chr) {
        record.set(0, chr);
        record.set(1, isFull());
        record.set(2, num);
        record.set(3, compress);
        return record;
    }

    public static BlockForGC decode(IRecord record) {
        BlockForGC block = new BlockForGC();
        block.compress = IntList.wrap(record.get(3));
        block.num = record.get(2);
        block.point = block.compress.size();
        return block;
    }

}