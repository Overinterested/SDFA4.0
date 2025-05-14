package edu.sysu.pmglab.sdfa.annotation.source.record;

import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.easytools.calculator.TranscriptCalculator;

/**
 * @author Wenjie Peng
 * @create 2024-09-07 07:52
 * @description
 */
public class SourceRNARecord implements SourceRecord {
    byte strand;
    int[] exons;
    TranscriptCalculator calc;
    String nameOfRNA, nameOfGene;
    IntInterval codingRange, range;
    int indexOfRNA, indexOfFile, indexOfContig;

    public static int UPSTREAM_DISTANCE = 2000;
    public static int DOWNSTREAM_DISTANCE = 2000;

    @Override
    public int getIndexOfFile() {
        return indexOfFile;
    }

    @Override
    public int getIndexOfContig() {
        return indexOfContig;
    }

    public static SourceRNARecord loadCoordinateFromPartialReader(IRecord record) {
        SourceRNARecord rnaRecord = new SourceRNARecord();
        rnaRecord.indexOfContig = record.get(0);
        rnaRecord.range = record.get(1);
        return rnaRecord;
    }

    @Override
    public IntInterval getInterval() {
        return range;
    }

    @Override
    public SourceRNARecord setIndexOfFile(int indexOfFile) {
        this.indexOfFile = indexOfFile;
        return this;
    }

    public static SourceRNARecord load(IRecord record) {
        SourceRNARecord rnaRecord = new SourceRNARecord();
        rnaRecord.nameOfGene = record.get(1);
        rnaRecord.indexOfRNA = record.get(3);
        rnaRecord.nameOfRNA = record.get(4);
        rnaRecord.strand = (byte) (int) record.get(5);
        rnaRecord.range = record.get(6);
        rnaRecord.codingRange = record.get(7);
        rnaRecord.exons = ((IntList) record.get(8)).toArray();
        return rnaRecord;
    }

    public byte getStrand() {
        return strand;
    }

    public int[] getExons() {
        return exons;
    }

    public String getNameOfRNA() {
        return nameOfRNA;
    }

    public String getNameOfGene() {
        return nameOfGene;
    }

    public IntInterval getCodingRange() {
        return codingRange;
    }

    public IntInterval getRange() {
        return range;
    }

    public int getIndexOfRNA() {
        return indexOfRNA;
    }

    public static int getUpstreamDistance() {
        return UPSTREAM_DISTANCE;
    }

    public static int getDownstreamDistance() {
        return DOWNSTREAM_DISTANCE;
    }

    public TranscriptCalculator getCalc() {
        if (calc == null) {
            calc = new TranscriptCalculator(this);
        }
        return calc;
    }

    public String geneRNAName() {
        return nameOfGene + ":" + nameOfRNA;
    }

    public void clear() {
        calc = null;
        exons = null;
        range = null;
        codingRange = null;
    }

    public static void setUpstreamDistance(int upstreamDistance) {
        UPSTREAM_DISTANCE = upstreamDistance;
    }

    public static void setDownstreamDistance(int downstreamDistance) {
        DOWNSTREAM_DISTANCE = downstreamDistance;
    }

    public boolean isCodingRNA() {
        return codingRange.start() != codingRange.end();
    }

    public IntInterval getWholeRange() {
        return new IntInterval(range.start() - UPSTREAM_DISTANCE, range.end() + DOWNSTREAM_DISTANCE);
    }
}
