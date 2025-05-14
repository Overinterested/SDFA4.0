package edu.sysu.pmglab.easytools.calculator;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.intervaltree.inttree.IntIntervalTree;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRNARecord;
import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

/**
 * @author Wenjie Peng
 * @create 2024-09-17 03:45
 * @description
 */
public class TranscriptCalculator {
    /**
     * contain 9 regions, 7 of 9 needs calculate
     * 1. leftIntergenic (exclude)
     * 2. upstream
     * 3. utr5; 4. coding exon; 5. exon; 6. intro; 7. utr3;
     * 8. downstream
     * 9. rightIntergenic (exclude)
     */
    byte strand;
    IntInterval range;
    int[] lenOfRegions;
    IntIntervalTree<TranscriptRegion> intervalTree;
    LinkedSet<TranscriptRegion> indexableTranscriptRegion;
    IntIntervalTree.Builder<TranscriptRegion> intervalTreeBuilder;

    private static final int UPSTREAM_INDEX = 0;
    private static final int UTR5_INDEX = 1;
    private static final int CODING_EXON_INDEX = 2;
    private static final int EXON_INDEX = 3;
    private static final int INTRO_INDEX = 4;
    private static final int UTR3_INDEX = 5;
    private static final int DOWNSTREAM_INDEX = 6;

    private static final float[] EMPTY_COVERAGE = new float[0];
    private static final SVTypeSign INS_TYPE = SVTypeSign.getByName("INS");
    private static final SVTypeSign INV_TYPE = SVTypeSign.getByName("INV");

