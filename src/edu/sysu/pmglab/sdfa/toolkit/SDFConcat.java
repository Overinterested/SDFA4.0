package edu.sysu.pmglab.sdfa.toolkit;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.meta.CCFMeta;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.executor.Pipeline;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.SDFTable;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVManager;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SingleFileSDSVManager;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Wenjie Peng
 * @create 2024-10-10 18:57
 * @description
 */
public class SDFConcat {
    int thread;
    Logger logger;
    File outputPath;
    boolean silent = false;
    private int concatTime;
    LinkedSet<String> individuals;
    private List<WrappedFile> concatQueue;

    private final File inputDir;
    private final File outputDir;

    final AtomicInteger concatCount = new AtomicInteger();
    final AtomicInteger concatRound = new AtomicInteger();

    public SDFConcat(Object inputDir, Object outputDir) {
        this.thread = 1;
        this.inputDir = new File(inputDir.toString());
        this.outputDir = new File(outputDir.toString());
        this.outputPath = FileUtils.getSubFile(this.outputDir, "concat_result.sdf");
    }


    /**
     * return a flag in an int value:\n
     * -1: both SDF files is empty\n
     * 0: both SDF files are full and individuals are same\n
     * 1: the first SDF file is full in the same individuals, the second is empty\n
     * 2: the first SDF file is empty, the second is full in the same individuals
     *
     * @param reader1
     * @param reader2
     * @return int
     */
    private int check(SDFReader reader1, SDFReader reader2) {
        boolean check1 = reader1.remaining() != 0, check2 = reader2.remaining() != 0;
        if (!check1 && !check2) {
            // both are empty
            return -1;
        }
        if (check1 && check2) {
            // both are full
            synchronized (this.getClass()) {
                if (individuals == null) {
                    individuals = reader1.getIndividuals();
                }
            }
            return 0;
        }
        synchronized (this.getClass()) {
            if (individuals == null) {
                individuals = new LinkedSet<>((check1 ? reader1 : reader2).getIndividuals().asUnmodifiable());
            }
        }
        return check1 ? 1 : 2;
    }

