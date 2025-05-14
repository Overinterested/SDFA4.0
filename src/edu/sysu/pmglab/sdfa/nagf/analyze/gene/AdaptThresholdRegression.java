package edu.sysu.pmglab.sdfa.nagf.analyze.gene;

import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.container.list.DoubleList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.easytools.r.ChngptInstance;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.gwas.PEDFile;
import gnu.trove.set.hash.TIntHashSet;
import org.rosuda.REngine.REngineException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2025-03-13 03:05
 * @description
 */
public class AdaptThresholdRegression {
    float lb = 0.1f;
    float ub = 0.9f;
    float maxZeroAF = 0.9f;
    String casePattern = null;
    int bootB = 10000, nOfMC = 50000;
    // lr or score
    String testStatistic = "lr";
    HashSet<Bytes> dropNames = new HashSet<>();
    TIntHashSet dropIndexes = new TIntHashSet();
    private static AdaptThresholdRegression instance = new AdaptThresholdRegression();
    private static final HashSet<String> VALID_TEST_STATISTICAL = new HashSet<>();
    private static final Bytes FILTERED_TEST = new Bytes("NA\tNA\tNA");
    private static final Bytes GENE_OUTPUT_HEADER = new Bytes("Gene\tIsCoding\t" +
            "P1\tT1\tM1\t" +
            "P2\tT2\tM2\t" +
            "P3\tT3\tM3\t" +
            "P4\tT4\tM4\t" +
            "P5\tT5\tM5\t" +
            "P6\tT6\tM6\t" +
            "P7\tT7\tM7\n"
    );
    private static final Bytes RNA_OUTPUT_HEADER = new Bytes("Gene\tRNA\tIsCoding\t" +
            "P1\tT1\tM1\t" +
            "P2\tT2\tM2\t" +
            "P3\tT3\tM3\t" +
            "P4\tT4\tM4\t" +
            "P5\tT5\tM5\t" +
            "P6\tT6\tM6\t" +
            "P7\tT7\tM7\n"
    );

    static {
        VALID_TEST_STATISTICAL.add("lr");
        VALID_TEST_STATISTICAL.add("score");
    }

    private AdaptThresholdRegression() {

    }

    public static AdaptThresholdRegression getInstance() {
        return instance;
    }

    public static void analyze(String inputFile, String outputFile, String pedFile, String level) throws IOException, InterruptedException, REngineException {
        int numericValueStartCol = -1;
        switch (level.toLowerCase()) {
            case "gene":
                numericValueStartCol = 2;
                break;
            case "rna":
                numericValueStartCol = 3;
                break;
            default:
                throw new UnsupportedEncodingException("Level must be chosen from `gene` or `rna`");
        }
        ByteStream readeCache = new ByteStream();
        ByteStream writeCache = new ByteStream();
        ReaderStream reader = new ReaderStream(inputFile, ReaderStream.Option.DEFAULT);
        WriterStream writer = new WriterStream(new File(outputFile), WriterStream.Option.DEFAULT);
        List<DoubleList> xList = new List<>();
        for (int i = 0; i < 7; i++) {
            xList.add(new DoubleList());
        }
        DoubleList y = new DoubleList();
        int col;
        int valueIndex;
        BytesSplitter tabSplitter = new BytesSplitter(Constant.TAB);
        BytesSplitter commaSplitter = new BytesSplitter(Constant.COMMA);
        Bytes header = null;
        while (reader.readline(readeCache) != -1) {
            Bytes line = readeCache.toBytes();
            if (line.byteAt(0) == Constant.NUMBER_SIGN) {
                if (line.byteAt(1) == Constant.NUMBER_SIGN) {
                    readeCache.clear();
                    continue;
                }
                header = line;
                break;
            }
        }
        if (instance.casePattern != null) {
            tabSplitter.init(header);
            int colCount = 0;
            while (tabSplitter.hasNext()){
                Bytes item = tabSplitter.next();
                if (colCount++<numericValueStartCol){
                    continue;
                }
                y.add(item.toString().matches(instance.casePattern)?1:0);
            }
        } else if (pedFile != null) {
            PEDFile pedInstance = PEDFile.load(pedFile);
            checkAndProduceY(header, numericValueStartCol, pedInstance, y);
        }else {
            throw new UnsupportedEncodingException("Case-control analyses can't be taken without casePattern or PED file.");
        }
        writer.write(numericValueStartCol == 2 ? GENE_OUTPUT_HEADER : RNA_OUTPUT_HEADER);
        int zeroCount;
        readeCache.clear();
        ProgressBar.Builder builder = new ProgressBar.Builder()
                .setTextRenderer("Calculation speed", "records");
        ProgressBar bar = builder.build();

        while (reader.readline(readeCache) != -1) {
            Bytes line = readeCache.toBytes();
            col = 0;
            tabSplitter.init(line);
            while (tabSplitter.hasNext()) {
                if (col < numericValueStartCol) {
                    Bytes next = tabSplitter.next();
                    writeCache.write(next.detach());
                    writeCache.write(Constant.TAB);
                    col++;
                    continue;
                }
                Bytes item = tabSplitter.next();
                if (instance.dropIndexes.contains(col)) {
                    col++;
                    continue;
                }
                // init x
                commaSplitter.init(item);
                valueIndex = 0;
                while (commaSplitter.hasNext()) {
                    double value = commaSplitter.next().toDouble();
                    xList.fastGet(valueIndex++).add(value);
                }
                col++;
            }
            boolean allDrop = true;
            int size = xList.fastGet(0).size();

            for (int i = 0; i < 7; i++) {
                DoubleList x = xList.fastGet(i);
                if (instance.maxZeroAF != -1) {
                    zeroCount = 0;
                    for (int j = 0; j < size; j++) {
                        if (x.fastGet(j) == 0) zeroCount++;
                    }
                    float zeroRate = zeroCount / (float) size;
                    if (zeroRate >= instance.maxZeroAF) {
                        writeCache.write(FILTERED_TEST);
                        if (i != 6) {
                            writeCache.write(Constant.TAB);
                        } else {
                            writeCache.write(Constant.NEWLINE);
                        }
                        x.clear();
                        continue;
                    }
                }
                allDrop = false;
                ChngptInstance.AdaptiveThresholdLogistic adaptiveThresholdLogistic = ChngptInstance.stepTestWithGLM(x.toArray(), y.toArray(), instance.lb, instance.ub, instance.testStatistic, instance.bootB, instance.nOfMC);
                writeCache.write(ASCIIUtility.toASCII(adaptiveThresholdLogistic.getPValue()));
                writeCache.write(Constant.TAB);
                writeCache.write(ASCIIUtility.toASCII(adaptiveThresholdLogistic.getThreshold()));
                writeCache.write(Constant.TAB);
                writeCache.write(ASCIIUtility.toASCII(adaptiveThresholdLogistic.getMethod(), Constant.CHAR_SET));
                if (i != 6) {
                    writeCache.write(Constant.TAB);
                } else {
                    writeCache.write(Constant.NEWLINE);
                }
                x.clear();
            }
            if (!allDrop) {
                writer.write(writeCache.toBytes());
            }
            writeCache.clear();
            readeCache.clear();
            bar.step(1);
        }
        bar.close();
        reader.close();
        writer.close();
        writeCache.clear();
        readeCache.clear();
    }

