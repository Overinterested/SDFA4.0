package edu.sysu.pmglab.sdfa.merge.output;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVManager;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFHeaderManager;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInstance;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * @author Wenjie Peng
 * @create 2024-09-10 21:34
 * @description
 */
public class GlobalMergeResultWriter {
    File outputFilePath;
    final File outputDir;
    WriterStream writerStream;

    private static GlobalMergeResultWriter instance;

    private GlobalMergeResultWriter(File outputDir) {
        this.outputDir = outputDir;
    }

    public static GlobalMergeResultWriter init(File outputDir) throws IOException {
        if (instance == null) {
            instance = new GlobalMergeResultWriter(outputDir);
            File outputFile = FileUtils.getSubFile(outputDir, UUID.randomUUID().toString());
            while (!outputDir.exists()) {
                outputFile = FileUtils.getSubFile(outputDir, UUID.randomUUID().toString());
            }
            instance.outputFilePath = outputFile;
            WriterStream fs = new WriterStream(outputFile, WriterStream.Option.DEFAULT);
            initHeader(fs);
            instance.writerStream = fs;
        }
        return instance;
    }

    private static void initHeader(WriterStream fs) throws IOException {
        SDFReader reader = SDSVManager.getInstance().getByIndex(0).getReader();
        CCFReader ccfReader = reader.getReader();
        List<CCFMetaItem> ccfMetaItems = ccfReader.getTable().getMeta().get(VCFHeaderManager.name());
        fs.write(ASCIIUtility.toASCII("##fileformat=VCFv4.1\n", Constant.CHAR_SET));
        fs.write(ASCIIUtility.toASCII("##source=sdfa\n", Constant.CHAR_SET));
        fs.write(ASCIIUtility.toASCII("##fileDate=" + new Date() + "\n", Constant.CHAR_SET));
        if (ccfMetaItems == null || ccfMetaItems.isEmpty()) {
            fs.write(ASCIIUtility.toASCII("##contig=<ID=chr1,length=248956422>\n" +
                    "##contig=<ID=chr2,length=242193529>\n" +
                    "##contig=<ID=chr3,length=198295559>\n" +
                    "##contig=<ID=chr4,length=190214555>\n" +
                    "##contig=<ID=chr5,length=181538259>\n" +
                    "##contig=<ID=chr6,length=170805979>\n" +
                    "##contig=<ID=chr7,length=159345973>\n" +
                    "##contig=<ID=chr8,length=145138636>\n" +
                    "##contig=<ID=chr9,length=138394717>\n" +
                    "##contig=<ID=chr10,length=133797422>\n" +
                    "##contig=<ID=chr11,length=135086622>\n" +
                    "##contig=<ID=chr12,length=133275309>\n" +
                    "##contig=<ID=chr13,length=114364328>\n" +
                    "##contig=<ID=chr14,length=107043718>\n" +
                    "##contig=<ID=chr15,length=101991189>\n" +
                    "##contig=<ID=chr16,length=90338345>\n" +
                    "##contig=<ID=chr17,length=83257441>\n" +
                    "##contig=<ID=chr18,length=80373285>\n" +
                    "##contig=<ID=chr19,length=58617616>\n" +
                    "##contig=<ID=chr20,length=64444167>\n" +
                    "##contig=<ID=chr21,length=46709983>\n" +
                    "##contig=<ID=chr22,length=50818468>\n" +
                    "##contig=<ID=chrX,length=156040895>\n" +
                    "##contig=<ID=chrY,length=57227415>\n" +
                    "##contig=<ID=chrM,length=16569>\n" +
                    "##contig=<ID=chr1_KI270706v1_random,length=175055>\n" +
                    "##contig=<ID=chr1_KI270707v1_random,length=32032>\n" +
                    "##contig=<ID=chr1_KI270708v1_random,length=127682>\n" +
                    "##contig=<ID=chr1_KI270709v1_random,length=66860>\n" +
                    "##contig=<ID=chr1_KI270710v1_random,length=40176>\n" +
                    "##contig=<ID=chr1_KI270711v1_random,length=42210>\n" +
                    "##contig=<ID=chr1_KI270712v1_random,length=176043>\n" +
                    "##contig=<ID=chr1_KI270713v1_random,length=40745>\n" +
                    "##contig=<ID=chr1_KI270714v1_random,length=41717>\n" +
                    "##contig=<ID=chr2_KI270715v1_random,length=161471>\n" +
                    "##contig=<ID=chr2_KI270716v1_random,length=153799>\n" +
                    "##contig=<ID=chr3_GL000221v1_random,length=155397>\n" +
                    "##contig=<ID=chr4_GL000008v2_random,length=209709>\n" +
                    "##contig=<ID=chr5_GL000208v1_random,length=92689>\n" +
                    "##contig=<ID=chr9_KI270717v1_random,length=40062>\n" +
                    "##contig=<ID=chr9_KI270718v1_random,length=38054>\n" +
                    "##contig=<ID=chr9_KI270719v1_random,length=176845>\n" +
                    "##contig=<ID=chr9_KI270720v1_random,length=39050>\n" +
                    "##contig=<ID=chr11_KI270721v1_random,length=100316>\n" +
                    "##contig=<ID=chr14_GL000009v2_random,length=201709>\n" +
                    "##contig=<ID=chr14_GL000225v1_random,length=211173>\n" +
                    "##contig=<ID=chr14_KI270722v1_random,length=194050>\n" +
                    "##contig=<ID=chr14_GL000194v1_random,length=191469>\n" +
                    "##contig=<ID=chr14_KI270723v1_random,length=38115>\n" +
                    "##contig=<ID=chr14_KI270724v1_random,length=39555>\n" +
                    "##contig=<ID=chr14_KI270725v1_random,length=172810>\n" +
                    "##contig=<ID=chr14_KI270726v1_random,length=43739>\n" +
                    "##contig=<ID=chr15_KI270727v1_random,length=448248>\n" +
                    "##contig=<ID=chr16_KI270728v1_random,length=1872759>\n" +
                    "##contig=<ID=chr17_GL000205v2_random,length=185591>\n" +
                    "##contig=<ID=chr17_KI270729v1_random,length=280839>\n" +
                    "##contig=<ID=chr17_KI270730v1_random,length=112551>\n" +
                    "##contig=<ID=chr22_KI270731v1_random,length=150754>\n" +
                    "##contig=<ID=chr22_KI270732v1_random,length=41543>\n" +
                    "##contig=<ID=chr22_KI270733v1_random,length=179772>\n" +
                    "##contig=<ID=chr22_KI270734v1_random,length=165050>\n" +
                    "##contig=<ID=chr22_KI270735v1_random,length=42811>\n" +
                    "##contig=<ID=chr22_KI270736v1_random,length=181920>\n" +
                    "##contig=<ID=chr22_KI270737v1_random,length=103838>\n" +
                    "##contig=<ID=chr22_KI270738v1_random,length=99375>\n" +
                    "##contig=<ID=chr22_KI270739v1_random,length=73985>\n" +
                    "##contig=<ID=chrY_KI270740v1_random,length=37240>\n" +
                    "##contig=<ID=chrUn_KI270302v1,length=2274>\n" +
                    "##contig=<ID=chrUn_KI270304v1,length=2165>\n" +
                    "##contig=<ID=chrUn_KI270303v1,length=1942>\n" +
                    "##contig=<ID=chrUn_KI270305v1,length=1472>\n" +
                    "##contig=<ID=chrUn_KI270322v1,length=21476>\n" +
                    "##contig=<ID=chrUn_KI270320v1,length=4416>\n" +
                    "##contig=<ID=chrUn_KI270310v1,length=1201>\n" +
                    "##contig=<ID=chrUn_KI270316v1,length=1444>\n" +
                    "##contig=<ID=chrUn_KI270315v1,length=2276>\n" +
                    "##contig=<ID=chrUn_KI270312v1,length=998>\n" +
                    "##contig=<ID=chrUn_KI270311v1,length=12399>\n" +
                    "##contig=<ID=chrUn_KI270317v1,length=37690>\n" +
                    "##contig=<ID=chrUn_KI270412v1,length=1179>\n" +
                    "##contig=<ID=chrUn_KI270411v1,length=2646>\n" +
                    "##contig=<ID=chrUn_KI270414v1,length=2489>\n" +
                    "##contig=<ID=chrUn_KI270419v1,length=1029>\n" +
                    "##contig=<ID=chrUn_KI270418v1,length=2145>\n" +
                    "##contig=<ID=chrUn_KI270420v1,length=2321>\n" +
                    "##contig=<ID=chrUn_KI270424v1,length=2140>\n" +
                    "##contig=<ID=chrUn_KI270417v1,length=2043>\n" +
                    "##contig=<ID=chrUn_KI270422v1,length=1445>\n" +
                    "##contig=<ID=chrUn_KI270423v1,length=981>\n" +
                    "##contig=<ID=chrUn_KI270425v1,length=1884>\n" +
                    "##contig=<ID=chrUn_KI270429v1,length=1361>\n" +
                    "##contig=<ID=chrUn_KI270442v1,length=392061>\n" +
                    "##contig=<ID=chrUn_KI270466v1,length=1233>\n" +
                    "##contig=<ID=chrUn_KI270465v1,length=1774>\n" +
                    "##contig=<ID=chrUn_KI270467v1,length=3920>\n" +
                    "##contig=<ID=chrUn_KI270435v1,length=92983>\n" +
                    "##contig=<ID=chrUn_KI270438v1,length=112505>\n" +
                    "##contig=<ID=chrUn_KI270468v1,length=4055>\n" +
                    "##contig=<ID=chrUn_KI270510v1,length=2415>\n" +
                    "##contig=<ID=chrUn_KI270509v1,length=2318>\n" +
                    "##contig=<ID=chrUn_KI270518v1,length=2186>\n" +
                    "##contig=<ID=chrUn_KI270508v1,length=1951>\n" +
                    "##contig=<ID=chrUn_KI270516v1,length=1300>\n" +
                    "##contig=<ID=chrUn_KI270512v1,length=22689>\n" +
                    "##contig=<ID=chrUn_KI270519v1,length=138126>\n" +
                    "##contig=<ID=chrUn_KI270522v1,length=5674>\n" +
                    "##contig=<ID=chrUn_KI270511v1,length=8127>\n" +
                    "##contig=<ID=chrUn_KI270515v1,length=6361>\n" +
                    "##contig=<ID=chrUn_KI270507v1,length=5353>\n" +
                    "##contig=<ID=chrUn_KI270517v1,length=3253>\n" +
                    "##contig=<ID=chrUn_KI270529v1,length=1899>\n" +
                    "##contig=<ID=chrUn_KI270528v1,length=2983>\n" +
                    "##contig=<ID=chrUn_KI270530v1,length=2168>\n" +
                    "##contig=<ID=chrUn_KI270539v1,length=993>\n" +
                    "##contig=<ID=chrUn_KI270538v1,length=91309>\n" +
                    "##contig=<ID=chrUn_KI270544v1,length=1202>\n" +
                    "##contig=<ID=chrUn_KI270548v1,length=1599>\n" +
                    "##contig=<ID=chrUn_KI270583v1,length=1400>\n" +
                    "##contig=<ID=chrUn_KI270587v1,length=2969>\n" +
                    "##contig=<ID=chrUn_KI270580v1,length=1553>\n" +
                    "##contig=<ID=chrUn_KI270581v1,length=7046>\n" +
                    "##contig=<ID=chrUn_KI270579v1,length=31033>\n" +
                    "##contig=<ID=chrUn_KI270589v1,length=44474>\n" +
                    "##contig=<ID=chrUn_KI270590v1,length=4685>\n" +
                    "##contig=<ID=chrUn_KI270584v1,length=4513>\n" +
                    "##contig=<ID=chrUn_KI270582v1,length=6504>\n" +
                    "##contig=<ID=chrUn_KI270588v1,length=6158>\n" +
                    "##contig=<ID=chrUn_KI270593v1,length=3041>\n" +
                    "##contig=<ID=chrUn_KI270591v1,length=5796>\n" +
                    "##contig=<ID=chrUn_KI270330v1,length=1652>\n" +
                    "##contig=<ID=chrUn_KI270329v1,length=1040>\n" +
                    "##contig=<ID=chrUn_KI270334v1,length=1368>\n" +
                    "##contig=<ID=chrUn_KI270333v1,length=2699>\n" +
                    "##contig=<ID=chrUn_KI270335v1,length=1048>\n" +
                    "##contig=<ID=chrUn_KI270338v1,length=1428>\n" +
                    "##contig=<ID=chrUn_KI270340v1,length=1428>\n" +
                    "##contig=<ID=chrUn_KI270336v1,length=1026>\n" +
                    "##contig=<ID=chrUn_KI270337v1,length=1121>\n" +
                    "##contig=<ID=chrUn_KI270363v1,length=1803>\n" +
                    "##contig=<ID=chrUn_KI270364v1,length=2855>\n" +
                    "##contig=<ID=chrUn_KI270362v1,length=3530>\n" +
                    "##contig=<ID=chrUn_KI270366v1,length=8320>\n" +
                    "##contig=<ID=chrUn_KI270378v1,length=1048>\n" +
                    "##contig=<ID=chrUn_KI270379v1,length=1045>\n" +
                    "##contig=<ID=chrUn_KI270389v1,length=1298>\n" +
                    "##contig=<ID=chrUn_KI270390v1,length=2387>\n" +
                    "##contig=<ID=chrUn_KI270387v1,length=1537>\n" +
                    "##contig=<ID=chrUn_KI270395v1,length=1143>\n" +
                    "##contig=<ID=chrUn_KI270396v1,length=1880>\n" +
                    "##contig=<ID=chrUn_KI270388v1,length=1216>\n" +
                    "##contig=<ID=chrUn_KI270394v1,length=970>\n" +
                    "##contig=<ID=chrUn_KI270386v1,length=1788>\n" +
                    "##contig=<ID=chrUn_KI270391v1,length=1484>\n" +
                    "##contig=<ID=chrUn_KI270383v1,length=1750>\n" +
                    "##contig=<ID=chrUn_KI270393v1,length=1308>\n" +
                    "##contig=<ID=chrUn_KI270384v1,length=1658>\n" +
                    "##contig=<ID=chrUn_KI270392v1,length=971>\n" +
                    "##contig=<ID=chrUn_KI270381v1,length=1930>\n" +
                    "##contig=<ID=chrUn_KI270385v1,length=990>\n" +
                    "##contig=<ID=chrUn_KI270382v1,length=4215>\n" +
                    "##contig=<ID=chrUn_KI270376v1,length=1136>\n" +
                    "##contig=<ID=chrUn_KI270374v1,length=2656>\n" +
                    "##contig=<ID=chrUn_KI270372v1,length=1650>\n" +
                    "##contig=<ID=chrUn_KI270373v1,length=1451>\n" +
                    "##contig=<ID=chrUn_KI270375v1,length=2378>\n" +
                    "##contig=<ID=chrUn_KI270371v1,length=2805>\n" +
                    "##contig=<ID=chrUn_KI270448v1,length=7992>\n" +
                    "##contig=<ID=chrUn_KI270521v1,length=7642>\n" +
                    "##contig=<ID=chrUn_GL000195v1,length=182896>\n" +
                    "##contig=<ID=chrUn_GL000219v1,length=179198>\n" +
                    "##contig=<ID=chrUn_GL000220v1,length=161802>\n" +
                    "##contig=<ID=chrUn_GL000224v1,length=179693>\n" +
                    "##contig=<ID=chrUn_KI270741v1,length=157432>\n" +
                    "##contig=<ID=chrUn_GL000226v1,length=15008>\n" +
                    "##contig=<ID=chrUn_GL000213v1,length=164239>\n" +
                    "##contig=<ID=chrUn_KI270743v1,length=210658>\n" +
                    "##contig=<ID=chrUn_KI270744v1,length=168472>\n" +
                    "##contig=<ID=chrUn_KI270745v1,length=41891>\n" +
                    "##contig=<ID=chrUn_KI270746v1,length=66486>\n" +
                    "##contig=<ID=chrUn_KI270747v1,length=198735>\n" +
                    "##contig=<ID=chrUn_KI270748v1,length=93321>\n" +
                    "##contig=<ID=chrUn_KI270749v1,length=158759>\n" +
                    "##contig=<ID=chrUn_KI270750v1,length=148850>\n" +
                    "##contig=<ID=chrUn_KI270751v1,length=150742>\n" +
                    "##contig=<ID=chrUn_KI270752v1,length=27745>\n" +
                    "##contig=<ID=chrUn_KI270753v1,length=62944>\n" +
                    "##contig=<ID=chrUn_KI270754v1,length=40191>\n" +
                    "##contig=<ID=chrUn_KI270755v1,length=36723>\n" +
                    "##contig=<ID=chrUn_KI270756v1,length=79590>\n" +
                    "##contig=<ID=chrUn_KI270757v1,length=71251>\n" +
                    "##contig=<ID=chrUn_GL000214v1,length=137718>\n" +
                    "##contig=<ID=chrUn_KI270742v1,length=186739>\n" +
                    "##contig=<ID=chrUn_GL000216v2,length=176608>\n" +
                    "##contig=<ID=chrUn_GL000218v1,length=161147>\n" +
                    "##contig=<ID=chrEBV,length=171823>", Constant.CHAR_SET));
        } else {
            CCFMetaItem header = ccfMetaItems.fastGet(0);
            String[] headerLines = header.getValue();
            for (String headerLine : headerLines) {
                if (headerLine.startsWith("##fileformat") ||
                        headerLine.startsWith("##source") ||
                        headerLine.startsWith("##fileDate=")) {
                    continue;
                }
                fs.write(ASCIIUtility.toASCII(headerLine, Constant.CHAR_SET));
                fs.write(Constant.NEWLINE);
            }
        }
        fs.write(ASCIIUtility.toASCII("##INFO=<ID=CIEND,Number=2,Type=Integer,Description=\"PE confidence interval around SVLEN\"\n", Constant.CHAR_SET));
        fs.write(ASCIIUtility.toASCII("##INFO=<ID=SUPP_VEC,Number=1,Type=String,Description=\"Vector of supporting samples.\"\n", Constant.CHAR_SET));
        fs.write(ASCIIUtility.toASCII("##INFO=<ID=AVG_POS,Number=1,Type=Float,Description=\"Average of SV POS.\"\n", Constant.CHAR_SET));
        fs.write(ASCIIUtility.toASCII("##INFO=<ID=AVG_END,Number=1,Type=Float,Description=\"Average of SV END.\"\n", Constant.CHAR_SET));
        fs.write(ASCIIUtility.toASCII("##INFO=<ID=AVG_LEN,Number=1,Type=Float,Description=\"Average of SV LEN.\"\n", Constant.CHAR_SET));
        fs.write(ASCIIUtility.toASCII("##INFO=<ID=AF,Number=1,Type=Float,Description=\"Allele Frequency of SV\"\n", Constant.CHAR_SET));
        fs.write(ASCIIUtility.toASCII("##INFO=<ID=STDEV_POS,Number=1,Type=Float,Description=\"Average of SV POS.\"\n", Constant.CHAR_SET));
        fs.write(ASCIIUtility.toASCII("##INFO=<ID=STDEV_END,Number=1,Type=Float,Description=\"Average of SV END.\"\n", Constant.CHAR_SET));
        fs.write(ASCIIUtility.toASCII("##INFO=<ID=STDEV_LEN,Number=1,Type=Float,Description=\"Average of SV LEN.\"\n", Constant.CHAR_SET));
        fs.write(VCFInstance.HEADER_COLUMN);
        List<SDFReader> sdfReaders = SDSVManager.getInstance().getSDFReaders();
        int size = sdfReaders.size();
        for (int i = 0; i < size; i++) {
            LinkedSet<String> individuals = sdfReaders.fastGet(i).getIndividuals();
            for (int j = 0; j < individuals.size(); j++) {
                fs.write(ASCIIUtility.toASCII(individuals.valueOf(j),Constant.CHAR_SET));
                if (i == size - 1 && j == individuals.size() - 1) {
                    fs.write(Constant.NEWLINE);
                } else {
                    fs.write(Constant.TAB);
                }
            }
        }
    }

    public synchronized void safeWrite(Bytes mergedRes) throws IOException {
        writerStream.write(mergedRes);
        if (mergedRes.length() != 0) {
            writerStream.write(Constant.NEWLINE);
        }
    }

    public void unsafeWrite(Bytes mergedRes) throws IOException {
        writerStream.write(mergedRes);
        if (mergedRes.length() != 0) {
            writerStream.write(Constant.NEWLINE);
        }
    }

    public boolean closeAndRename() throws IOException {
        writerStream.close();
        File subFile = FileUtils.getSubFile(outputDir, "merged.vcf");
        boolean exists = subFile.exists();
        if (!exists) {
            this.outputFilePath.renameTo(subFile);
            this.outputFilePath = subFile;
            return true;
        }
        return false;
    }

    public static GlobalMergeResultWriter getInstance() {
        return instance;
    }

    public File getOutputFilePath() {
        return outputFilePath;
    }
}
