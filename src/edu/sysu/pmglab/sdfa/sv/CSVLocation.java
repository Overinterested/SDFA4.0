package edu.sysu.pmglab.sdfa.sv;

import edu.sysu.pmglab.container.list.IntList;

/**
 * @author Wenjie Peng
 * @create 2024-08-26 00:55
 * @description store information for the retrieval of a complete complex SV
 */
public class CSVLocation {
    int indexInFile;
    IntList indexesOfContig;

    public CSVLocation(int indexInFile) {
        this.indexInFile = indexInFile;
    }

    public CSVLocation(int indexInFile, int... indexesOfContig) {
        this.indexInFile = indexInFile;
        this.indexesOfContig = new IntList(indexesOfContig);
    }
    public CSVLocation(int indexInFile, IntList chrIndexes){
        this.indexInFile = indexInFile;
        this.indexesOfContig = chrIndexes;
    }
    public int indexInFile() {
        return indexInFile;
    }

    public IntList getIndexesOfContig() {
        return indexesOfContig;
    }

    public int indexOfRawSV() {
        if (indexesOfContig == null) {
            return -1;
        }
        int index = 0;
        int size = indexesOfContig.size();
        for (int i = 0; i < size; i++) {
            int chrIndex = indexesOfContig.fastGet(i);
            if (chrIndex == -1) {
                break;
            }
            index++;
        }
        return index;
    }

    public int[] encodeIndexesOfCSV() {
        return indexesOfContig.toArray();
    }

    public CSVLocation setIndexesOfContig(IntList indexesOfContig) {
        this.indexesOfContig = indexesOfContig;
        return this;
    }

    public static IntList decodeIndexOfContig(int[] encode) {
        if (encode == null || encode.length == 0) {
            return null;
        }
        return new IntList(encode);
    }

    public int numOfItem() {
        return indexesOfContig == null ? 1 : indexesOfContig.size();
    }

    public CSVLocation setIndexInFile(int indexInFile) {
        this.indexInFile = indexInFile;
        return this;
    }
}
