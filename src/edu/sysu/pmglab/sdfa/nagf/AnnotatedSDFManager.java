package edu.sysu.pmglab.sdfa.nagf;

import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.executor.ITask;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.nagf.reference.RefGenomicElementManager;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-09-27 00:39
 * @description manager svs from all annotated sdf files
 */
public class AnnotatedSDFManager {
    private List<SDSVGenomicIndexedAnnotation> fileIndexedAnnotation;

    private static AnnotatedSDFManager instance;

    /**
     * collect all annotated sdf files
     * @param annotatedDir
     * @param readerMode
     * @return
     * @throws IOException
     */
    public static AnnotatedSDFManager init(File annotatedDir, SDFReadType readerMode) throws IOException {
        if (instance != null) {
            return instance;
        }
        int index = 0;
        File[] files = annotatedDir.listFiles();
        if (files == null || files.length == 0) {
            LogBackOptions.getRootLogger().error("Please check whether " + annotatedDir + " contains annotated sdf files.");
            throw new UnsupportedOperationException();
        }
        List<SDSVGenomicIndexedAnnotation> sdsvGenomicIndexedAnnotations = new List<>();
        AnnotatedSDFManager annotatedSDFManager = new AnnotatedSDFManager();
        for (File file : files) {
            if (file.getName().endsWith(".sdf")) {
                SDFReader sdfReader = new SDFReader(file, readerMode);
                sdsvGenomicIndexedAnnotations.add(new SDSVGenomicIndexedAnnotation(index++, sdfReader));
                sdfReader.close();
            }
        }
        annotatedSDFManager.fileIndexedAnnotation = sdsvGenomicIndexedAnnotations;
        instance = annotatedSDFManager;
        return instance;
    }

    public List<ISDSV> getSDSVByFileID(int fileID) {
        return fileIndexedAnnotation.fastGet(fileID).sdsvCache;
    }

    static class SDSVGenomicIndexedAnnotation {
        final int fileID;
        long startPoint = 0;
        List<ISDSV> sdsvCache;
        final SDFReader reader;
        final long numOfRecords;

        public SDSVGenomicIndexedAnnotation(int fileID, SDFReader reader) {
            this.reader = reader;
            this.fileID = fileID;
            this.sdsvCache = new List<>();
            this.numOfRecords = reader.numOfRecords();
        }

        /**
         * continue loading the sdsv from the last start pointer until the start sdsv annotation index is larger than or equal to maxRefIndex
         * at the same time, the related sdsv is registered into the related sdsv index cache of responding rna elements
         * @param minRefIndex min reference rna index
         * @param maxRefIndex max reference rna index
         * @return ITask object
         */
        public ITask updateSDSVIndexInRelatedRefRNA(int minRefIndex, int maxRefIndex) {
            return ((status, context) -> {
                dropTopNoAnnotationSDSVs(minRefIndex);
                // seek th pointer reference file to [minRefIndex, maxRefIndex)
                reader.open();
                reader.limit(startPoint, reader.numOfRecords());

                int index = 0;
                int size = sdsvCache.size();
                if (size != 0) {
                    for (int i = 0; i < size; i++) {
                        IntList annotationIndexes = sdsvCache.fastGet(i).getAnnotationIndexes();
                        int updateFlag = updateRefRNARelatedSDSV(annotationIndexes, index++, minRefIndex, maxRefIndex);
                        // no overlap with [minRefIndex, maxRefIndex)
                        if (updateFlag == 0) {
                            return;
                        }
                        // drop reason: all the sdsv in the cache has annotation indexes, which can be proofed in the following for loop
//                        if (updateFlag == -1){
//                            index--;
//                        }
                    }
                }
                ISDSV sdsv;
                while ((sdsv = reader.read()) != null) {
                    IntList annotationIndexes = sdsv.getAnnotationIndexes();
                    int updateFlag = updateRefRNARelatedSDSV(annotationIndexes, index++, minRefIndex, maxRefIndex);
                    // no overlap with [minRefIndex, maxRefIndex)
                    if (updateFlag == 0) {
                        startPoint = reader.tell();
                        reader.close();
                        return;
                    }
                    if (updateFlag == 1) {
                        // has update
                        sdsvCache.add(sdsv);
                    }else {
                        // no annotation
                        index--;
                    }
                }
            });
        }

        public boolean check() {
            return startPoint < numOfRecords;
        }

        /**
         * update reference RNA related SDSV indexes
         *
         * @param annotationIndexes sdsv related SV
         * @param index             corresponding sdsv index in the cache list
         * @param minRefIndex       collected min reference pointer index
         * @param maxRefIndex       collected max reference pointer index
         * @return -1 represents no annotation, 0 represents no overlap, 1 represents overlapping and having been updated
         */
        private int updateRefRNARelatedSDSV(IntList annotationIndexes, int index,
                                            int minRefIndex, int maxRefIndex) {
            // no annotation
            if (annotationIndexes.isEmpty()) {
                return -1;
            }
            int startRefIndex = annotationIndexes.fastGet(0);
            // no overlap
            if (startRefIndex >= maxRefIndex) {
                return 0;
            }
            // update
            int endRefIndex = annotationIndexes.fastGet(1);
            RefGenomicElementManager.getInstance().updateRelatedSDSVInRefRNA(
                    Math.max(minRefIndex, startRefIndex) - minRefIndex,
                    Math.min(maxRefIndex - 1, endRefIndex) - minRefIndex,
                    fileID, index
            );
            return 1;
        }

        /**
         * drop SV whose annotation indexes are less than current reference index, minRefIndex
         * @param minRefIndex
         */
        protected void dropTopNoAnnotationSDSVs(int minRefIndex) {
            int size = sdsvCache.size();
            while (size > 0) {
                IntList annotationIndexes = sdsvCache.fastGet(0).getAnnotationIndexes();
                // drop SV whose annotation indexes are less than current reference index, minRefIndex
                if (annotationIndexes.fastGet(1) < minRefIndex) {
                    sdsvCache.popFirst();
                } else {
                    return;
                }
                size--;
            }
        }

        public SDFReader getReader() {
            return reader;
        }
    }

    /**
     * for all sdf file, the related sdsv is registered into the related sdsv index cache of responding rna elements
     * @param minRefIndex
     * @param maxRefIndex
     * @return a list tasks for all sdf files
     */
    public List<ITask> updateSDSV(int minRefIndex, int maxRefIndex) {
        List<ITask> tasks = new List<>();
        for (SDSVGenomicIndexedAnnotation sdsvGenomicIndexedAnnotation : fileIndexedAnnotation) {
            if (sdsvGenomicIndexedAnnotation.check()) {
                tasks.add(
                        sdsvGenomicIndexedAnnotation.updateSDSVIndexInRelatedRefRNA(minRefIndex, maxRefIndex)
                );
            }
        }
        if (tasks.isEmpty()) {
            return null;
        }
        return tasks;
    }

    public static AnnotatedSDFManager getInstance() {
        return instance;
    }

    public int numOfSamples() {
        return fileIndexedAnnotation.size();
    }

    public SDFReader getReaderByIndex(int index){
        return fileIndexedAnnotation.fastGet(index).reader;
    }

    public int sizeOfAnnotationFile(){
        return fileIndexedAnnotation.size();
    }
}
