package edu.sysu.pmglab.tmp;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.io.writer.WriterStream;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-22 09:28
 * @description
 */
public class SampleSplit {
    public static void main(String[] args) throws IOException {
        ByteStream cache = new ByteStream();
        ByteStream writeCache = new ByteStream();
        String prefix = "/home/pwj/ukbb_sv/sdf/";
        byte[] prefixBytes = ASCIIUtility.toASCII(prefix, Constant.CHAR_SET);
        BytesSplitter splitter = new BytesSplitter(Constant.UNDERLINE);
        ReaderStream readerStream = new ReaderStream("/Users/wenjiepeng/Desktop/SDFA_4.0/UKB/all_sdf_file_names.txt", ReaderStream.Option.DEFAULT);
        WriterStream writerStream = new WriterStream(new File("/Users/wenjiepeng/Desktop/SDFA_4.0/UKB/sample_file_map.txt"), WriterStream.Option.DEFAULT);
        while (readerStream.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            splitter.init(line);
            Bytes sampleName = splitter.next();
            writerStream.write(prefixBytes);
            writerStream.write(cache.toBytes());
            writerStream.write(Constant.TAB);
            writerStream.write(sampleName);
            writerStream.write(Constant.NEWLINE);
            cache.clear();
            writerStream.write(writeCache.toBytes());
            writeCache.clear();
        }
        writerStream.close();
        readerStream.close();
    }
}
