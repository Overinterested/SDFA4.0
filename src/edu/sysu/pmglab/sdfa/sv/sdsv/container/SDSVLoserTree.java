package edu.sysu.pmglab.sdfa.sv.sdsv.container;

import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.annotation.output.SourceOutput;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Wenjie Peng
 * @create 2024-09-13 10:12
 * @description
 */
public class SDSVLoserTree {
    protected int lastIndex;
    protected int rootIndex;
    protected final int numFiles;
    protected final ISDSV[] records;
    protected final List<File> files;
    protected final SDFReader[] readers;
    protected final int[] loserTreeIndexes;
    protected final SDFReadType readerMode;

    public SDSVLoserTree(int numFiles, SDFReadType readerMode) {
        this.numFiles = numFiles;
        this.readerMode = readerMode;
        this.files = new List<>(numFiles);
        this.records = new ISDSV[numFiles];
        this.readers = new SDFReader[numFiles];
        this.loserTreeIndexes = new int[numFiles];
    }

    public SDSVLoserTree(List<SDFReader> readers, SDFReadType readerMode, String contigName) throws IOException {
        this.files = null;
        this.readerMode = readerMode == null ? SDFReadType.MERGE : readerMode;
        this.numFiles = readers.size();
        this.records = new ISDSV[this.numFiles];
        this.readers = new SDFReader[this.numFiles];
        this.loserTreeIndexes = new int[this.numFiles];
        for (int i = 0; i < this.numFiles; i++) {
            SDFReader sdfReader = readers.get(i);
            sdfReader = new SDFReader(sdfReader.getFile(), this.readerMode);
            SDFReader limit = sdfReader.limit(contigName);
            if (limit == null) {
                sdfReader.limit(0, 0);
                records[i] = null;
                continue;
            }
            records[i] = sdfReader.read();
            sdfReader.close();
            this.readers[i] = sdfReader;
        }
        initializeLoserTree();
    }

    public SDSVLoserTree(List<File> files, SDFReadType readerMode) throws IOException {
        this.files = files;
        this.numFiles = files.size();
        this.readerMode = readerMode;
        this.records = new ISDSV[this.numFiles];
        this.readers = new SDFReader[this.numFiles];
        this.loserTreeIndexes = new int[this.numFiles];

        initializeReaders();
        initializeLoserTree();
    }

    private void initializeReaders() throws IOException {
        for (int i = 0; i < numFiles; i++) {
            SDFReader reader = new SDFReader(files.fastGet(i), readerMode);
            readers[i] = reader;
            records[i] = reader.read();
            reader.close();
        }
    }

    protected void initializeLoserTree() {
        int winner = 0;
        for (int i = 1; i < numFiles; i++) {
            if (beat(i, winner)) {
                winner = i;
            }
        }
        // 非叶子节点初始化为冠军节点
        Arrays.fill(loserTreeIndexes, winner);
        // 插入每个文件的初始记录，调整败者树
        for (int i = numFiles - 1; i >= 0; i--) {
            adjust(i);  // 调整树结构，使其保持败者树的性质
        }
    }

    // 调整败者树，更新胜者和败者
    protected void adjust(int fileIndex) {
        // 从当前节点的父节点开始调整
        int parent = (fileIndex + numFiles) / 2;
        while (parent > 0) {
            // 不断向上调整直到根节点
            if (beat(loserTreeIndexes[parent], fileIndex)) {
                int tmp = loserTreeIndexes[parent];
                loserTreeIndexes[parent] = fileIndex;
                fileIndex = tmp;
            }
            // 移动到上一个父节点
            parent = parent / 2;
        }
        loserTreeIndexes[0] = fileIndex;
        rootIndex = loserTreeIndexes[0];
    }

    public boolean getMinRecords(int numOfRecords, List<ISDSV> collectedSDSVList,
                                 IntList fileIDList, List<SourceOutput> sourceOutputList) throws IOException {
        int fileID;
        int annotationSize;
        int startPointer, endPointer;
        for (int i = 0; i < numOfRecords; i++) {
            if (records[rootIndex] == null) {
                // finish reading
                return false;
            }
            // get min record
            fileID = rootIndex;
            ISDSV minRecord = records[rootIndex];
            // update loser tree
            SDFReader reader = readers[rootIndex];
            if (reader.isClosed()) {
                reader.openLastWithLimit();
            }
            records[rootIndex] = reader.read();
            adjust(rootIndex);
            reader.close();
            // judge annotation
            IntList annotationIndexes = minRecord.getAnnotationIndexes();
            if ((annotationSize = annotationIndexes.size()) == 0) {
                continue;
            }
            for (int j = 0; j < annotationSize / 2; j++) {
                startPointer = annotationIndexes.fastGet(2 * j);
                if (startPointer == -1) {
                    continue;
                }
                endPointer = annotationIndexes.fastGet(2 * j + 1);
                sourceOutputList.fastGet(j).expand(startPointer, endPointer);

            }
            collectedSDSVList.add(minRecord);
            fileIDList.add(fileID);
        }
        return true;
    }


    // 获取下一个最小的记录
    public ISDSV getNextMinRecord() throws IOException {
        if (records[rootIndex] == null) {
            // 所有文件都处理完毕
            return null;
        }
        // 当前的最小值
        ISDSV minRecord = records[rootIndex].setFileIndex(rootIndex);
        lastIndex = rootIndex;
        // 从胜者文件中读取下一条记录
        SDFReader reader = readers[rootIndex];
        if (reader.isClosed()) {
            reader.openLastWithLimit();
        }
        records[rootIndex] = reader.read();
        // 调整败者树
        adjust(rootIndex);
        return minRecord;
    }

    public int getNextMinRecordIndex() {
        return rootIndex;
    }

    protected boolean beat(int index1, int index2) {
        ISDSV var1 = records[index1];
        ISDSV var2 = records[index2];
        if (var1 == null)
            return false;
        if (var2 == null)
            return true;
        // 这里, 当叶节点数据相等时比较分支索引是为了实现排序算法的稳定性
        int status = var1.getCoordinate().compareTo(var2.getCoordinate());
        return status != 0 ? status < 0 : index1 < index2;
    }

    // 关闭所有文件流
    public void close() throws IOException {
        for (SDFReader reader : readers) {
            if (!reader.isClosed())
                reader.close();
        }
    }

    public int getLastIndex() {
        return lastIndex;
    }

    public static void main(String[] args) throws IOException {
        List<File> files = new List<>(3);
        files.add(new File("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf.sdf"));
        files.add(new File("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/HG01891_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf.sdf"));
        files.add(new File("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/HG01928_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf.sdf"));
        SDSVLoserTree tree = new SDSVLoserTree(files, SDFReadType.COORDINATE);
        for (int i = 0; i < 10; i++) {
            ISDSV nextMinRecord = tree.getNextMinRecord();
            System.out.println(nextMinRecord.getCoordinate().toString());
        }
    }
}

