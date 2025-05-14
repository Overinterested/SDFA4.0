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
import edu.sysu.pmglab.progressbar.ProgressBar;
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
 * @create 2024-10-10 03:14
 * @description
 */
public class LZBED2SDF {
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
        ProgressBar bar = new ProgressBar.Builder().setTextRenderer("load SVs").build();
        SVTypeSign type = null;
        SimpleSDSV simpleSDSV;
        int contigIndex = -1, pos = 0, end = 0, length;
        List<SimpleSDSV> sdsvList = new List<>();
        int count = 0;
        do {
            count = 0;
            simpleSDSV = new SimpleSDSV();
            Iterator<Bytes> iterator = cache.toBytes().split(Constant.TAB);
            while (iterator.hasNext()) {
                Bytes item = iterator.next();
                switch (count++) {
                    case 0:
                        contigIndex = contig.getContigIndexByName(item.toString());
                        break;
                    case 1:
                        pos = item.toInt();
                        break;
                    case 2:
                        end = item.toInt();

                    case 3:
                    case 4:
                        break;
                    case 5:
                        type = SVTypeSign.getByName(item);
                        break;
                }
            }
            length = end - pos;
            simpleSDSV.setLength(length).setCoordinate(new SVCoordinate(pos, end, contigIndex)).setType(type);
            if (containGty) {

            }
            sdsvList.add(simpleSDSV);
            contig.countContigByIndex(contigIndex);
            bar.step(1);
        } while (reader.readline(cache) != -1);
        bar.close();
        sdsvList.sort((Comparator.comparing(SimpleSDSV::getCoordinate)));
        IRecord record = manager.getRecord();
        // TODO
        CCFWriter writer = CCFWriter.setOutput(outputFilePath).addFields(new FieldGroupMetas()).instance();
        int index = 0;
        while (!sdsvList.isEmpty()) {
            SimpleSDSV simpleSDSV1 = sdsvList.popFirst();
            writer.write(manager.unsafeEncodeRecord(simpleSDSV1.toRecord(record, index++)));
        }
        List<CCFMetaItem> contigs = contig.save();
        for (CCFMetaItem item : contigs) {
            writer.addMeta(item);
        }
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        LZBED2SDF bed2SDF = new LZBED2SDF();
        bed2SDF.bedFilePath = new File("/Users/wenjiepeng/Desktop/tmp/all.merge.SV.bed");
        bed2SDF.outputFilePath = new File("/Users/wenjiepeng/Desktop/tmp/all.merge.SV.bed.ccf");
        bed2SDF.convert();
    }
}
