package edu.sysu.pmglab.sdfa.annotation.output.convertor;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.function.Function;

/**
 * @author Wenjie Peng
 * @create 2024-09-09 02:01
 * @description
 */
public abstract class MathOutputConvertor implements OutputConvertor {
    Function<double[], byte[]> function;
    public ByteStream cache = new ByteStream();
    private static final DecimalFormat df = new DecimalFormat("0.##");

    @Override
    public Bytes output(List<IRecord> relatedSourceRecords, int columnRelatedIndex) {
        cache.clear();
        Number number;
        int numOfElements = relatedSourceRecords.size();
        double[] values = new double[numOfElements];
        for (int i = 0; i < numOfElements; i++) {
            Object obj = relatedSourceRecords.fastGet(i).get(columnRelatedIndex);
            if (obj instanceof Bytes) {
                number = ((Bytes) obj).toDouble();
                values[i] = number.doubleValue();
            } else if (obj instanceof Number) {
                number = (Number) obj;
                values[i] = number.doubleValue();
            }
        }
        return new Bytes(function.apply(values));
    }

    @Override
    public Bytes output(List<IRecord> relatedSourceRecords, IntList columnRelatedIndexes) {
        return output(relatedSourceRecords, columnRelatedIndexes.fastGet(0));
    }

    public static class MinOutputConvertor extends MathOutputConvertor {
        public MinOutputConvertor() {
            this.function = (values -> {
                cache.clear();
                double min = Float.MAX_VALUE;
                for (double value : values) {
                    if (value <= min) {
                        min = value;
                    }
                }
                cache.write(ASCIIUtility.toASCII(df.format(min), Constant.CHAR_SET));
                return cache.toBytes().bytes();
            });
        }
    }

    public static class MaxOutputConvertor extends MathOutputConvertor {
        public MaxOutputConvertor() {
            this.function = (values -> {
                cache.clear();
                double max = Float.MIN_VALUE;
                for (double value : values) {
                    if (value >= max) {
                        max = value;
                    }
                }
                cache.write((ASCIIUtility.toASCII(df.format(max),Constant.CHAR_SET)));
                return cache.toBytes().bytes();
            });
        }
    }

    public static class AverageOutputConvertor extends MathOutputConvertor {
        public AverageOutputConvertor() {
            this.function = (values -> {
                cache.clear();
                double sum = 0;
                for (double value : values) {
                    sum += value;
                }
                cache.write((ASCIIUtility.toASCII(df.format((float)sum / values.length),Constant.CHAR_SET)));
                return cache.toBytes().bytes();
            });
        }
    }

    public static class MiddleOutputConvertor extends MathOutputConvertor {
        public MiddleOutputConvertor() {
            this.function = (values -> {
                cache.clear();
                Arrays.sort(values);
                int length = values.length;
                if (length % 2 == 0) {
                    double middle = (values[length / 2] + values[length / 2 - 1]) / 2;
                    cache.write((ASCIIUtility.toASCII(df.format(middle),Constant.CHAR_SET)));
                } else {
                    cache.write((ASCIIUtility.toASCII(df.format((float) values[values.length / 2]),Constant.CHAR_SET)));
                }
                return cache.toBytes().bytes();
            });
        }
    }
}
