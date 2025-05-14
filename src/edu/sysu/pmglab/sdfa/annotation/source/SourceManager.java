package edu.sysu.pmglab.sdfa.annotation.source;

import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.executor.ITask;
import edu.sysu.pmglab.sdfa.annotation.output.SourceOutputManager;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Wenjie Peng
 * @create 2024-09-08 20:21
 * @description
 */
public class SourceManager {
    LinkedSet<Source> sources = new LinkedSet<>();
    Map<String, Source> fileNameMap = new HashMap<>();
    private static final SourceManager instance = new SourceManager();

    private SourceManager() {

    }

    public static void switchToWrite() throws IOException {
        for (Source source : instance.sources) {
            SourceOutputManager.addSourceOutput(source);
        }
    }

    public List<ITask> loadSource(){
        int size = sources.size();
        List<ITask> tasks = new List<>(size);
        for (int i = 0; i < size; i++) {
            int finalI = i;
            tasks.add(((status, context) -> sources.valueOf(finalI).buildIntervalTree()));
        }
        return tasks;
    }

    public List<ITask> annotateTask(int startFileIndex, int endFileIndex) {
        SDSVManager sdsvManager = SDSVManager.getInstance();
        int size = sdsvManager.numOfFileSize();
        List<ITask> tasks = new List<>();
        for (int i = 0; i < sources.size(); i++) {
            Source source = sources.valueOf(i);
            for (int j = startFileIndex; j < Math.min(size, endFileIndex); j++) {
                tasks.addAll(source.annotate(sdsvManager.getByIndex(j)));
            }
        }
        return tasks;
    }

    public Source getSourceByIndex(int index) {
        return sources.valueOf(index);
    }

    public Source getSourceByFile(String fileName){
        return fileNameMap.get(fileName);
    }

    public static SourceManager getManager() {
        return instance;
    }

    /**
     * add source into output manager
     * @param source
     */
    public synchronized static void addSource(Source source) {
        instance.fileNameMap.put(source.getFile().getName(), source);
        instance.sources.add(source.setSourceID(instance.sources.size()));
    }

    public static int numOfSource(){
        return instance.sources.size();
    }

    @Deprecated
    public void clear(){
        sources = new LinkedSet<>();
    }
    @Deprecated
    public void addExtraSource(Source source){
        sources.add(source);
    }
}
