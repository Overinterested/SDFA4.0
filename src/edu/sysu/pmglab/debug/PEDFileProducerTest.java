package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.ByteList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.gwas.PEDFile;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Wenjie Peng
 * @create 2025-05-20 08:26
 * @description
 */
public class PEDFileProducerTest {
    public static void main(String[] args) throws IOException {
        String names = "1075936\n" +
                "1941534\n" +
                "2190765\n" +
                "2211207\n" +
                "2247421\n" +
                "2450684\n" +
                "2782452\n" +
                "2845615\n" +
                "3473069\n" +
                "3621916\n" +
                "3799052\n" +
                "4079365\n" +
                "4139857\n" +
                "5535616\n" +
                "5878062";
        String[] nameList = names.split("\n");
        ByteList byteList = ByteList.wrap(new byte[nameList.length]);
        List<Bytes> ids = new List<>();
        for (int i = 0; i < nameList.length; i++) {
            ids.add(new Bytes(nameList[i]));
        }
        PEDFile.PEDEasyProducer.produce(ids, byteList, "/Users/wenjiepeng/Desktop/SDFA_4.0/UKB/test_sdf/sdf/test.ped");
    }
}