    /**
     * @param header
     * @param valueStartIndex
     * @param pedFile
     * @param y
     */
    private static void checkAndProduceY(Bytes header, int valueStartIndex, PEDFile pedFile, DoubleList y) {
        Iterator<Bytes> iterator = header.split(Constant.TAB);
        int col = 0;
        while (iterator.hasNext()) {
            Bytes item = iterator.next();
            if (col < valueStartIndex) {
                col++;
                continue;
            }
            PEDFile.PEDItem pedItem = pedFile.valueOf(col - valueStartIndex);
            if (!item.equals(pedItem.getIid())) {
                throw new UnsupportedOperationException("Sample name doesn't match.");
            }
            if (!instance.dropNames.isEmpty()) {
                Bytes iid = pedItem.getIid();
                boolean drop = false;
                for (Bytes dropName : instance.dropNames) {
                    boolean contains = iid.toString().replace("-", ".").contains(dropName.toString());
                    if (contains) {
                        instance.dropIndexes.add(col++);
                        drop = true;
                        break;
                    }
                }
                if (drop) {
                    continue;
                }
            }
            y.add(pedItem.getPhenotype().toDouble() == 1 ? 0 : 1);
            col++;
        }
    }


    public AdaptThresholdRegression setLb(float lb) {
        this.lb = lb;
        return this;
    }

    public AdaptThresholdRegression setUb(float ub) {
        this.ub = ub;
        return this;
    }

    public AdaptThresholdRegression setBootB(int bootB) {
        this.bootB = bootB;
        return this;
    }

    public AdaptThresholdRegression setNOfMC(int nOfMC) {
        this.nOfMC = nOfMC;
        return this;
    }

    public AdaptThresholdRegression setTestStatistic(String testStatistic) {
        if (VALID_TEST_STATISTICAL.contains(testStatistic)) {
            this.testStatistic = testStatistic;
        } else {
            LogBackOptions.getRootLogger().error("The threshold test statistical method can only be chosen from `lr` or `score`.");
        }
        return this;
    }

    public void setZeroAFFilter(float zeroAFFilter) {
        this.maxZeroAF = zeroAFFilter;
    }

    public AdaptThresholdRegression setDropSampleFile(String dropSampleFile) throws IOException {
        ByteStream cache = new ByteStream();
        ReaderStream readerStream = LiveFile.of(dropSampleFile).openAsText();
        while (readerStream.readline(cache) != -1) {
            dropNames.add(cache.toBytes().detach());
            cache.clear();
        }
        return this;
    }

    public AdaptThresholdRegression setCasePattern(String casePattern) {
        this.casePattern = casePattern;
        return this;
    }
}
