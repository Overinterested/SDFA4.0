package edu.sysu.pmglab.sdfa.annotation.output.frame;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.easytools.calculator.TranscriptCalculator;
import edu.sysu.pmglab.sdfa.annotation.source.SourceMeta;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRNARecord;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.util.Arrays;

/**
 * @author Wenjie Peng
 * @create 2024-09-18 02:14
 * @description
 */
public class GenomeSourceOutputFrame implements SourceOutputFrame<SourceRNARecord> {
    protected final SourceMeta sourceMeta;
    private static final List<Bytes> outputColumnNames = new List<>(new Bytes[]{
            new Bytes("Range")
    });

    public GenomeSourceOutputFrame(SourceMeta sourceMeta) {
        this.sourceMeta = sourceMeta;
    }

    public List<Bytes> getOutputColumnNames() {
        return outputColumnNames;
    }

    public void write(ISDSV sv, List<SourceRNARecord> records, int startIndex, int endIndex, ByteStream cache) {
        int size = endIndex - startIndex + 1;
        TranscriptCalculator[] calcs = new TranscriptCalculator[size];
        List<TranscriptCalculator.TranscriptRegion>[] overlapsList = new List[size];
        int endLoop = size - 1;
        float[] overlappedValues = new float[7];
        for (int i = startIndex; i <= endIndex; i++) {
            int index = i - startIndex;
            SourceRNARecord sourceRNARecord = records.fastGet(index);
            TranscriptCalculator calc = sourceRNARecord.getCalc();
            calcs[i - startIndex] = calc;
            List<TranscriptCalculator.TranscriptRegion> overlaps = calc.overlap(sv);
            cache.write(ASCIIUtility.toASCII(sourceRNARecord.geneRNAName(), Constant.CHAR_SET));
            cache.write(Constant.COLON);
            cache.write(ASCIIUtility.toASCII(calc.locateRange(overlaps, sv.getCoordinate()), Constant.CHAR_SET));

            cache.write(Constant.AT);
            overlapsList[i - startIndex] = overlaps;
            calcs[i-startIndex].locateProportion(overlapsList[i-startIndex], sv, overlappedValues);
            TranscriptCalculator.parseOverlapFloatList(overlappedValues, cache);
            if (i != endLoop) {
                cache.write(Constant.SEMICOLON);
            }
        }
    }

}
