package edu.sysu.pmglab.sdfa.merge.manner;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.merge.output.GlobalMergeResultWriter;
import edu.sysu.pmglab.sdfa.merge.output.MultiSSVOutputter;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import edu.sysu.pmglab.sdfa.sv.assembly.CSVAssembler;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVCachedLoserTree;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVLoserTree;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVManager;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SingleFileSDSVManager;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Collectors;

/**
 * @author Wenjie Peng
 * @create 2024-09-10 21:27
 * @description
 */
public class ContigMerger {
    String contigName;
    SDSVLoserTree loserTree;
    ByteStream cache = new ByteStream();
    TIntObjectMap<SSVTypeMerger> typeMapMerger;

    final CSVAssembler globalCSVAssembler;
    final GlobalMergeResultWriter globalMergeResultWriter;


    public ContigMerger(CSVAssembler csvAssembler, GlobalMergeResultWriter globalMergeResultWriter) {
        this.globalCSVAssembler = csvAssembler;
        this.typeMapMerger = new TIntObjectHashMap<>();
        this.globalMergeResultWriter = globalMergeResultWriter;
    }

    /**
     * clear existing storage before and initialize current merging contig name
     * @param contigName
     * @param readerMode
     * @return
     * @throws IOException
     */
    public synchronized ContigMerger init(String contigName, SDFReadType readerMode) throws IOException {
        this.contigName = contigName;
        SDSVManager sdsvManager = SDSVManager.getInstance();
        LinkedSet<SingleFileSDSVManager> fileManagers = sdsvManager.getFileManagers();
        typeMapMerger.forEachValue(ssvTypeMerger -> {
            ssvTypeMerger.clear();
            return true;
        });
        loserTree = new SDSVCachedLoserTree(
                new List<>(
                        fileManagers.stream()
                                .map(SingleFileSDSVManager::getReader)
                                .collect(Collectors.toList())
                ),
                readerMode, contigName
        );
        return this;
    }

    /**
     * merge simple SVs and collect complex SVs
     *
     * @throws IOException
     */
    public void merge() throws IOException {
        while (true) {
            ISDSV svWithMinCoordinate = loserTree.getNextMinRecord();
            if (svWithMinCoordinate == null) {
                // no sv can be loaded
                break;
            }
            int currFileIndex = loserTree.getLastIndex();
            if (svWithMinCoordinate.isComplexType()) {
                globalCSVAssembler.put(currFileIndex, svWithMinCoordinate);
            } else {
                SVTypeSign type = svWithMinCoordinate.getType();
                SSVTypeMerger ssvTypeMerger = typeMapMerger.get(type.getIndex());
                if (ssvTypeMerger == null) {
                    ssvTypeMerger = new SSVTypeMerger(type);
                    this.typeMapMerger.put(type.getIndex(), ssvTypeMerger);
                }
                List<ISDSV> mergedSDSV = ssvTypeMerger.merge(svWithMinCoordinate);
                if (mergedSDSV != null && !mergedSDSV.isEmpty()) {
                    MultiSSVOutputter.getByDefault(type.getName()).output(mergedSDSV, cache);
                    globalMergeResultWriter.safeWrite(cache.toBytes());
                    cache.clear();
                    mergedSDSV.clear();
                }
            }
        }
        // check remaining ssv
        Iterator<SSVTypeMerger> iterator = typeMapMerger.valueCollection().iterator();
        while (iterator.hasNext()) {
            SSVTypeMerger value = iterator.next();
            List<ISDSV> ssvList = value.getSSVList();
            if (!ssvList.isEmpty()) {
                MultiSSVOutputter.getByDefault(value.nameOfType).output(ssvList, cache);
                globalMergeResultWriter.safeWrite(cache.toBytes());
                cache.clear();
                ssvList.clear();
            }
        }
    }
}
