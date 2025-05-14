package edu.sysu.pmglab.sdfa.merge.manner;

import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.merge.method.MultiSSVMerger;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import gnu.trove.set.hash.TIntHashSet;

/**
 * @author Wenjie Peng
 * @create 2024-10-04 19:57
 * @description
 */
public class SSVTypeMerger {
    final int typeIndex;
    List<ISDSV> ssvList;
    final String nameOfType;
    TIntHashSet existFileIDSet;

    private List<ISDSV> popSSVList = new List<>();

    public SSVTypeMerger(SVTypeSign svTypeSign) {
        this.ssvList = new List<>();
        this.typeIndex = svTypeSign.getIndex();
        this.nameOfType = svTypeSign.getName();
        this.existFileIDSet = new TIntHashSet();
    }

    public void clear() {
        ssvList.clear();
        existFileIDSet.clear();
    }

    public List<ISDSV> merge(ISDSV sdsv) {
        if (ssvList.isEmpty()) {
            ssvList.add(sdsv);
            return null;
        }
        MultiSSVMerger merger = MultiSSVMerger.getByDefault(nameOfType);
        boolean merge = merger.merge(sdsv, ssvList);
        if (merge) {
            ssvList.add(sdsv);
            return null;
        } else {
            int size = ssvList.size();
            for (int i = 0; i < size; i++) {
                popSSVList.add(ssvList.popFirst());
            }
            ssvList.add(sdsv);
            return popSSVList;
        }
    }

    public List<ISDSV> getSSVList() {
        return ssvList;
    }
}