    private File concat(SDFReader k, SDFReader v) throws IOException {
        k.open();
        k.seek(0);
        v.open();
        v.seek(0);
        SVContig contig = SVContig.merge(k.getReaderOption().getSDFTable().getContig(), v.getReaderOption().getSDFTable().getContig());
        CCFReader kReader = k.getReader();
        CCFReader vReader = v.getReader();
        IRecord kRecord = kReader.getRecord();
        IRecord vRecord = vReader.getRecord();
        IntList kCoordinateCache = new IntList(3);
        IntList vCoordinateCache = new IntList(3);
        for (int i = 0; i < 3; i++) {
            kCoordinateCache.add(0);
            vCoordinateCache.add(0);
        }
        IntList kCSVLocationCache = new IntList();
        IntList vCSVLocationCache = new IntList();
        CCFWriter writer = CCFWriter.setOutput(FileUtils.getSubFile(outputDir, concatCount.get() + ".sdf")).addFields(kReader.getAllFields()).instance();
        concatCount.incrementAndGet();
        List<String> validContigNames = contig.getContigNames();
        for (int i = 0; i < validContigNames.size(); i++) {
            String chromosome = validContigNames.fastGet(i);
            boolean kLimit = k.limit(chromosome) != null;
            boolean vLimit = v.limit(chromosome) != null;
            if (kLimit || vLimit) {
                if (kLimit && vLimit) {
                    //region both contain records of current contig
                    boolean kRead = kReader.read(kRecord);
                    boolean vRead = vReader.read(vRecord);
                    if (kRead && vRead) {
                        int compare;
                        IntList kCoordinate;
                        IntList vCoordinate;
                        while (true) {
                            // update chr index within coordinate
                            kCoordinate = kRecord.get(0);
                            vCoordinate = vRecord.get(0);
                            updateCoordinate(k, contig, kCoordinate, kCoordinateCache);
                            updateCoordinate(v, contig, vCoordinate, vCoordinateCache);
                            kRecord.set(0, kCoordinateCache);
                            vRecord.set(0, vCoordinateCache);
                            // update csv chr index list
                            IntList kCSVLocation = kRecord.get(13);
                            updateCSVLocation(k, contig, kRecord, kCSVLocation, kCSVLocationCache);
                            IntList vCSVLocation = vRecord.get(13);
                            updateCSVLocation(v, contig, kRecord, vCSVLocation, vCSVLocationCache);

                            compare = Integer.compare(kCoordinateCache.fastGet(1), vCoordinateCache.fastGet(1));
                            compare = compare == 0 ? Integer.compare(kCoordinateCache.fastGet(2), vCoordinateCache.fastGet(2)) : compare;
                            if (compare < 0) {
                                writer.write(kRecord);
                                kRead = kReader.read(kRecord);
                                if (!kRead) {
                                    while (vReader.read(vRecord)) {
                                        updateCoordinate(v, contig, vRecord.get(0), vCoordinateCache);
                                        vRecord.set(0, vCoordinateCache);
                                        writer.write(vRecord);
                                    }
                                    break;
                                }
                            } else {
                                writer.write(vRecord);
                                vRead = vReader.read(vRecord);
                                if (!vRead) {
                                    while (kReader.read(kRecord)) {
                                        updateCoordinate(k, contig, kRecord.get(0), kCoordinateCache);
                                        kRecord.set(0, kCoordinateCache);
                                        writer.write(kRecord);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    //endregion
                } else {
                    //region only one contain records of current contig
                    SDFReader tmp = kLimit ? k : v;
                    CCFReader tmpReader = kLimit ? kReader : vReader;
                    IRecord record = tmpReader.getRecord();
                    while (tmpReader.read(record)) {
                        // update coordinate
                        IntList coordinate = record.get(0);
                        updateCoordinate(tmp, contig, coordinate, kCoordinateCache);
                        record.set(0, kCoordinateCache);
                        // update csv location
                        IntList csvLocation = record.get(13);
                        updateCSVLocation(tmp, contig, record, csvLocation, kCSVLocationCache);
                        writer.write(record);
                    }
                    //endregion
                }
            }
        }
        CCFMeta kMeta = k.getReaderOption().getSDFTable().getMetaExceptContig();
        writer.addMeta(new CCFMetaItem(SDFTable.SDF_INDIVIDUAL_TAG, FieldType.stringIndexableSet, individuals));
        writer.addMeta(kMeta);
        writer.addMeta(contig.save());
        writer.close();
        CCFReader reader = new CCFReader(writer.getFile());
        reader.close();
        return writer.getFile();
    }

    public void submitTo(Workflow workflow) {
        ProgressBar bar = null;
        try {
            prepare(workflow);
            if (!silent && LogBackOptions.getRootLogger() != null) {
                LogBackOptions.getRootLogger().info("The concat task will spend " + concatTime + " rounds to concatenate all into one.");
            }
            List<WrappedFile> currentRoundFiles = new List<>();
            do {
                if (concatQueue.size() >= 2) {
                    bar = new ProgressBar.Builder()
                            .setTextRenderer("Start round " + concatRound.get() + " concat", "times")
                            .setInitialMax(concatQueue.size() / 2)
                            .build();
                    concatRound.incrementAndGet();
                }
                while (concatQueue.size() >= 2) {
                    WrappedFile k = concatQueue.popFirst();
                    WrappedFile v = concatQueue.popFirst();
                    if (concatRound.get() > 1) {
                        currentRoundFiles.add(k);
                        currentRoundFiles.add(v);
                    }
                    ProgressBar finalBar = bar;
                    workflow.addTask((status, context) ->
                            {
                                SDFReader var1 = new SDFReader(k.file);
                                SDFReader var2 = new SDFReader(v.file);
                                int checkFlag = check(var1, var2);
                                if (checkFlag == 0) {
                                    updateArray(concat(var1, var2), false);
                                    var1.closeAll();
                                    var2.closeAll();
                                    var1 = null;
                                    var2 = null;
                                } else {
                                    LiveFile file1 = var1.getFile();
                                    LiveFile file2 = var2.getFile();
                                    var1.closeAll();
                                    var1 = null;
                                    var2.closeAll();
                                    var2 = null;
                                    synchronized (this.getClass()) {
                                        File outputFile = FileUtils.getSubFile(outputDir, concatCount.get() + ".sdf");
                                        switch (checkFlag) {
                                            case -1:
                                            case 1:
                                                // both is empty
                                                // first is full
                                                file1.copyFileTo(outputFile);
                                                updateArray(outputFile, false);
                                                break;
                                            case 2:
                                                // second is full
                                                file2.copyFileTo(outputFile);
                                                updateArray(outputFile, false);
                                                break;
                                            default:
                                                break;
                                        }
                                        concatCount.incrementAndGet();
                                    }
                                }
                                if (!silent && LogBackOptions.getRootLogger() != null) {
                                    finalBar.step(1);
                                }
                            }
                    );
                }
                workflow.execute();
                workflow.clearTasks();
                if (bar != null) {
                    bar.close();
                }
                // 在每轮合并后立即删除该轮使用的中间文件
                if (concatRound.get() > 1 && !currentRoundFiles.isEmpty()) {
                    for (WrappedFile deleteFile : currentRoundFiles) {
                        if (!deleteFile.raw) {
                            deleteFile.file.delete();
                        }
                    }
                }

            } while (concatQueue.size() != 1);
            WrappedFile file = concatQueue.popFirst();
            boolean successRename = new File(file.file.getPath()).renameTo(outputPath);
            if (!successRename) {
                outputPath = FileUtils.getSubFile(outputDir, UUID.randomUUID().toString());
                new File(file.file.getPath()).renameTo(outputPath);
            }
            if (!successRename) {
                LogBackOptions.getRootLogger().warn("The concat task finishes and the result is stored at " + outputPath);
            } else {
                LogBackOptions.getRootLogger().info("The concat task finishes and the result is stored at " + outputPath);
            }
        } finally {
            //
        }

    }

    public void submit() throws IOException {
        submitTo(new Workflow(thread));
    }

    private synchronized void updateArray(File file, boolean raw) {
        concatQueue.add(new WrappedFile(file, raw));
    }

    public SDFConcat threads(int thread) {
        this.thread = thread;
        return this;
    }

    public SDFConcat setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public SDFConcat silent(boolean silent) {
        this.silent = silent;
        return this;
    }

    private void updateCoordinate(SDFReader reader, SVContig contig, IntList raw, IntList replace) {
        String contigName = reader.getContigByIndex(raw.fastGet(0));
        int index = contig.getContigIndexByName(contigName);
        replace.fastSet(0, index);
        replace.fastSet(1, raw.fastGet(1));
        replace.fastSet(2, raw.fastGet(2));
    }

    private void updateCoordinate(int contigIndex, IntList raw, IntList replace) {
        replace.fastSet(0, contigIndex);
        replace.fastSet(1, raw.fastGet(1));
        replace.fastSet(2, raw.fastGet(2));
    }

    private void updateCSVLocation(SDFReader reader, SVContig contig, IRecord record, IntList raw, IntList replace) {
        replace.clear();
        if (!raw.isEmpty()) {
            for (int j = 0; j < raw.size(); j++) {
                int indexOfContigInRaw = raw.fastGet(j);
                if (indexOfContigInRaw == -1) {
                    replace.add(-1);
                    continue;
                }
                String tmpContigName = reader.getContigByIndex(indexOfContigInRaw);
                int currTmpContigIndex = contig.getContigIndexByName(tmpContigName);
                replace.add(currTmpContigIndex);
            }
            record.set(13, replace);
        }
    }

    public File getOutputPath() {
        return outputPath;
    }

    private void prepare(Workflow workflow) {
        SDSVManager sdsvManager = SDSVManager.getInstance();
        if (sdsvManager == null) {
            sdsvManager = SDSVManager.of(inputDir).setOutputDir(outputDir);
        }
        concatQueue = new List<>();
        List<Pipeline> pipelines = sdsvManager.parseToSDFFileTask();
        for (Pipeline pipeline : pipelines) {
            workflow.addTask(pipeline);
        }
        workflow.execute();
        workflow.clearTasks();
        for (SingleFileSDSVManager singleFileSDSVManager : sdsvManager.getFileManagers()) {
            concatQueue.add(new WrappedFile(singleFileSDSVManager.getSdfFile(), true));
        }
        concatTime = (int) (Math.log(concatQueue.size()) / Math.log(2)) + 1;
    }

    private static class WrappedFile {
        final File file;
        final boolean raw;

        public WrappedFile(File file, boolean raw) {
            this.file = file;
            this.raw = raw;
        }
    }
}
