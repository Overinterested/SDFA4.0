package edu.sysu.pmglab.sdfa.base;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFTable;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.ccf.meta.ICCFMeta;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.container.indexable.ConcurrentLinkedSet;
import edu.sysu.pmglab.container.indexable.IndexableMap;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFFormatManager;
import edu.sysu.pmglab.sdfa.sv.vcf.format.FormatAttrBox;
import edu.sysu.pmglab.sdfa.sv.vcf.format.FormatAttrType;


/**
 * @author Wenjie Peng
 * @create 2025-02-24 02:19
 * @description
 */
public class SDFFormatManager {
    List<FormatAttrBox> formatAttrBoxList;
    final LinkedSet<Bytes> formatAttrNameList;

    LinkedSet<String> stringFormatAttrNameList = new LinkedSet<>();

    public SDFFormatManager(ICCFMeta meta) {
        List<CCFMetaItem> formatMetaList = meta.get(VCFFormatManager.name());
        if (formatMetaList == null || formatMetaList.isEmpty()) {
            formatAttrNameList = null;
            formatAttrBoxList = null;
        } else {
            formatAttrBoxList = new List<>();
            formatAttrNameList = new LinkedSet<>();
            CCFMetaItem formatAttrList = formatMetaList.fastGet(0);
            StringArray values = formatAttrList.getValue();
            if (values != null) {
                for (String value : values) {
                    if (value == null) {
                        continue;
                    }
                    String internName = value.intern();
                    formatAttrNameList.add(new Bytes(internName));
                    formatAttrBoxList.add(FormatAttrType.getByName(internName));
                }
            }
        }
    }

    public SDFFormatManager(IndexableMap<String, FormatAttrBox> formatDetails) {
        if (formatDetails != null && !formatDetails.isEmpty()) {
            this.formatAttrBoxList = new List<>();
            this.formatAttrNameList = new ConcurrentLinkedSet<>();
            int size = formatDetails.size();
            for (int i = 0; i < size; i++) {
                String tmp = formatDetails.keyOfIndex(i);
                this.stringFormatAttrNameList.add(tmp);
                this.formatAttrNameList.add(new Bytes(tmp));
                this.formatAttrBoxList.add(formatDetails.getByIndex(i));
            }
        } else {
            this.formatAttrBoxList = null;
            this.formatAttrNameList = null;
            this.stringFormatAttrNameList = null;
        }
    }


    public Object decode(List<Bytes> encodedValues, int index) {
        return formatAttrBoxList.fastGet(index).decode(encodedValues.fastGet(index));
    }

    public static Object decode(SDFFormatManager sdfFormatManager, List<Bytes> encodedValues, int index) {
        return sdfFormatManager.formatAttrBoxList.fastGet(index).decode(encodedValues.fastGet(index));
    }

    public LinkedSet<Bytes> getFormatAttrNameList() {
        return formatAttrNameList;
    }

    public int indexOf(String formatName) {
        if (!stringFormatAttrNameList.isEmpty()){
            return stringFormatAttrNameList.indexOf(formatName);
        }
        if (formatAttrNameList == null || formatAttrNameList.isEmpty()) {
            return -1;
        }
        return formatAttrNameList.indexOf(new Bytes(formatName));
    }

    public int indexOf(Bytes formatName) {
        if (formatAttrNameList == null || formatAttrNameList.isEmpty()) {
            return -1;
        }
        return formatAttrNameList.indexOf(formatName);
    }

    public Object getValue(int index, Bytes encodedValue) {
        return formatAttrBoxList.fastGet(index).decode(encodedValue);
    }

    public void replaceWithExtractedSubjects(IntList extractSubject) {
        int subjectSize = extractSubject.size();
        int formatSize = formatAttrBoxList.size();
        List<FormatAttrBox> newFormatAttrBoxList = new List<>();
        for (int i = 0; i < formatSize; i++) {
            FormatAttrBox rawFormatBox = formatAttrBoxList.fastGet(i);
            if (rawFormatBox.sizeOfIndividual() == 0) {
                continue;
            }
            FormatAttrBox newFormatBox = rawFormatBox.newInstance();

            int itemSize = newFormatBox.sizeOfItemInEachIndividual();
            for (int j = 0; j < subjectSize; j++) {
                int indexOfSubject = extractSubject.fastGet(j);
                for (int k = 0; k < itemSize; k++) {
                    newFormatBox.forceLoadOne(rawFormatBox.getByIndex(indexOfSubject * itemSize + k));
                }
            }
            newFormatAttrBoxList.add(newFormatBox);
        }
        this.formatAttrBoxList = newFormatAttrBoxList;
    }

    public List<Bytes> encode() {
        List<Bytes> res = new List<>(formatAttrNameList.size());
        for (int i = 0; i < formatAttrBoxList.size(); i++) {
            res.add(formatAttrBoxList.fastGet(i).encode());
        }
        return res;
    }
    public void encodeTo(List<Bytes> cache){
        cache.clear();
        int size = formatAttrBoxList.size();
        for (int i = 0; i < size; i++) {
            cache.add(
                    formatAttrBoxList.fastGet(i).encode()
            );
        }
    }

    public FormatAttrBox getBox(int index) {
        return formatAttrBoxList.fastGet(index);
    }

    public int size(){
        return formatAttrNameList.size();
    }
}
