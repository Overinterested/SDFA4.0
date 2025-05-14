package edu.sysu.pmglab.sdfa.merge;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.executor.ITask;
import edu.sysu.pmglab.sdfa.merge.manner.ContigMerger;
import edu.sysu.pmglab.sdfa.merge.method.MultiCSVMerger;
import edu.sysu.pmglab.sdfa.merge.output.GlobalMergeResultWriter;
import edu.sysu.pmglab.sdfa.merge.output.MultiCSVOutputter;
import edu.sysu.pmglab.sdfa.merge.output.MultiSSVOutputter;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.assembly.CSVAssembler;
import edu.sysu.pmglab.sdfa.sv.csv.ComplexSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVManager;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SingleFileSDSVManager;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;


/**
 * @author Wenjie Peng
 * @create 2024-10-04 02:01
 * @description
 */
public class MergeManager {
    int fileSize;
    File outputDir;
    SDFReadType readerMode;
    CSVAssembler globalCSVAssembler;
    List<ContigMerger> contigMergerList;
    GlobalMergeResultWriter globalMergeResultWriter;
    LinkedSet<String> globalContigNameList = new LinkedSet<>();

    private static MergeManager instance;

    private MergeManager(int threads, File outputDir) throws IOException {
        setOutputDir(outputDir);
        this.contigMergerList = new List<>(threads);
        SDSVManager sdsvManager = SDSVManager.getInstance();
        int fileSize = sdsvManager.numOfFileSize();
        this.fileSize = fileSize;
        globalCSVAssembler = new CSVAssembler(fileSize,  threads > 1);
        for (int i = 0; i < threads; i++) {
            contigMergerList.add(new ContigMerger(globalCSVAssembler, globalMergeResultWriter));
        }
        MultiSSVOutputter.initSupportRecords(fileSize);
        MultiCSVOutputter.initSupportRecords(fileSize);
        collectValidContigList();
    }

    public static MergeManager getInstance() {
        return instance;
    }

    public static MergeManager init(int threads, File outputDir) throws IOException {
        if (instance == null) {
            instance = new MergeManager(threads, outputDir);
        }
        return instance;
    }

    public List<ITask> mergeSSVTasks(int startContigIndex, int endContigIndex) {
        List<ITask> tasks = new List<>();
        for (int i = startContigIndex; i < endContigIndex; i++) {
            String contigName = globalContigNameList.valueOf(i);
            int finalI = i;
            tasks.add(((status, context) -> {
                ContigMerger rebuild = contigMergerList.fastGet(finalI - startContigIndex)
                        .init(contigName, readerMode);
                rebuild.merge();
            }));
        }
        return tasks;
    }

    public ITask mergeCSVTask() {
        return ((status, context) -> {
            List<CSVAssembler.FileAssembler> fileAssemblerList = globalCSVAssembler.getFileAssemblerList();
            TIntObjectHashMap<List<ComplexSV>> collectedCSVOfDiffType = new TIntObjectHashMap<>();
            // collect all csvs into container -> collectedCSVOfDiffType
            for (int i = 0; i < fileSize; i++) {
                TIntObjectHashMap<List<ComplexSV>> completeCSVs = fileAssemblerList.fastGet(i).getCompleteCSVs();
                if (completeCSVs.isEmpty()) {
                    continue;
                }
                int[] keys = completeCSVs.keys();
                for (int j = 0; j < keys.length; j++) {
                    int key = keys[j];
                    List<ComplexSV> value = completeCSVs.get(key);
                    if (value.isEmpty()) {
                        continue;
                    }
                    List<ComplexSV> lists = collectedCSVOfDiffType.get(key);
                    if (lists == null) {
                        lists = new List<>();
                        collectedCSVOfDiffType.put(key, lists);
                    }
                    lists.addAll(value.popFirst(value.size(),true));
                }
            }
            if (!collectedCSVOfDiffType.isEmpty()) {
                ByteStream cache = new ByteStream();
                Iterator<List<ComplexSV>> iterator = collectedCSVOfDiffType.valueCollection().iterator();
                while (iterator.hasNext()) {
                    List<ComplexSV> value = iterator.next();
                    value.sort(ComplexSV::compareTo);
                    List<ComplexSV> complexSVS = new List<>();
                    // init
                    ComplexSV firstComplexSV = value.popFirst();
                    String nameOfType = firstComplexSV.getNameOfType();
                    MultiCSVMerger csvMerger = MultiCSVMerger.getByDefault(nameOfType);
                    MultiCSVOutputter csvOutputter = MultiCSVOutputter.getByDefault(nameOfType);
                    complexSVS.add(firstComplexSV);
                    while (!value.isEmpty()) {
                        ComplexSV complexSV = value.popFirst();
                        boolean merge = csvMerger.merge(complexSV, complexSVS);
                        if (!merge) {
                            csvOutputter.outputTo(complexSVS, cache);
                            globalMergeResultWriter.unsafeWrite(cache.toBytes());
                            cache.clear();
                            complexSVS.clear();
                        }
                        complexSVS.add(complexSV);
                    }
                    // check the remaining csv in container
                    if (!complexSVS.isEmpty()) {
                        csvOutputter.outputTo(complexSVS, cache);
                        globalMergeResultWriter.unsafeWrite(cache.toBytes());
                        cache.clear();
                        complexSVS.clear();
                    }
                }
            }
        });
    }

    public IntInterval check(int startContigIndex, int endContigIndex) {
        if (startContigIndex >= globalContigNameList.size()) {
            return null;
        }
        endContigIndex = Math.min(endContigIndex, globalContigNameList.size());
        return new IntInterval(startContigIndex, endContigIndex);
    }

    public MergeManager setOutputDir(File outputDir) throws IOException {
        this.outputDir = outputDir;
        this.globalMergeResultWriter = GlobalMergeResultWriter.init(outputDir);
        return this;
    }

    public void collectValidContigList() throws IOException {
        SDSVManager instance = SDSVManager.getInstance();
        LinkedSet<SingleFileSDSVManager> fileManagers = instance.getFileManagers();
        for (SingleFileSDSVManager fileManager : fileManagers) {
            List<String> validContigNames = fileManager.getReader().getValidContigNames();
            this.globalContigNameList.addAll(validContigNames);
        }
    }

    public MergeManager setReaderMode(SDFReadType readerMode) {
        this.readerMode = readerMode;
        return this;
    }

}
