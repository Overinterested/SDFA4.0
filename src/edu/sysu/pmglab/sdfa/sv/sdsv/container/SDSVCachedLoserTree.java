package edu.sysu.pmglab.sdfa.sv.sdsv.container;

import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.annotation.output.SourceOutput;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-10-08 01:58
 * @description
 */
public class SDSVCachedLoserTree extends SDSVLoserTree {
    final int perCacheCount;
    List<List<ISDSV>> cache = new List<>();
    public static int TOTAL_SV_CACHE_THRESHOLD = 16384;

    public SDSVCachedLoserTree(List<SDFReader> readers, SDFReadType readerMode, String contigName) throws IOException {
        super(readers.size(), readerMode);
        cache.clear();
        this.perCacheCount = TOTAL_SV_CACHE_THRESHOLD / this.numFiles;
        List<SDFReader> initializedReaders = initializeReaders(readers, contigName);
        cacheAndPrepareReaders(initializedReaders);
        initializeLoserTree();
    }

    public SDSVCachedLoserTree(List<File> files, SDFReadType readerMode) throws IOException {
        super(files.size(), readerMode);
        this.perCacheCount = TOTAL_SV_CACHE_THRESHOLD / this.numFiles;
        cache.clear();
        List<SDFReader> readers = new List<>(files.size());
        for (int i = 0; i < files.size(); i++) {
            SDFReader sdfReader = new SDFReader(files.get(i), readerMode);
            readers.add(sdfReader);
        }
        cacheAndPrepareReaders(readers);
        initializeLoserTree();
    }


    @Override
    public boolean getMinRecords(int numOfRecords, List<ISDSV> collectedSDSVList, IntList fileIDList, List<SourceOutput> sourceOutputList) throws IOException {
        int fileID;
        int annotationSize;
        int startPointer, endPointer;
        for (int i = 0; i < numOfRecords; i++) {
            if (records[rootIndex] == null) {
                // finish reading
                return false;
            }
            // get min record
            fileID = rootIndex;
            ISDSV minRecord = records[rootIndex];
            // update loser tree
            updateTree(rootIndex);
            // judge annotation
            IntList annotationIndexes = minRecord.getAnnotationIndexes();
            if (annotationIndexes == null || (annotationSize = annotationIndexes.size()) == 0) {
                continue;
            }
            for (int j = 0; j < annotationSize / 2; j++) {
                startPointer = annotationIndexes.fastGet(2 * j);
                if (startPointer == -1) {
                    continue;
                }
                endPointer = annotationIndexes.fastGet(2 * j + 1);
                sourceOutputList.fastGet(j).expand(startPointer, endPointer);

            }
            collectedSDSVList.add(minRecord);
            fileIDList.add(fileID);
        }
        return true;
    }

    @Override
    public ISDSV getNextMinRecord() throws IOException {
        if (records[rootIndex] == null) {
            // 所有文件都处理完毕
            return null;
        }
        // 当前的最小值
        ISDSV minRecord = records[rootIndex].setFileIndex(rootIndex);
        lastIndex = rootIndex;
        // 先检查缓存，如果没有再从胜者文件中读取下一条记录
        updateTree(rootIndex);
        return minRecord;
    }

    public static void setTotalSvCacheThreshold(int totalSVCacheThreshold) {
        TOTAL_SV_CACHE_THRESHOLD = totalSVCacheThreshold;
    }


    protected void updateTree(int index) throws IOException {
        List<ISDSV> svs = cache.fastGet(index);
        if (!svs.isEmpty()) {
            records[rootIndex] = svs.popFirst();
        } else {
            SDFReader reader = readers[rootIndex];
            if (reader.isClosed()) {
                reader.openLastWithLimit();
            }
            for (int i = 0; i < perCacheCount; i++) {
                ISDSV sv = reader.read();
                if (sv == null) {
                    break;
                }
                svs.add(sv);
            }
            reader.close();
            records[rootIndex] = svs.isEmpty() ? null : svs.popFirst();
        }
        adjust(rootIndex);
    }


    /**
     * limit the reading range of sdf file at contigName
     *
     * @param readers
     * @param contigName limit contig name
     * @return
     * @throws IOException
     */
    private List<SDFReader> initializeReaders(List<SDFReader> readers, String contigName) throws IOException {
        List<SDFReader> initializedReaders = new List<>(readers.size());
        for (int i = 0; i < readers.size(); i++) {
            SDFReader sdfReader = readers.get(i);
            if (contigName != null) {
                sdfReader = new SDFReader(sdfReader.getReaderOption());
                SDFReader limit = sdfReader.limit(contigName);
                if (limit == null) {
                    sdfReader.limit(0, 0);
                }
                sdfReader.close();
            }
            initializedReaders.add(sdfReader);
        }
        return initializedReaders;
    }

    private void cacheAndPrepareReaders(List<SDFReader> readers) throws IOException {
        cache.clear();
        int loadCacheCount = 0;
        int reallocateCacheCount = 0;
        for (int i = 0; i < this.numFiles; i++) {
            SDFReader sdfReader = readers.get(i);
            List<ISDSV> currCache = new List<>();
            cache.add(currCache);
            sdfReader.openLastWithLimit();
            for (int j = 0; j < perCacheCount + reallocateCacheCount; j++) {
                ISDSV sdsv = sdfReader.read();
                if (sdsv == null) {
                    break;
                }
                currCache.add(sdsv);
                loadCacheCount++;
            }
            reallocateCacheCount = (perCacheCount + reallocateCacheCount - loadCacheCount) / this.numFiles;
            loadCacheCount = 0;
            records[i] = currCache.isEmpty() ? null : currCache.popFirst();  // Use remove(0) instead of popFirst() for ArrayList.
            this.readers[i] = sdfReader;
            sdfReader.close();
        }
    }
}
