package edu.sysu.pmglab.sdfa.nagf.numeric.process.conversion;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.AffectedNumericConvertor;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-10-17 02:13
 * @description
 */
public class AffectedStringConvertor implements AffectedNumericConvertor {

    @Override
    public String convert(float[] numericValues) {
        ByteStream cache = new ByteStream();
        int size = numericValues.length;
        for (int i = 0; i < size; i++) {
            cache.write(ASCIIUtility.toASCII(numericValues[i]));
            if (i != size - 1) {
                cache.write(Constant.COMMA);
            }
        }
        return cache.toBytes().toString();
    }

    @Override
    public void convertTo(float[] numericValues, ByteStream cache) {
        int size = numericValues.length;
        for (int i = 0; i < size; i++) {
            cache.write(ASCIIUtility.toASCII(numericValues[i]));
            if (i != size - 1) {
                cache.write(Constant.COMMA);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ByteStream cache = new ByteStream();
        float[] values = new float[]{0.01f,0.2f,0.3f};
        for (int i = 0; i < values.length; i++) {
            cache.write(ASCIIUtility.toASCII(values[i]));
            cache.write(Constant.COMMA);
        }
        WriterStream writerStream = new WriterStream(
                new File("/Users/wenjiepeng/Desktop/SDFA/ukbb_disease/concat/res/1.txt"),
                WriterStream.Option.DEFAULT
        );
        writerStream.write(ASCIIUtility.toASCII(cache.toBytes().toString(),Constant.CHAR_SET));
        writerStream.close();
        System.out.println(cache.toString());
    }
}