    public TranscriptCalculator(SourceRNARecord rnaRecord) {
        intervalTreeBuilder = new IntIntervalTree.Builder<>();
        int[] exons = rnaRecord.getExons();
        int numOfExon = exons.length / 2;
        indexableTranscriptRegion = new LinkedSet<>();
        IntInterval range = rnaRecord.getRange();
        this.range = range;
        int rnaStart = range.start();
        int rnaEnd = range.end();
        IntInterval codingRange = rnaRecord.getCodingRange();
        this.strand = rnaRecord.getStrand();
        switch (strand) {
            case 0:
                // + strand
                // sum regions : exon + intro[len: exon -1] + stream[2] + utr[2] + intergenic[2]
                indexableTranscriptRegion.add(
                        new LeftIntergenic(
                                Integer.MIN_VALUE, rnaStart - SourceRNARecord.UPSTREAM_DISTANCE
                        ));
                indexableTranscriptRegion.add(
                        new Upstream(
                                rnaStart - SourceRNARecord.UPSTREAM_DISTANCE, rnaStart
                        ));
                // add exon, intro, utr
                if (codingRange.end() != codingRange.start()) {
                    // coding rna
                    int codingStart = codingRange.start();
                    int codingEnd = codingRange.end();
                    for (int i = 0; i < numOfExon; i++) {
                        int exonStart = exons[2 * i];
                        int exonEnd = exons[2 * i + 1];
                        // intro
                        if (i != numOfExon - 1) {
                            indexableTranscriptRegion.add(new Intro(exons[2 * i + 1], exons[2 * i + 2], i));
                        }
                        // utr5
                        if (codingStart >= exonEnd) {
                            // coding start is larger than curr exon end, so not coding exon is full utr5
                            indexableTranscriptRegion.add(new UTR5(exonStart, exonEnd, i));
                            continue;
                        } else if (codingStart >= exonStart) {
                            // coding start is larger than curr exon start but less than curr exon end, so part is full utr5
                            if (codingStart != exonStart) {
                                indexableTranscriptRegion.add(new UTR5(exonStart, codingStart, i));
                            }
                            indexableTranscriptRegion.add(new CodingExon(codingStart, exonEnd, i));
                            continue;
                        }
                        // utr3
                        if (codingEnd <= exonStart) {
                            // coding end is less than curr exon start, so not coding exon is full utr3
                            indexableTranscriptRegion.add(new UTR3(exonStart, exonEnd, i));
                        } else if (codingEnd <= exonEnd) {
                            // coding end is larger than curr exon start but less than curr exon end, so is part utr3
                            if (codingEnd != exonEnd) {
                                indexableTranscriptRegion.add(new UTR3(codingEnd, exonEnd, i));
                            }
                            indexableTranscriptRegion.add(new CodingExon(exonStart, codingEnd, i));
                        }
                        indexableTranscriptRegion.add(new CodingExon(exonStart, exonEnd, i));
                    }
                } else {
                    // non-coding rna
                    for (int i = 0; i < numOfExon; i++) {
                        try {
                            indexableTranscriptRegion.add(new Exon(exons[2 * i], exons[2 * i + 1], i));
                        } catch (Exception e) {
                            int a = 1;
                        }
                        if (i != numOfExon - 1) {
                            try {
                                indexableTranscriptRegion.add(new Intro(exons[2 * i + 1], exons[2 * i + 2], i));
                            } catch (Exception e) {
                                int a = 1;
                            }
                        }
                    }
                }
                indexableTranscriptRegion.add(new Downstream(rnaEnd, rnaEnd + SourceRNARecord.DOWNSTREAM_DISTANCE));
                indexableTranscriptRegion.add(new RightIntergenic(rnaEnd + SourceRNARecord.DOWNSTREAM_DISTANCE, Integer.MAX_VALUE));
                break;
            case 1:
                // - strand
                indexableTranscriptRegion.add(
                        new LeftIntergenic(
                                rnaEnd + SourceRNARecord.UPSTREAM_DISTANCE, Integer.MAX_VALUE
                        ));
                indexableTranscriptRegion.add(
                        new Upstream(
                                rnaEnd, rnaEnd + SourceRNARecord.UPSTREAM_DISTANCE
                        ));
                // add exon, intro, utr
                if (codingRange.end() != codingRange.start()) {
                    // coding rna
                    int codingStart = codingRange.start();
                    int codingEnd = codingRange.end();
                    for (int i = numOfExon - 1; i >= 0; --i) {
                        int exonStart = exons[2 * i];
                        int exonEnd = exons[2 * i + 1];
                        int index = numOfExon - 1 - i;
                        if (i != numOfExon - 1) {
                            indexableTranscriptRegion.add(new Intro(exons[2 * i + 1], exons[2 * i + 2], index));
                        }
                        // utr3
                        if (codingStart >= exonEnd) {
                            // full utr3
                            indexableTranscriptRegion.add(new UTR3(exonStart, exonEnd, index));
                            continue;
                        } else if (codingStart >= exonStart) {
                            // part utr3
                            if (codingStart != exonStart) {
                                indexableTranscriptRegion.add(new UTR3(exonStart, codingStart, index));
                            }
                            indexableTranscriptRegion.add(new CodingExon(codingStart, exonEnd, index));
                            continue;
                        }
                        // utr5
                        if (codingEnd <= exonStart) {
                            // full utr5
                            indexableTranscriptRegion.add(new UTR5(exonStart, exonEnd, index));
                        } else if (codingEnd <= exonEnd) {
                            // part utr5
                            if (codingEnd != exonEnd) {
                                indexableTranscriptRegion.add(new UTR5(codingEnd, exonEnd, index));
                            }
                            indexableTranscriptRegion.add(new CodingExon(exonStart, codingEnd, index));
                        }
                        indexableTranscriptRegion.add(new CodingExon(exonStart, exonEnd, index));
                    }
                } else {
                    for (int i = numOfExon - 1; i >= 0; --i) {
                        int index = numOfExon - 1 - i;
                        indexableTranscriptRegion.add(new Exon(exons[2 * i], exons[2 * i + 1], index));
                        if (i != numOfExon - 1) {
                            indexableTranscriptRegion.add(new Intro(exons[2 * i + 1], exons[2 * i + 2], index));
                        }
                    }
                }
                indexableTranscriptRegion.add(new Downstream(rnaEnd, rnaEnd + SourceRNARecord.DOWNSTREAM_DISTANCE));
                indexableTranscriptRegion.add(new RightIntergenic(rnaEnd + SourceRNARecord.DOWNSTREAM_DISTANCE, Integer.MAX_VALUE));
                break;
            default:
                throw new UnsupportedOperationException("Unknown transcript strand");
        }
        for (TranscriptRegion transcriptRegion : indexableTranscriptRegion) {
            intervalTreeBuilder.add(transcriptRegion.getRange(), transcriptRegion);
        }
        intervalTree = intervalTreeBuilder.build();
    }

