package edu.sysu.pmglab.sdfa.nagf.sv;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.sdfa.annotation.output.frame.GenomeSourceOutputFrame;
import edu.sysu.pmglab.sdfa.annotation.source.SourceMeta;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRNARecord;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.AffectedNumericConvertor;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

/**
 * @author Wenjie Peng
 * @create 2024-11-15 19:04
 * @description
 */
public class NAGFGenomeOutputFrame extends GenomeSourceOutputFrame {
    static boolean geneLevel;
    String affectedNumericConvertorName = "full";
    private static List<Bytes> nagfOutputColumnNames = List.wrap(
            new Bytes[]{
                    new Bytes("NAGF_Values")
            }
    );

    public NAGFGenomeOutputFrame(SourceMeta sourceMeta) {
        super(sourceMeta);
    }

    public List<Bytes> getOutputColumnNames() {
        return nagfOutputColumnNames;
    }

    @Override
    public void write(ISDSV sv, List<SourceRNARecord> records, int startIndex, int endIndex, ByteStream cache) {
        AffectedNumericConvertor affectedNumericConvertor = AffectedNumericConvertor.getByDefault(affectedNumericConvertorName);
        if (geneLevel) {
            SourceRNARecord firstSourceRecord = records.fastGet(0);
            String geneName = firstSourceRecord.getNameOfGene();
            float[] numericValues = firstSourceRecord.getCalc().locateProportion(sv);
            for (int i = startIndex + 1; i <= endIndex; i++) {
                int index = i - startIndex;
                SourceRNARecord sourceRNARecord = records.fastGet(index);
                if (sourceRNARecord.getNameOfGene().equals(geneName)) {
                    float[] tmpNumericValues = sourceRNARecord.getCalc().locateProportion(sv);
                    for (int j = 0; j < numericValues.length; j++) {
                        numericValues[j] = Math.max(numericValues[j], tmpNumericValues[j]);
                        numericValues[j] = Math.min(numericValues[j], 1f);
                    }
                } else {
                    cache.write(ASCIIUtility.toASCII(geneName,Constant.CHAR_SET));
                    cache.write(Constant.COLON);
                    cache.write(ASCIIUtility.toASCII(affectedNumericConvertor.convert(numericValues),Constant.CHAR_SET));
                    cache.write(Constant.SEMICOLON);
                    geneName = sourceRNARecord.getNameOfGene();
                    numericValues = sourceRNARecord.getCalc().locateProportion(sv);
                }
            }
            cache.write(ASCIIUtility.toASCII(geneName,Constant.CHAR_SET));
            cache.write(Constant.COLON);
            cache.write(ASCIIUtility.toASCII(affectedNumericConvertor.convert(numericValues),Constant.CHAR_SET));
            cache.write(Constant.SEMICOLON);
        } else {
            // rna level
            for (int i = startIndex; i <= endIndex; i++) {
                int index = i - startIndex;
                SourceRNARecord sourceRNARecord = records.fastGet(index);
                float[] numericValues = sourceRNARecord.getCalc().locateProportion(sv);
                cache.write(ASCIIUtility.toASCII(sourceRNARecord.geneRNAName(),Constant.CHAR_SET));
                cache.write(Constant.COLON);
                cache.write(ASCIIUtility.toASCII(affectedNumericConvertor.convert(numericValues),Constant.CHAR_SET));
                cache.write(Constant.SEMICOLON);
            }
        }
    }

    public NAGFGenomeOutputFrame setAffectedNumericConvertorName(String affectedNumericConvertorName) {
        this.affectedNumericConvertorName = affectedNumericConvertorName;
        return this;
    }
}
