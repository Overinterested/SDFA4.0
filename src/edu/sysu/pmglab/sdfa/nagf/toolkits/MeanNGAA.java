package edu.sysu.pmglab.sdfa.nagf.toolkits;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.io.writer.WriterStream;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Wenjie Peng
 * @create 2025-03-31 07:54
 * @description
 */
public class MeanNGAA {
    String file;
    String outputFile;
    boolean geneLevel = true;

    public MeanNGAA(String file) {
        this.file = file;
    }

    public MeanNGAA(String file, boolean geneLevel) {
        this.file = file;
        this.geneLevel = geneLevel;
    }

    public MeanNGAA setOutputFile(String outputFile) {
        this.outputFile = outputFile;
        return this;
    }

    public void submit() throws IOException {
        ByteStream cache = new ByteStream();
        ByteStream writeCache = new ByteStream();
        ReaderStream readerStream = new ReaderStream(file, ReaderStream.Option.DEFAULT);
        WriterStream writerStream = new WriterStream(new File(outputFile), WriterStream.Option.DEFAULT);
        int itemStart = geneLevel ? 0 : 1;
        int colStart = geneLevel ? 2 : 3;
        BytesSplitter tabSplitter = new BytesSplitter(Constant.TAB);
        BytesSplitter commaSplitter = new BytesSplitter(Constant.COMMA);
        readerStream.readline(cache);
        double[] res = new double[7];
        cache.clear();
        int colIndex, valueIndex = 0;
        while (readerStream.readline(cache) != -1) {
            tabSplitter.init(cache.toBytes());
            colIndex = 0;
            while (tabSplitter.hasNext()) {
                Bytes item = tabSplitter.next();
                if (colIndex <= itemStart) {
                    writeCache.write(item.detach());
                    writeCache.write(Constant.TAB);
                    colIndex++;
                    continue;
                }
                if (colIndex >= colStart) {
                    commaSplitter.init(item);
                    valueIndex = 0;
                    while (commaSplitter.hasNext()) {
                        Bytes item1 = commaSplitter.next();
                        double value = item1.toDouble();
                        if (value != 0) {
                            res[valueIndex] += value;
                        }
                        valueIndex++;
                    }
                }
                colIndex++;
            }
            for (int i = 0; i < res.length; i++) {
                writeCache.write(ASCIIUtility.toASCII(res[i] / (colIndex-colStart+1)));
                if (i != 6) {
                    writeCache.write(Constant.TAB);
                } else {
                    writeCache.write(Constant.NEWLINE);
                }
            }
            Arrays.fill(res, 0);
            writerStream.write(writeCache.toBytes());
            writeCache.clear();
            cache.clear();
        }
        writerStream.close();
        readerStream.close();
    }

    public static void main(String[] args) throws IOException {
        new MeanNGAA("/Users/pwj/Documents/paper_writer/NGAA/population/sniffles/GQ_10_AND_SIZE_20_1e5_res/gene_numeric_output.txt")
                .setOutputFile("/Users/pwj/Documents/paper_writer/NGAA/population/sniffles/GQ_10_AND_SIZE_20_1e5_res/mean.txt")
                .submit();
    }
}
