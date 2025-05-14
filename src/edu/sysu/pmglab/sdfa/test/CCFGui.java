package edu.sysu.pmglab.sdfa.test;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.reader.ReaderStream;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-02-11 09:52
 * @description
 */
public class CCFGui {
    public static void main(String[] args) throws IOException {
        CCFReader reader = new CCFReader("/Users/wenjiepeng/Desktop/SDFA3.0/nagf/data/normal/drop_null_gty_AND_precise_AND_DV_7/sdf/CN005_filt_centromere.vcf.gz.sdf");
        IRecord record = reader.read();
        ReaderStream readerStream = new ReaderStream(
                "/Users/wenjiepeng/Desktop/SDFA3.0/test/vcf2sdf/UKBB/ukb23353_c1_b123_v1.vcf.gz",
                ReaderStream.Option.GZIP
        );
        ByteStream cache = new ByteStream();
        int count = 0;
        while(readerStream.readline(cache)!=-1){
            boolean b = cache.toBytes().byteAt(0) == Constant.NUMBER_SIGN;
            if (!b){
                count++;
            }
            cache.clear();
        }
        System.out.println(count);
    }
}
