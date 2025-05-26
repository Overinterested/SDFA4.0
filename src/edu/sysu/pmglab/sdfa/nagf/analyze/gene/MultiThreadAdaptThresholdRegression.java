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
import edu.sysu.pmglab.executor.*;
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
 * @create 2025-05-23 08:54
 * @description
 */
public class MultiThreadAdaptThresholdRegression {
    int thread = 4;
    float lb = 0.1f;
    float ub = 0.9f;
    float maxZeroAF = 0.9f;
    String casePattern = null;
    int bootB = 10000, nOfMC = 50000;
    // lr or score
    String testStatistic = "lr";
    HashSet<Bytes> dropNames = new HashSet<>();
    TIntHashSet dropIndexes = new TIntHashSet();
    private static MultiThreadAdaptThresholdRegression instance = new MultiThreadAdaptThresholdRegression();
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

    private MultiThreadAdaptThresholdRegression() {

    }

    public static MultiThreadAdaptThresholdRegression getInstance() {
        return instance;
    }

    public static void analyze(String inputFile, String outputFile, String pedFile, String level) throws IOException, InterruptedException, REngineException {
        int numericValueStartCol;
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
        ReaderStream reader = new ReaderStream(inputFile, ReaderStream.Option.DEFAULT);
        WriterStream writer = new WriterStream(new File(outputFile), WriterStream.Option.DEFAULT);
        List<DoubleList> xList = new List<>();
        for (int i = 0; i < 7; i++) {
            xList.add(new DoubleList());
        }
        DoubleList y = new DoubleList();
        BytesSplitter tabSplitter = new BytesSplitter(Constant.TAB);
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
            while (tabSplitter.hasNext()) {
                Bytes item = tabSplitter.next();
                if (colCount++ < numericValueStartCol) {
                    continue;
                }
                y.add(item.toString().matches(instance.casePattern) ? 1 : 0);
            }
        } else if (pedFile != null) {
            PEDFile pedInstance = PEDFile.load(pedFile);
            checkAndProduceY(header, numericValueStartCol, pedInstance, y);
        } else {
            throw new UnsupportedEncodingException("Case-control analyses can't be taken without casePattern or PED file.");
        }
        writer.write(numericValueStartCol == 2 ? GENE_OUTPUT_HEADER : RNA_OUTPUT_HEADER);
        readeCache.clear();

        int thread = instance.thread;
        Workflow workflow = new Workflow(thread);
        workflow.addTask((status, context) -> {
            ProgressBar.Builder builder = new ProgressBar.Builder()
                    .setTextRenderer("Calculation speed", "records");
            ProgressBar bar = builder.build();
            context.put("bar", bar);
        });
        // init x and y for all threads
        List<List<DoubleList>> xListOfAllThreads = new List<>();
        for (int i = 0; i < thread; i++) {
            List<DoubleList> item = new List<>();
            xListOfAllThreads.add(item);
            for (int j = 0; j < 7; j++) {
                item.add(new DoubleList());
            }
        }
        // init cache
        List<ByteStream> readerCacheList = new List<>();
        List<ByteStream> writerCacheList = new List<>();
        for (int i = 0; i < thread; i++) {
            readerCacheList.add(new ByteStream());
            writerCacheList.add(new ByteStream());
        }
        for (int i = 0; i < thread; i++) {
            int finalI = i;
            workflow.addTask((status, context) -> {
                calcByLine(
                        reader, writer, xListOfAllThreads.fastGet(finalI),
                        y, numericValueStartCol, readerCacheList.fastGet(finalI),
                        writerCacheList.fastGet(finalI), context
                );
            });
        }
        workflow.execute();
        workflow.clearTasks();
        workflow.addTask((status, context) -> {
            ProgressBar bar = (ProgressBar) context.get("bar");
            if (bar != null) {
                bar.close();
            }
        });
        workflow.execute();
        workflow.clearTasks();
        reader.close();
        writer.close();
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


    public MultiThreadAdaptThresholdRegression setLb(float lb) {
        this.lb = lb;
        return this;
    }

