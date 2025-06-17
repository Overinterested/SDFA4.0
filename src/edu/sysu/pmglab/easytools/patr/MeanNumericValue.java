package edu.sysu.pmglab.easytools.patr;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.RNAAffectedCalculator;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-06-16 07:13
 * @description
 */
public class MeanNumericValue {

    private static Bytes EMPTY = new Bytes("0,0,0,0,0,0,0");
    public static void outputMean(String inputNumericFile, String output, boolean geneLevel) throws IOException {
        File outputFile = new File(output);
        ByteStream cache = new ByteStream();
        ByteStream writerCache = new ByteStream();
        ReaderStream readerStream = LiveFile.of(inputNumericFile).openAsText();
        WriterStream writerStream = new WriterStream(outputFile, WriterStream.Option.DEFAULT);

        int sampleStartCol = geneLevel ? 2 : 3;
        BytesSplitter tabSplitter = new BytesSplitter(Constant.TAB);
        BytesSplitter commaSplitter = new BytesSplitter(Constant.COMMA);

        readerStream.readline(cache);
        cache.clear();
        writeHeader(writerStream, geneLevel);
        double[] sumOfFeatures = new double[7];
        int indexOfCol, indexOfFeature;
        while (readerStream.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            tabSplitter.init(line);
            indexOfCol = 0;
            while (tabSplitter.hasNext()) {
                Bytes item = tabSplitter.next();
                if (indexOfCol++ < sampleStartCol) {
                    writerCache.write(item);
                    writerStream.write(Constant.TAB);
                    continue;
                }
                if (item.equals(EMPTY)){
                    continue;
                }
                commaSplitter.init(item);
                indexOfFeature = 0;
                while (commaSplitter.hasNext()) {
                    Bytes featureValue = commaSplitter.next();
                    double value = featureValue.toDouble();
                    sumOfFeatures[indexOfFeature++] += value;
                }
            }
            writeLine(writerCache, sumOfFeatures);
            writerStream.write(writerCache.toBytes());
            cache.clear();
            writerCache.clear();
        }
        cache.close();
        writerCache.close();
        readerStream.close();
        writerStream.close();
    }

    private static void writeHeader(WriterStream writerStream, boolean geneLevel) throws IOException {
        writerStream.write(ASCIIUtility.toASCII("Gene\t", Constant.CHAR_SET));
        if (!geneLevel) {
            writerStream.write(ASCIIUtility.toASCII("RNA\t", Constant.CHAR_SET));
        }
        writerStream.write(ASCIIUtility.toASCII("Upstream\t", Constant.CHAR_SET));
        writerStream.write(ASCIIUtility.toASCII("UTR5\t", Constant.CHAR_SET));
        writerStream.write(ASCIIUtility.toASCII("CodingExon\t", Constant.CHAR_SET));
        writerStream.write(ASCIIUtility.toASCII("Exon\t", Constant.CHAR_SET));
        writerStream.write(ASCIIUtility.toASCII("Intro\t", Constant.CHAR_SET));
        writerStream.write(ASCIIUtility.toASCII("UTR3\t", Constant.CHAR_SET));
        writerStream.write(ASCIIUtility.toASCII("Downstream\n", Constant.CHAR_SET));
    }

    private static void writeLine(ByteStream cache, double[] featureSumValues) {
        int length = featureSumValues.length;
        int endLoop = length - 1;
        for (int i = 0; i < length; i++) {
            cache.write(ASCIIUtility.toASCII(featureSumValues[i]));
            cache.write(i == endLoop ? Constant.NEWLINE : Constant.TAB);
            featureSumValues[i] = 0;
        }
    }

    public static void main(String[] args) throws IOException {
        MeanNumericValue.outputMean(
                "/Users/wenjiepeng/Desktop/PaperWriter/SV/PATR/results/1019_fangli_samples/cuteSV-2.1.1/gene_numeric_output.txt",
                "/Users/wenjiepeng/Desktop/PaperWriter/SV/PATR/results/1019_fangli_samples/cuteSV-2.1.1/mean_gene_numeric_output.txt",
                true
        );
    }
}
