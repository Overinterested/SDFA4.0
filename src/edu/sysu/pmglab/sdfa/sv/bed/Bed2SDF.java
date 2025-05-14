package edu.sysu.pmglab.sdfa.sv.bed;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.sdfa.sv.SDSVConversionManager;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import edu.sysu.pmglab.sdfa.sv.sdsv.SimpleSDSV;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2024-09-29 22:53
 * @description
 */
public class Bed2SDF {
    int gtyStartIndex;
    boolean containGty = false;

    File bedFilePath;
    File outputFilePath;
    SVContig contig = SVContig.init();
    LinkedSet<String> individuals = new LinkedSet<>();
    SDSVConversionManager manager = new SDSVConversionManager();

    public static final String infoNameList = "BED::INFO";

    public void convert() throws IOException {
        ByteStream cache = new ByteStream();
        ReaderStream reader = new ReaderStream(bedFilePath.toString(), ReaderStream.Option.DEFAULT);
        while (reader.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            // drop annotation header
            if (line.byteAt(1) == Constant.NUMBER_SIGN) {
                cache.clear();
                continue;
            }
            // parse column header
            if (line.byteAt(0) == Constant.NUMBER_SIGN) {
                Bytes columns = cache.toBytes();
                List<Bytes> colList = new List<>();
                Iterator<Bytes> iterator = columns.split(Constant.TAB);
                while (iterator.hasNext()) colList.add(iterator.next().detach());

                if (containGty) {
                    for (int i = gtyStartIndex; i < colList.size(); i++) {
                        individuals.add(colList.fastGet(i).detach().toString());
                    }
                }
                cache.clear();
                continue;
            }
            break;
        }
        if (cache.length() == 0) {
            throw new UnsupportedEncodingException("No SV records in file");
        }
        SVTypeSign type;
        SimpleSDSV simpleSDSV;
        int contigIndex, pos, end, length;
        List<SimpleSDSV> sdsvList = new List<>();
        do {
            simpleSDSV = new SimpleSDSV();
            List<Bytes> items = new List<>();
            Iterator<Bytes> iterator = cache.toBytes().split(Constant.TAB);
            while (iterator.hasNext()) items.add(iterator.next().detach());

            contigIndex = contig.getContigIndexByName(items.fastGet(0).toString());
            pos = items.fastGet(1).toInt();
            end = items.fastGet(2).toInt();
            type = SVTypeSign.getByName(items.fastGet(3));
            length = items.fastGet(4).toInt();
            simpleSDSV.setLength(length).setCoordinate(new SVCoordinate(pos, end, contigIndex)).setType(type);
            if (containGty) {
                // TODO
            }
            sdsvList.add(simpleSDSV);
            contig.countContigByIndex(contigIndex);
        } while (reader.readline(cache) != -1);
        sdsvList.sort((Comparator.comparing(SimpleSDSV::getCoordinate)));
        IRecord record = manager.getRecord();
        // TODO
        CCFWriter writer = CCFWriter.setOutput(outputFilePath).addFields( new FieldGroupMetas()).instance();
        for (int i = 0; i < sdsvList.size(); i++) {
            writer.write(manager.unsafeEncodeRecord(sdsvList.fastGet(i).toRecord(record, i)));
        }
        List<CCFMetaItem> contigs = contig.save();
        for (CCFMetaItem item : contigs) {
            writer.addMeta(item);
        }
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        Bed2SDF bed2SDF = new Bed2SDF();
        bed2SDF.bedFilePath = new File("/Users/wenjiepeng/Desktop/SDFA3.0/nagf/cuteSV_nagf/bed.txt");
        bed2SDF.outputFilePath = new File("/Users/wenjiepeng/Desktop/SDFA3.0/nagf/cuteSV_nagf/bed.ccf");
        bed2SDF.convert();
    }
}