    public MultiThreadAdaptThresholdRegression setUb(float ub) {
        this.ub = ub;
        return this;
    }

    public MultiThreadAdaptThresholdRegression setBootB(int bootB) {
        this.bootB = bootB;
        return this;
    }

    public MultiThreadAdaptThresholdRegression setNOfMC(int nOfMC) {
        this.nOfMC = nOfMC;
        return this;
    }

    public MultiThreadAdaptThresholdRegression setTestStatistic(String testStatistic) {
        if (VALID_TEST_STATISTICAL.contains(testStatistic)) {
            this.testStatistic = testStatistic;
        } else {
            LogBackOptions.getRootLogger().error("The threshold test statistical method can only be chosen from `lr` or `score`.");
        }
        return this;
    }

    public MultiThreadAdaptThresholdRegression setZeroAFFilter(float zeroAFFilter) {
        this.maxZeroAF = zeroAFFilter;
        return this;
    }

    public MultiThreadAdaptThresholdRegression setDropSampleFile(String dropSampleFile) throws IOException {
        ByteStream cache = new ByteStream();
        ReaderStream readerStream = LiveFile.of(dropSampleFile).openAsText();
        while (readerStream.readline(cache) != -1) {
            dropNames.add(cache.toBytes().detach());
            cache.clear();
        }
        return this;
    }

    public MultiThreadAdaptThresholdRegression setCasePattern(String casePattern) {
        this.casePattern = casePattern;
        return this;
    }

    private static void calcByLine(ReaderStream readerStream, WriterStream writerStream,
                                   List<DoubleList> x, DoubleList y, int numericValueStartCol,
                                   ByteStream readCache, ByteStream writeCache, Context context) throws IOException, REngineException, InterruptedException {
        Bytes line;
        while ((line = readOne(readerStream, readCache)) != null) {
            int col = 0;
            BytesSplitter tabSplitter = new BytesSplitter(Constant.TAB);
            BytesSplitter commaSplitter = new BytesSplitter(Constant.COMMA);
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
                int valueIndex = 0;
                while (commaSplitter.hasNext()) {
                    double value = commaSplitter.next().toDouble();
                    x.fastGet(valueIndex++).add(value);
                }
                col++;
            }
            boolean allDrop = true;
            int zeroCount;
            int size = x.fastGet(0).size();
            for (int i = 0; i < 7; i++) {
                DoubleList xOne = x.fastGet(i);
                if (instance.maxZeroAF != -1) {
                    zeroCount = 0;
                    for (int j = 0; j < size; j++) {
                        if (xOne.fastGet(j) == 0) zeroCount++;
                    }
                    float zeroRate = zeroCount / (float) size;
                    if (zeroRate >= instance.maxZeroAF) {
                        writeCache.write(FILTERED_TEST);
                        if (i != 6) {
                            writeCache.write(Constant.TAB);
                        } else {
                            writeCache.write(Constant.NEWLINE);
                        }
                        xOne.clear();
                        continue;
                    }
                }
                allDrop = false;
                ChngptInstance.AdaptiveThresholdLogistic adaptiveThresholdLogistic = ChngptInstance.stepTestWithGLM(xOne.toArray(), y.toArray(), instance.lb, instance.ub, instance.testStatistic, instance.bootB, instance.nOfMC);
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
                xOne.clear();
            }
            ProgressBar bar = (ProgressBar) context.get("bar");
            if (bar != null) {
                bar.step(1);
            }
            readCache.clear();
            if (allDrop) {
                writeCache.clear();
                continue;
            }
            write(writerStream, writeCache);
        }
    }

    public static synchronized Bytes readOne(ReaderStream reader, ByteStream cache) throws IOException {
        int status = reader.readline(cache);
        if (status == -1) {
            return null;
        }
        return cache.toBytes().detach();
    }

    public static synchronized void write(WriterStream writer, ByteStream cache) throws IOException {
        if (cache.length() == 0) {
            return;
        }
        writer.write(cache.toBytes());
        cache.clear();
    }

    public MultiThreadAdaptThresholdRegression setThread(int thread) {
        this.thread = thread;
        return this;
    }
}
