package edu.sysu.pmglab.sdfa.base;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.ccf.meta.ICCFMeta;
import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.container.indexable.ConcurrentLinkedSet;
import edu.sysu.pmglab.container.indexable.IndexableSet;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInfoManager;

/**
 * @author Wenjie Peng
 * @create 2025-02-24 03:10
 * @description
 */
public class SDFInfoManager {
    LinkedSet<Bytes> infoKeys = new LinkedSet<>();
    LinkedSet<String> stringInfoKeys = new LinkedSet<>();

    public SDFInfoManager(ICCFMeta meta) {
        List<CCFMetaItem> infoMetas = meta.get(VCFInfoManager.name());
        if (infoMetas != null && !infoMetas.isEmpty()) {
            StringArray infoKeys = infoMetas.fastGet(0).getValue();
            for (String value : infoKeys) {
                if (value != null){
                    this.infoKeys.add(new Bytes(value));
                }
            }
        }
    }

    public SDFInfoManager(IndexableSet<String> infoKeys) {
        if (infoKeys != null && !infoKeys.isEmpty()){
            for (String infoKey : infoKeys) {
                this.stringInfoKeys.add(infoKey);
                this.infoKeys.add(new Bytes(infoKey));
            }
        }
    }


    public int indexOf(String infoKey) {
        if (!stringInfoKeys.isEmpty()){
            return this.stringInfoKeys.indexOf(infoKey);
        }
        return infoKeys.indexOf(new Bytes(infoKey));
    }

    public LinkedSet<Bytes> getInfoKeys() {
        return infoKeys;
    }

    public int indexOf(Bytes infoKey) {
        return infoKeys.indexOf(infoKey);
    }

    public int sizeOfInfo(){
        return infoKeys.size();
    }
}
