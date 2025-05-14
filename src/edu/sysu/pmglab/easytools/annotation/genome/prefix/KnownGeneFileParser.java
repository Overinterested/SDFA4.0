package edu.sysu.pmglab.easytools.annotation.genome.prefix;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.gtb.genome.coordinate.Chromosome;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.io.writer.WriterStream;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Wenjie Peng
 * @create 2024-11-07 21:23
 * @description
 */
public class KnownGeneFileParser {
    File outputPath;
    File knownGeneFile;
    /**
     * this file comes from the UCSC biomart:
     * 1. Gene stable ID
     * 2. Gene stable ID version
     * 3. Transcript stable ID
     * 4. Transcript stable ID version
     * 5. HGNC symbol
     * 6. HGNC ID
     */
    File extraGeneFile;
    HashSet<Chromosome> storedContigName;
    static BytesSplitter tabSplitter = new BytesSplitter(Constant.TAB);
    static BytesSplitter semicolonSplitter = new BytesSplitter(Constant.SEMICOLON);
    static BytesSplitter colonSplitter = new BytesSplitter(Constant.COLON);

    public void submit() throws IOException {
        File tmp = outputPath;
        ByteStream cache = new ByteStream();
        HashMap<Bytes, KnownGeneTransfer> transcriptMap = new HashMap<>();

        //region 1. collect all transcript names
        ReaderStream readerStream = LiveFile.of(extraGeneFile).openAsText();
        while (readerStream.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            if (line.byteAt(0) != Constant.NUMBER_SIGN) {
                KnownGeneTransfer.register(cache.toBytes(), transcriptMap);
            }
            cache.clear();
        }
        int dropSize = 0;
        readerStream.close();
        //endregion
        cache.clear();
        ByteStream writerCache = new ByteStream();
        readerStream = LiveFile.of(knownGeneFile).openAsText();
        Chromosome chromosome;
        KnownGeneTransfer knownGeneTransfer = null;
        WriterStream writerStream = new WriterStream(tmp, WriterStream.Option.DEFAULT);
        while (readerStream.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            if (line.byteAt(0) != Constant.NUMBER_SIGN) {
                tabSplitter.init(line);
                int cloCount = 0;
                boolean invalid = false;
                while (tabSplitter.hasNext()) {
                    Bytes item = tabSplitter.next();
                    switch (cloCount++) {
                        case 0:
                            knownGeneTransfer = transcriptMap.get(item);
                            invalid = knownGeneTransfer == null;
                            if (!invalid) {
                                writerCache.write(ASCIIUtility.toASCII(knownGeneTransfer.idOfHGNC));
                                writerCache.write(Constant.TAB);
                                writerCache.write(item.detach());
                                writerCache.write(Constant.TAB);
                            }
                            break;
                        case 1:
                            chromosome = Chromosome.get(item.toString());
                            invalid = !storedContigName.contains(chromosome);
                            if (!invalid) {
                                writerCache.write(item.detach());
                                writerCache.write(Constant.TAB);
                            }
                            break;
                        default:
                            writerCache.write(item.detach());
                            writerCache.write(Constant.TAB);
                            break;
                    }
                    if (invalid) {
                        break;
                    }
                }
                if (invalid || knownGeneTransfer == null) {
                    writerCache.clear();
                    cache.clear();
                    continue;
                }
                writerCache.write(Constant.ZERO);
                writerCache.write(Constant.TAB);
                writerCache.write(knownGeneTransfer.getGeneName());
                writerCache.write(Constant.TAB);
                writerCache.write(RefSeqGTFParser.UNKNOWN_FLAG);
                writerCache.write(Constant.TAB);
                writerCache.write(RefSeqGTFParser.UNKNOWN_FLAG);
                writerCache.write(Constant.TAB);
                writerCache.write(Constant.PERIOD);
                writerCache.write(Constant.COMMA);
                writerCache.write(Constant.TAB);
                writerCache.write(Constant.NEWLINE);
                writerStream.write(writerCache.toBytes());
            }
            cache.clear();
            writerCache.clear();
        }
        readerStream.close();
        writerStream.close();
    }

    public KnownGeneFileParser setOutputPath(Object outputPath) {
        this.outputPath = new File(outputPath.toString());
        return this;
    }

    public KnownGeneFileParser setKnownGeneFile(Object knownGeneFile) {
        this.knownGeneFile = new File(knownGeneFile.toString());
        return this;
    }

    public KnownGeneFileParser setStoredContigName(HashSet<Chromosome> storedContigName) {
        this.storedContigName = storedContigName;
        return this;
    }

    private static class KnownGeneTransfer {
        int idOfHGNC = -1;
        Bytes geneNameOfHGNC;
        Bytes geneNameOfUCSC;

        private KnownGeneTransfer() {

        }

        public static void register(Bytes line, HashMap<Bytes, KnownGeneTransfer> transcriptMap) {
            KnownGeneTransfer knownGeneTransfer = new KnownGeneTransfer();
            tabSplitter.init(line);
            Bytes nameOfRNA = null, versionOfRNA = null;
            int colCount = 0;
            boolean invalid;
            while (tabSplitter.hasNext()) {
                Bytes item = tabSplitter.next();
                switch (colCount++) {
                    case 0:
                        knownGeneTransfer.geneNameOfUCSC = item.detach();
                        break;
                    case 2:
                        nameOfRNA = item.detach();
                        break;
                    case 3:
                        versionOfRNA = item.detach();
                        break;
                    case 4:
                        if (item.length() != 0) {
                            knownGeneTransfer.geneNameOfHGNC = item.detach();
                            Bytes last = null;
                            while (tabSplitter.hasNext()) last = tabSplitter.next();
                            if (last != null) {
                                colonSplitter.init(last);
                                colonSplitter.next();
                                knownGeneTransfer.idOfHGNC = colonSplitter.next().detach().toInt();
                            }
                        }
                        break;

                }
            }
            if (nameOfRNA != null && versionOfRNA != null) {
                transcriptMap.put(nameOfRNA, knownGeneTransfer);
                transcriptMap.put(versionOfRNA, knownGeneTransfer);
            }
        }

        public Bytes getGeneName() {
            return geneNameOfHGNC == null ? geneNameOfUCSC : new Bytes(geneNameOfUCSC.toString() + ";" + geneNameOfHGNC.toString());
        }

        public int getIdOfHGNC() {
            return idOfHGNC;
        }
    }


    public File getKnownGeneFile() {
        return knownGeneFile;
    }

    public File getExtraGeneFile() {
        return extraGeneFile;
    }

    public KnownGeneFileParser setExtraGeneFile(Object extraGeneFile) {
        this.extraGeneFile = new File(extraGeneFile.toString());
        return this;
    }

    public static void main(String[] args) throws IOException {
        Chromosome.get("chrMT").addAlias("NC_012920.1", "MT", "chrM");
        HashSet<Chromosome> chromosomes = new HashSet<>(Chromosome.values());
        new KnownGeneFileParser().setExtraGeneFile("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/knownGene/mart_export-3.txt")
                .setKnownGeneFile("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/knownGene/knownGene.txt")
                .setOutputPath("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/knownGene")
                .setStoredContigName(chromosomes)
                .submit();
    }
}
