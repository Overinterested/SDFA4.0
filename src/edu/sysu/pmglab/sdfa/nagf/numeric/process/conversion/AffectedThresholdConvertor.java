package edu.sysu.pmglab.sdfa.nagf.numeric.process.conversion;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.AffectedNumericConvertor;

/**
 * @author Wenjie Peng
 * @create 2024-10-17 20:52
 * @description
 */
public class AffectedThresholdConvertor implements AffectedNumericConvertor {
    static int UPSTREAM_INDEX = 0;
    static int UTR5_INDEX = 1;
    static int CODING_EXON_INDEX = 2;
    static int EXON_INDEX = 3;
    static int INTRO_INDEX = 4;
    static int UTR3_INDEX = 5;
    static int DOWNSTREAM_INDEX = 6;

    @Override
    public String convert(float[] numericValues) {
        double value = numericValues[CODING_EXON_INDEX] * Math.pow(2, 4) +
                numericValues[EXON_INDEX] * Math.pow(2, 3) +
                numericValues[UTR3_INDEX] * Math.pow(2, 2) + numericValues[UTR5_INDEX] * Math.pow(2, 2) +
                numericValues[UPSTREAM_INDEX] * Math.pow(2, 1) +  numericValues[DOWNSTREAM_INDEX] * Math.pow(2, 1) +
                numericValues[INTRO_INDEX] * Math.pow(2, 0);
        return String.format("%.2f", value);
    }

    @Override
    public void convertTo(float[] numericValues, ByteStream cache) {
        cache.write(ASCIIUtility.toASCII(convert(numericValues), Constant.CHAR_SET));
    }
}