    public List<TranscriptRegion> overlap(ISDSV sv) {
        List<TranscriptRegion> overlaps = intervalTree.getOverlaps(sv.getCoordinateInterval());
        overlaps.sort(TranscriptRegion::compareTo);
        return overlaps;
    }

    public String locateStartRegion(List<TranscriptRegion> overlaps, int startPos) {
        return overlaps.fastGet(0).locate(startPos, strand);
    }

    public String locateEndRegion(List<TranscriptRegion> overlaps, int endPos) {
        return overlaps.fastLastGet(0).locate(endPos, strand);
    }

    public String locateRange(List<TranscriptRegion> overlaps, SVCoordinate coordinate) {
        return overlaps.fastGet(0).locate(coordinate.getPos(), strand) +
                "~" +
                overlaps.fastLastGet(0).locate(coordinate.getEnd(), strand);
    }

    public float[] locateProportion(ISDSV sv) {
        List<TranscriptRegion> overlaps = intervalTree.getOverlaps(sv.getCoordinateInterval());
        return locateProportion(overlaps, sv);
    }

    public float[] locateProportion(List<TranscriptRegion> overlaps, ISDSV sv, float[] cache) {
        int[] overlapLen = new int[7];
        SVTypeSign type = sv.getType();
        IntInterval coordinateInterval = sv.getCoordinateInterval();
        // TODO: add a nagf tag
        if (type == INV_TYPE) {
            if (coordinateInterval.contains(range.start(), range.end())) {
                return EMPTY_COVERAGE;
            }
        }
        boolean isInsertion = type == INS_TYPE;
        int length = Math.abs(sv.length());
        for (TranscriptRegion overlapRegion : overlaps) {
            int indexOfStoredList = overlapRegion.getIndexOfStoredList();
            // left and right inter-genetic
            if (indexOfStoredList == -1) {
                continue;
            }
            if (!isInsertion) {
                IntInterval overlapsRange = coordinateInterval.getOverlaps(overlapRegion.range);
                length = overlapsRange.end() - overlapsRange.start();
            }
            overlapLen[indexOfStoredList] += length;
        }
        // get all exon len by adding all coding, utr5, ut3
        overlapLen[EXON_INDEX] += (overlapLen[UTR5_INDEX] + overlapLen[UTR3_INDEX] + overlapLen[CODING_EXON_INDEX]);
        // init
        if (this.lenOfRegions == null) {
            lenOfRegions = new int[7];
            for (TranscriptRegion transcriptRegion : indexableTranscriptRegion) {
                int indexOfStoredList = transcriptRegion.getIndexOfStoredList();
                if (indexOfStoredList == -1) {
                    continue;
                }
                lenOfRegions[indexOfStoredList] += transcriptRegion.getLen();
            }
            lenOfRegions[EXON_INDEX] += (lenOfRegions[UTR5_INDEX] + lenOfRegions[UTR3_INDEX] + lenOfRegions[CODING_EXON_INDEX]);
        }
        for (int i = 0; i < 7; i++) {
            cache[i] = lenOfRegions[i] != 0 ? overlapLen[i] / (float) lenOfRegions[i] : 0;
            if (cache[i]>=1f){
                cache[i] = 1f;
            }
        }
        return cache;
    }
    public float[] locateProportion(List<TranscriptRegion> overlaps, ISDSV sv) {
        return locateProportion(overlaps, sv, new float[7]);
    }

    /**
     * contain 9 regions, 7 of 9 needs calculate
     * 1. leftIntergenic (exclude)
     * 2. upstream
     * 3. utr5; 4. coding exon; 5. exon; 6. intro; 7. utr3; [these region can be a same level]
     * 8. downstream
     * 9. rightIntergenic (exclude)
     */
    public static abstract class TranscriptRegion implements Comparable<TranscriptRegion> {
        final IntInterval range;

        public TranscriptRegion(int pos, int end) {
            this.range = new IntInterval(pos, end);
        }

        public IntInterval getRange() {
            return range;
        }

        abstract String getRegion();

        /**
         * get order of region in RNA
         *
         * @return int value
         */
        abstract public int getOrderOfRegion();

        /**
         * get index where the value of region is stored in list
         *
         * @return
         */
        abstract public int getIndexOfStoredList();

        /**
         * get sub-index of regions which contain multiple regions like exon and intro
         *
         * @return
         */
        public int getIndexOfSubRegion() {
            return -1;
        }

