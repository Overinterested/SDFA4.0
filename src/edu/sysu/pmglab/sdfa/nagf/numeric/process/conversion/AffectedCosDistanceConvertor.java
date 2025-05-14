package edu.sysu.pmglab.sdfa.nagf.numeric.process.conversion;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.AffectedNumericConvertor;

/**
 * @author Wenjie Peng
 * @create 2024-10-18 23:08
 * @description
 */
public class AffectedCosDistanceConvertor implements AffectedNumericConvertor {

    @Override
    public String convert(float[] numericValues) {
        boolean allZeros = true;
        boolean noncoding = numericValues[CODING_EXON_INDEX] == 0 && numericValues[UTR3_INDEX] == 0 && numericValues[UTR5_INDEX] == 0;
        // 点积部分：A与全1向量的点积为向量A所有元素的和
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0; // 全1向量的模

        for (int i = 0; i < numericValues.length; i++) {
            float tmp = numericValues[i];
            if (!noncoding && i == EXON_INDEX) {
                continue;
            }
            if (tmp != 0) {
                allZeros = false;
                dotProduct += tmp; // A与全1向量点积就是A元素的和
                normA += Math.pow(tmp, 2); // 计算A的模的平方
            }
        }
        if (allZeros) {
            return String.valueOf(1);
        }
        normA = Math.sqrt(normA);
        normB = Math.sqrt(numericValues.length); // 全1向量的模为 sqrt(向量长度)

        // 计算余弦相似度
        double cosineSimilarity = dotProduct / (normA * normB);

        // 计算余弦距离
        double value = 1.0 - cosineSimilarity;
        return String.format("%.2f", value);
    }

    @Override
    public void convertTo(float[] numericValues, ByteStream cache) {
        cache.write(ASCIIUtility.toASCII(convert(numericValues), Constant.CHAR_SET));
    }
}
