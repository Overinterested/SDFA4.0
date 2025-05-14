package edu.sysu.pmglab.sdfa.sv.assembly;

import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.csv.ComplexSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Wenjie Peng
 * @create 2024-09-05 22:37
 * @description assemble multiple SDSVs
 */
public class CSVAssembler {
    final List<FileAssembler> fileAssemblerList;

    public CSVAssembler(int numOfFiles, boolean isParallel) {
        fileAssemblerList = new List<>(numOfFiles);
        for (int i = 0; i < numOfFiles; i++) {
            fileAssemblerList.set(i, new FileAssembler(isParallel));
        }
    }

    public void put(int indexOfFile, ISDSV sdsv) {
        fileAssemblerList.get(indexOfFile).put(sdsv);
    }


    public List<ISDSV> putThenGet(int indexOfFile, ISDSV sdsv) {
        return fileAssemblerList.get(indexOfFile).putAndCollect(sdsv);
    }

    public List<FileAssembler> getFileAssemblerList() {
        return fileAssemblerList;
    }

    public TIntObjectHashMap<List<ComplexSV>> getCompleteCSVs(int indexOfFile) {
        return fileAssemblerList.get(indexOfFile).getCompleteCSVs();
    }

    public static class FileAssembler {
        private TIntObjectHashMap<List<ComplexSV>> completeCSVs;
        private final ReentrantReadWriteLock.WriteLock writeLock;
        private final TIntObjectMap<List<ISDSV>> sdsvCollector = new TIntObjectHashMap<>();

        public FileAssembler(boolean isParallel) {
            completeCSVs = new TIntObjectHashMap<>();
            if (isParallel) {
                writeLock = new ReentrantReadWriteLock().writeLock();
            } else {
                writeLock = null;
            }
        }

        // put sdsv and at time get one complex sv when finishing collecting
        protected List<ISDSV> putAndCollect(ISDSV sdsv) {
            if (writeLock != null) {
                writeLock.lock();
            }
            try {
                int indexOfFile = sdsv.indexInFile();
                int numOfSubSVs = sdsv.numOfSubSVs();
                List<ISDSV> collectedSVs = sdsvCollector.get(indexOfFile);
                if (collectedSVs == null) {
                    collectedSVs = new List<>(numOfSubSVs);
                    collectedSVs.add(sdsv);
                    sdsvCollector.put(indexOfFile, collectedSVs);
                    return null;
                } else {
                    if (numOfSubSVs == (1 + collectedSVs.size())) {
                        // is completed
                        collectedSVs.add(sdsv);
                        sdsvCollector.remove(sdsv.getType().getIndex());
                        return collectedSVs;
                    } else {
                        // not completed
                        collectedSVs.add(sdsv);
                        return null;
                    }
                }
            } finally {
                if (writeLock != null) {
                    writeLock.unlock();
                }
            }
        }

        protected void put(ISDSV sdsv) {
            List<ISDSV> returns = putAndCollect(sdsv);
            if (returns != null) {
                if (writeLock != null) {
                    writeLock.lock();
                }
                try {
                    int typeIndex = sdsv.getType().getIndex();
                    List<ComplexSV> lists = completeCSVs.get(typeIndex);
                    if (lists == null) {
                        lists = new List<>();
                        completeCSVs.put(typeIndex, lists);
                    }
                    lists.add(ComplexSV.of(returns));
                } finally {
                    if (writeLock != null) {
                        writeLock.unlock();
                    }
                }
            }
        }

        public TIntObjectHashMap<List<ComplexSV>> getCompleteCSVs() {
            return completeCSVs;
        }
    }
}