        /**
         * help sort the intro and exon with the same index
         *
         * @return exon return -1, intro return 0
         */
        public int diffExonAndIntro() {
            return -1;
        }

        public int getLen() {
            return range.end() - range.start();
        }

        public String locate(int position, byte strand) {
            int relativeDistance = (strand == 0 ? position - range.start() : range.end() - position + 1);
            if (relativeDistance > 0) {
                return getRegion() + "+" + relativeDistance;
            } else {
                return getRegion() + relativeDistance;
            }
        }

        @Override
        public int compareTo(TranscriptRegion o) {
            int status = Integer.compare(getOrderOfRegion(), o.getOrderOfRegion());
            if (status == 0) {
                status = Integer.compare(getIndexOfSubRegion(), o.getIndexOfSubRegion());
            }
            return status == 0 ? Integer.compare(diffExonAndIntro(), o.diffExonAndIntro()) : status;
        }
    }

    static class LeftIntergenic extends TranscriptRegion {
        private static final int orderOfRegion = 1;
        private static final int indexOfStoredList = -1;

        private static final String region = "left intergenic region";
        private static final String FLAG = "LeftIntergenic";

        public LeftIntergenic(int pos, int end) {
            super(pos, end);
        }

        @Override
        String getRegion() {
            return region;
        }

        @Override
        public String locate(int position, byte strand) {
            return FLAG;
        }

        @Override
        public int getOrderOfRegion() {
            return orderOfRegion;
        }

        @Override
        public int getIndexOfStoredList() {
            return indexOfStoredList;
        }
    }

    static class Upstream extends TranscriptRegion {
        private static final int orderOfRegion = 2;
        private static final int indexOfStoredList = 0;


        private static final String region = "upstream";
        private static final Bytes bytesRegion = new Bytes(region);

        public Upstream(int pos, int end) {
            super(pos, end);
        }

        @Override
        String getRegion() {
            return region;
        }

        @Override
        public String locate(int position, byte strand) {
            int relativeDistance = strand == 0 ? position - range.start() : range.end() - position + 1;
            if (relativeDistance >= 0) {
                return region + "+" + relativeDistance;
            } else {
                return region + relativeDistance;
            }
        }

        @Override
        public int getOrderOfRegion() {
            return orderOfRegion;
        }

        @Override
        public int getIndexOfStoredList() {
            return indexOfStoredList;
        }
    }

    static class UTR5 extends TranscriptRegion {
        private static final int orderOfRegion = 3;
        private static final int indexOfStoredList = 1;

        final int index;
        private static final String region = "UTR5";
        private static final Bytes beytsRegion = new Bytes(region);

        public UTR5(int pos, int end, int index) {
            super(pos, end);
            this.index = index;
        }

        @Override
        String getRegion() {
            return region;
        }

        @Override
        public int getOrderOfRegion() {
            return orderOfRegion;
        }

        @Override
        public int getIndexOfSubRegion() {
            return index;
        }

        @Override
        public int getIndexOfStoredList() {
            return indexOfStoredList;
        }
    }

    static class CodingExon extends Exon {
        private static final int orderOfRegion = 4;
        private static final int indexOfStoredList = 2;
        private static final Bytes bytesRegion = new Bytes("CodingExon");

        public CodingExon(int pos, int end, int exonIndex) {
            super(pos, end, exonIndex);
        }

        @Override
        public int getIndexOfStoredList() {
            return indexOfStoredList;
        }

        @Override
        public int getOrderOfRegion() {
            return orderOfRegion;
        }
    }

    static class Exon extends TranscriptRegion {
        private static final int orderOfRegion = 5;
        private static final int indexOfStoredList = 3;

        int index;
        private static final String region = "Exon";
        private static final Bytes bytesRegion = new Bytes("Exon");

        public Exon(int pos, int end, int exonIndex) {
            super(pos, end);
            this.index = exonIndex;
        }

        @Override
        String getRegion() {
            return region;
        }

        @Override
        public String locate(int position, byte strand) {
            int reltaiveDistance = strand == 0 ? position - range.start() : range.end() - position + 1;
            if (reltaiveDistance >= 0) {
                return region + index + "+" + reltaiveDistance;
            } else {
                return region + index + reltaiveDistance;
            }
        }

        @Override
        public int getOrderOfRegion() {
            return orderOfRegion;
        }

