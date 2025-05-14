package edu.sysu.pmglab.sdfa.nagf.simulation.bias;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.io.writer.WriterStream;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2025-01-07 00:44
 * @description
 */
public class CuteSVBias {
    static DecimalFormat df = new DecimalFormat("0.##");
    static List<String> keys = List.wrap(new String[]{
            "START",
            "END",
            "SVLEN",
            "SVTYPE",
    });
    static String map = "winnowmap";
    static byte[] SIM = "Sim".getBytes();
    static String caller = "Sniffles2";

    public static void main(String[] args) throws IOException {
        File output = new File(
                "/Users/wenjiepeng/Desktop/SDFA3.0/nagf/simulation/bias/winnowmap/sniffles2/concat.txt"
        );
        boolean exists = output.exists();
        WriterStream writerStream = new WriterStream(
                output,
                output.exists() ? WriterStream.Option.APPEND : WriterStream.Option.DEFAULT
        );
        if (!exists) {
            writerStream.write(ASCIIUtility.toASCII("Map\tCaller\tTYPE\tLength\tStartBias\tLengthBias\n", Constant.CHAR_SET));
        }

        String root = "/Users/wenjiepeng/Desktop/SDFA3.0/nagf/simulation/bias/winnowmap/sniffles2";
        File[] files = new File(root).listFiles(pathname -> pathname.getName().endsWith("vcf"));
        for (int i = 0; i < files.length; i++) {
            LiveFile file = LiveFile.of(
                    files[i]
            );
            ReaderStream readerStream = file.openAsText();
            List<Bytes> sim;
            List<Bytes> call;
            List<Bytes> var1 = new List<>();
            List<Bytes> var2 = new List<>();
            ByteStream cache = new ByteStream();
            while (readerStream.readline(cache) != -1) {
//                var1.addAll(() -> cache.toBytes().detach().split(Constant.TAB));
                cache.clear();
                readerStream.readline(cache);
//                var2.addAll(() -> cache.toBytes().detach().split(Constant.TAB));
                if (var1.fastGet(2).startsWith(SIM)) {
                    sim = var1;
                    call = var2;
                } else {
                    sim = var2;
                    call = var1;
                }
                List<Bytes> simItem = findAll(sim.fastGet(7).split(Constant.SEMICOLON));
                List<Bytes> callItem = findAll(call.fastGet(7).split(Constant.SEMICOLON));
                simItem.fastSet(0, sim.fastGet(1));
                callItem.fastSet(0, call.fastGet(1));
                cache.clear();
                writeTo(cache, simItem, callItem);
                writerStream.write(cache.toBytes());
                cache.clear();
            }
            readerStream.close();
        }
        writerStream.close();
    }

    public static List<Bytes> findAll(Iterator<Bytes> items) {
        int size = keys.size();
        List<Bytes> res = List.wrap(new Bytes[size]);
        while (items.hasNext()) {
            Bytes item = items.next();
            for (int i = 1; i < size; i++) {
                String s = keys.fastGet(i);
                if (item.startsWith(s.getBytes())) {
                    Iterator<Bytes> iterator = item.split(Constant.EQUAL);
                    iterator.next();
                    res.fastSet(i, iterator.next());
                    break;
                }
            }
        }
        return res;
    }

    public static void writeTo(ByteStream cache, List<Bytes> sim, List<Bytes> call) {
        cache.write(ASCIIUtility.toASCII(map, Constant.CHAR_SET));
        cache.write(Constant.TAB);
        cache.write(ASCIIUtility.toASCII(caller, Constant.CHAR_SET));
        cache.write(Constant.TAB);
        cache.write(sim.fastGet(3));
        cache.write(Constant.TAB);
        int trueLen = sim.fastGet(2).toInt();
        if (Math.abs(trueLen) <= 50) {
            cache.clear();
            return;
        }
        cache.write(ASCIIUtility.toASCII(trueLen));
        cache.write(Constant.TAB);
        // start
        int simStart = sim.fastGet(0).toInt();
        int callStart = call.fastGet(0).toInt();
        double v = (simStart - callStart) / ((double) trueLen);
        cache.write(ASCIIUtility.toASCII(df.format(v), Constant.CHAR_SET));
        cache.write(Constant.TAB);
        // length
        int simLen = call.fastGet(2).toInt();
        cache.write(
                ASCIIUtility.toASCII(
                        df.format((simLen - trueLen) / ((double) trueLen)),
                        Constant.CHAR_SET
                )
        );
        cache.write(Constant.NEWLINE);
    }
}
