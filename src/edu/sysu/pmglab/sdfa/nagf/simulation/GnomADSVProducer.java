package edu.sysu.pmglab.sdfa.nagf.simulation;

import edu.sysu.pmglab.bytecode.*;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.io.writer.WriterStream;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2024-12-29 02:40
 * @description
 */
public class GnomADSVProducer {
    private static final byte[] BND = "BND".getBytes();
    private static final byte[] CTX = "CTX".getBytes();
    private static final byte[] CPX = "CPX".getBytes();
    public static void main(String[] args) throws IOException {
        ByteStream cache = new ByteStream();
        ReaderStream readerStream = new ReaderStream("/Users/wenjiepeng/Downloads/gnomad.v4.1.sv.sites.bed", ReaderStream.Option.DEFAULT);
        WriterStream writerStream = new WriterStream(
                new File("/Users/wenjiepeng/Desktop/SDFA3.0/nagf/simulation/gnomAD_SV_exclude_AF.txt"),
                WriterStream.Option.DEFAULT);
        Bytes name = new Bytes("Unassigned");
        readerStream.readline(cache);
        int indexOfAF = -1;
        List<Bytes> split1 = new List<>();
        Iterator<Bytes> iterator = cache.toBytes().split(Constant.TAB);
        while (iterator.hasNext()) split1.add(iterator.next().detach());

        for (int i = 0; i < split1.size(); i++) {
            if (split1.fastGet(i).equals(new Bytes("AF"))){
                indexOfAF = i;
                break;
            }
        }
        List<Bytes> store1 = split1.subList(0, 5);
        store1.fastSet(3, name);
        store1.add(split1.fastGet(indexOfAF));
        writerStream.writeChar(store1.toString("\t"));
        writerStream.write(Constant.NEWLINE);
        cache.clear();
        while (readerStream.readline(cache) != -1) {
            List<Bytes> split = new List<>();
            iterator = cache.toBytes().split(Constant.TAB);
            while (iterator.hasNext()) split.add(iterator.next().detach());
            Bytes type = split.fastGet(4);
            if (type.startsWith(BND)
                    ||type.startsWith(CTX)
                    ||type.startsWith(CPX)
                    || type.indexOf(new Bytes(":")) != -1) {
                cache.clear();
                continue;
            }
            List<Bytes> store = split.subList(0, 5);
            store.fastSet(3, name);
            Bytes bytesOfAF = split.fastGet(indexOfAF);
            if (bytesOfAF.startsWith(Constant.NA)){
                cache.clear();
                continue;
            }
            store.add(bytesOfAF);
            writerStream.writeChar(store.toString("\t"));
            writerStream.write(Constant.NEWLINE);
            cache.clear();
        }
        readerStream.close();
        writerStream.close();
    }
}
