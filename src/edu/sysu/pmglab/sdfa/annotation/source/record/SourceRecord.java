package edu.sysu.pmglab.sdfa.annotation.source.record;

import edu.sysu.pmglab.container.interval.IntInterval;

/**
 * @author Wenjie Peng
 * @create 2024-09-07 07:13
 * @description
 */
public interface SourceRecord {
    int getIndexOfFile();

    int getIndexOfContig();

    SourceRecord setIndexOfFile(int indexOfFile);

    IntInterval getInterval();

    default int getLen() {
        return -1;
    }

    /**
     * get the sv type if the record is in Reference SV Database
     *
     * @return sv type
     */
    default int typeOfRecordInSV() {
        return -1;
    }
}
