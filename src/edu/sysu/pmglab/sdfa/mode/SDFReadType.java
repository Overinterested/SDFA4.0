package edu.sysu.pmglab.sdfa.mode;

import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Wenjie Peng
 * @create 2025-04-27 15:08
 * @description
 */
public enum SDFReadType {
    FULL(FullMode.getInstance()),
    PLINK(PlinkMode.getInstance()),
    MERGE(MergeMode.getInstance()),
    COORDINATE(CoordinateMode.getInstance()),
    ANNOTATION(AnnotationMode.getInstance()),
    ANNOTATION_GT(AnnotationGTMode.getInstance());
    IReaderMode readerMode;

    public static final HashMap<String, SDFReadType> readModeMap = new HashMap<>();

    static {
        readModeMap.put("full", FULL);
        readModeMap.put("plink", PLINK);
        readModeMap.put("merge", MERGE);
        readModeMap.put("coordinate", COORDINATE);
        readModeMap.put("annotation", ANNOTATION);
        readModeMap.put("annotation_GT", ANNOTATION_GT);
    }

    SDFReadType(IReaderMode readerMode) {
        this.readerMode = readerMode;
    }

    public static void add(String name, SDFReadType readType){
        readModeMap.put(name, readType);
    }

    public IReaderMode getReaderMode() {
        return readerMode;
    }
}
