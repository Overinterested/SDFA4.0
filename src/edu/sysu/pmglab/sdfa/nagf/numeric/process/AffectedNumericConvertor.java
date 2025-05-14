package edu.sysu.pmglab.sdfa.nagf.numeric.process;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.easytools.Constant;

import java.util.HashMap;

/**
 * @author Wenjie Peng
 * @create 2024-09-26 03:08
 * @description
 */
public interface AffectedNumericConvertor {

    int UPSTREAM_INDEX = 0;
    int UTR5_INDEX = 1;
    int CODING_EXON_INDEX = 2;
    int EXON_INDEX = 3;
    int INTRO_INDEX = 4;
    int UTR3_INDEX = 5;
    int DOWNSTREAM_INDEX = 6;

    HashMap<String, AffectedNumericConvertor> cache = new HashMap<>();
    AffectedNumericConvertor DEFAULT = new AffectedNumericConvertor() {
        @Override
        public String convert(float[] numericValues) {
            float upstream = numericValues[0];
            float utr5 = numericValues[1];
            float codingExon = numericValues[2];
            float exon = numericValues[3];
            float intro = numericValues[4];
            float utr3 = numericValues[5];
            float downstream = numericValues[6];
            double value = Math.pow(2, 6) * codingExon +
                    Math.pow(2, 5) * utr5 +
                    Math.pow(2, 5) * utr3;
            if (value == 0) {
                value += Math.pow(2, 4) * exon;
            }
            value += Math.pow(2, 3) * intro + Math.pow(2, 2) * (upstream + downstream);
            return String.format("%.2f", value);
        }

        @Override
        public void convertTo(float[] numericValues, ByteStream cache) {
            cache.write(ASCIIUtility.toASCII(convert(numericValues), Constant.CHAR_SET));
        }
    };

    /**
     * for reference transcript or gene elements, we all take 7 features to represent the effect of SV on them:
     *     1: UPSTREAM_INDEX;
     *     2: UTR5_INDEX;
     *     3: CODING_EXON_INDEX;
     *     4: EXON_INDEX;
     *     5: INTRO_INDEX;
     *     6: UTR3_INDEX;
     *     7: DOWNSTREAM_INDEX;
     * @return feature number(at 7 by default)
     */
    default int sizeOfConversionArray(){
        return 7;
    };

    /**
     * convert the numeric annotation values into a string type value
     * @param numericValues
     * @return output string value
     */
    String convert(float[] numericValues);

    void convertTo(float[] numericValues, ByteStream cache);

    static AffectedNumericConvertor getByDefault(String name) {
        if (name == null) {
            return DEFAULT;
        }
        return cache.getOrDefault(name, DEFAULT);
    }

    static void add(String name, AffectedNumericConvertor affectedNumericConvertor){
        cache.put(name, affectedNumericConvertor);
    }
}