        @Override
        public int getIndexOfSubRegion() {
            return index;
        }

        @Override
        public int getIndexOfStoredList() {
            return indexOfStoredList;
        }
    }

    static class Intro extends TranscriptRegion {
        private static final int orderOfRegion = 6;
        private static final int indexOfStoredList = 4;

        int index;
        private static final String region = "Intro";
        private static final Bytes bytesRegion = new Bytes(region);

        public Intro(int pos, int end, int introIndex) {
            super(pos, end);
            this.index = introIndex;
        }

        @Override
        String getRegion() {
            return region;
        }

        @Override
        public String locate(int position, byte strand) {
            int relativeDistance = strand == 0 ? position - range.start() : range.end() - position + 1;
            if (relativeDistance >= 0) {
                return region + index + "+" + relativeDistance;
            } else {
                return region + index + relativeDistance;
            }
        }

        @Override
        public int getOrderOfRegion() {
            return orderOfRegion;
        }

        @Override
        public int getIndexOfSubRegion() {
            return index;
        }

        @Override
        public int getIndexOfStoredList() {
            return indexOfStoredList;
        }

        @Override
        public int diffExonAndIntro() {
            return 1;
        }
    }

    static class UTR3 extends TranscriptRegion {
        private static final int orderOfRegion = 7;
        private static final int indexOfStoredList = 5;

        final int index;
        private static final String region = "UTR3";
        private static final Bytes bytesRegion = new Bytes("UTR3");

        public UTR3(int pos, int end, int index) {
            super(pos, end);
            this.index = index;
        }

        @Override
        String getRegion() {
            return region;
        }

        @Override
        public int getOrderOfRegion() {
            return orderOfRegion;
        }

        @Override
        public int getIndexOfSubRegion() {
            return index;
        }

        @Override
        public int getIndexOfStoredList() {
            return indexOfStoredList;
        }
    }

    static class Downstream extends TranscriptRegion {
        private static final int orderOfRegion = 8;
        private static final int indexOfStoredList = 6;

        private static final String region = "downstream";
        private static final Bytes bytesRegion = new Bytes("downstream");

        public Downstream(int pos, int end) {
            super(pos, end);
        }

        @Override
        String getRegion() {
            return region;
        }

        @Override
        public String locate(int position, byte strand) {
            int relativeDistance = strand == 0 ? position - range.start() : range.end() - position + 1;
            if (relativeDistance >= 0) {
                return region + "+" + relativeDistance;
            } else {
                return region + relativeDistance;
            }
        }

        @Override
        public int getOrderOfRegion() {
            return orderOfRegion;
        }

        @Override
        public int getIndexOfStoredList() {
            return indexOfStoredList;
        }
    }

    static class RightIntergenic extends TranscriptRegion {
        private static final int orderOfRegion = 9;
        private static final int indexOfStoredList = -1;


        private static final String region = "right intergenic region";
        private static final String FLAG = "RightIntergenic";

        public RightIntergenic(int pos, int end) {
            super(pos, end);
        }

        @Override
        String getRegion() {
            return region;
        }

        @Override
        public String locate(int position, byte strand) {
            return FLAG;
        }

        @Override
        public int getOrderOfRegion() {
            return orderOfRegion;
        }

        @Override
        public int getIndexOfStoredList() {
            return indexOfStoredList;
        }
    }

    public static void parseOverlapFloatList(float[] overlap, ByteStream cache){
        boolean existLast = false;
        for (int i = 0; i < overlap.length; i++) {
            if (overlap[i] != 0) {
                if (existLast) {
                    cache.write(Constant.UNDERLINE);
                }
                switch (i) {
                    case 0:
                        cache.write(Upstream.bytesRegion);
                        break;
                    case 1:
                        cache.write(UTR5.beytsRegion);
                        break;
                    case 2:
                        cache.write(CodingExon.bytesRegion);
                        break;
                    case 3:
                        cache.write(Exon.bytesRegion);
                        break;
                    case 4:
                        cache.write(Intro.bytesRegion);
                        break;
                    case 5:
                        cache.write(UTR3.bytesRegion);
                        break;
                    case 6:
                        cache.write(Downstream.bytesRegion);
                        break;
                }
                cache.write(Constant.COLON);
                cache.write(ASCIIUtility.toASCII(overlap[i]));
                existLast = true;
            }
        }
    }
}
