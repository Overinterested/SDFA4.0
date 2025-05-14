package edu.sysu.pmglab.sdfa.sv.vcf;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.ccf.CCFTable;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;

/**
 * @author Wenjie Peng
 * @create 2024-08-29 01:07
 * @description
 */
public class VCFInfoManager {
    int indexOfEnd = -1;
    int indexOfLen = -1;
    int indexOfType = -1;
    int indexOfContig2 = -1;
    private SVTypeSign typeSign;
    private boolean initType = false;
    private BytesSplitter equalSplit = new BytesSplitter(Constant.EQUAL);
    private BytesSplitter semicolonSplit = new BytesSplitter(Constant.SEMICOLON);
    private final IntList dropFieldIndex = new IntList();
    public final List<Bytes> essentialValues = new List<>();
    private final DynamicIndexableMap<Bytes, Bytes> indexedInfoFields = new DynamicIndexableMap<>();

    public static Bytes END = new Bytes("END" );
    public static Bytes SVLEN = new Bytes("SVLEN" );
    public static Bytes SVTYPE = new Bytes("SVTYPE" );
    public static Bytes CONTIG2 = new Bytes("CHR2" );
    public static Bytes EXIST = new Bytes("." );
    private static final List<Bytes> drop_fields = new List<>();
    // UKBB Config
    public static final Bytes SVSIZE = new Bytes("SVSIZE" );

    // potential drop fields for complex SVs
    public static final List<Bytes> potentialDropFields = List.wrap(
            new Bytes[]{
                    new Bytes("POS2" ),
                    new Bytes("END2" ),
                    new Bytes("SVLEN2" ),
                    new Bytes("SVTYPE2" )
            }
    );

    static {
        drop_fields.add(CONTIG2);
        drop_fields.add(new Bytes("CHR" ));
        drop_fields.add(new Bytes("POS" ));
        drop_fields.add(SVSIZE);

    }

    private VCFInfoManager() {

    }

    private VCFInfoManager(List<Bytes> infoFieldSet, boolean drop) {
        int dropIndex = 0;
        boolean redundant = false;
        for (Bytes infoField : infoFieldSet) {
            if (infoField.equals(END)) {
                indexOfEnd = dropIndex;
                dropFieldIndex.add(dropIndex);
                if (drop) {
                    continue;
                }
            } else if (infoField.equals(SVTYPE)) {
                indexOfType = dropIndex;
                dropFieldIndex.add(dropIndex);
                if (drop) {
                    continue;
                }
            } else if (infoField.equals(SVLEN)) {
                indexOfLen = dropIndex;
                dropFieldIndex.add(dropIndex);
                if (drop) {
                    continue;
                }
            }
            for (Bytes dropField : drop_fields) {
                if (infoField.equals(dropField)) {
                    redundant = true;
                    dropFieldIndex.add(dropIndex);
                    break;
                }
            }
            if (drop && redundant) {
                redundant = false;
                continue;
            }
            dropIndex++;
            indexedInfoFields.put(infoField.detach(), null);
        }
    }

    public static VCFInfoManager init(List<Bytes> infoFieldSet) {
        return new VCFInfoManager(infoFieldSet, false);
    }

    /**
     * update info keys with input info
     *
     * @param info
     */
    public void parse(Bytes info) {
        initType = false;
        for (int i = 0; i < indexedInfoFields.size(); i++) {
            indexedInfoFields.putByIndex(i, null);
        }
        BytesSplitter items = semicolonSplit.init(info);
        int count;
        int index = -1;
        while (items.hasNext()) {
            Bytes infoItem = items.next();
            equalSplit.init(infoItem);
            count = 0;
            while (equalSplit.hasNext()) {
                Bytes value = equalSplit.next();
                switch (count++) {
                    case 0:
                        index = indexedInfoFields.indexOfKey(value);
                        if (index == -1) {
                            indexedInfoFields.put(value.detach(), EXIST);
                            index = indexedInfoFields.size() - 1;
                        }
                        break;
                    case 1:
                        indexedInfoFields.putByIndex(index, value.detach());
                        break;
                    default:
                        break;
                }
                if (count >= 2) {
                    break;
                }
            }
            if (count == 1) {
                indexedInfoFields.putByIndex(index, EXIST);
            }
        }

    }

    public int getEnd() {
        int indexOfEnd = getIndexOfEnd();
        if (indexOfEnd == -1) {
            return -1;
        }
        try {
            Bytes value = indexedInfoFields.getByIndex(indexOfEnd);
            if (value == null) {
                return -1;
            }
            return value.toInt();
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public int getLen() {
        int indexOfLen = getIndexOfLen();
        if (indexOfLen == -1) {
            return -1;
        }
        try {
            Bytes value = indexedInfoFields.getByIndex(indexOfLen);
            if (value == null) {
                return -1;
            }
            return value.toInt();
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public SVTypeSign getType() {
        if (initType) {
            return typeSign;
        }
        initType = true;
        int indexOfType = getIndexOfType();
        if (indexOfType == -1) {
            return (typeSign = null);
        }
        try {
            Bytes value = indexedInfoFields.getByIndex(indexOfType);
            if (value == null) {
                return null;
            }
            typeSign = SVTypeSign.getByName(indexedInfoFields.getByIndex(indexOfType).detach());
            return typeSign;
        } catch (NumberFormatException e) {
            return (typeSign = null);
        }
    }

    public Bytes getContig2() {
        int indexOfContig2 = getIndexOfContig2();
        if (indexOfContig2 == -1) {
            return null;
        } else {
            return indexedInfoFields.getByIndex(indexOfContig2);
        }
    }

    public List<Bytes> getEssentialValues() {
        essentialValues.clear();
        int dropIndex = 0;
        boolean checked = false;
        boolean drop = !(dropFieldIndex == null||dropFieldIndex.isEmpty());
        for (int i = 0; i < indexedInfoFields.size(); i++) {
            if (drop&& !checked && i == dropFieldIndex.fastGet(dropIndex)) {
                dropIndex++;
                if (dropIndex == dropFieldIndex.size()) {
                    checked = true;
                }
                continue;
            }
            Bytes value = indexedInfoFields.getByIndex(i);
            essentialValues.add(value);
        }
        return essentialValues;
    }

    public int getIndexOfEnd() {
        if (indexOfEnd == -1) {
            indexOfEnd = indexedInfoFields.indexOfKey(END);
        }
        return indexOfEnd;
    }

    public int getIndexOfLen() {
        if (indexOfLen == -1) {
            indexOfLen = indexedInfoFields.indexOfKey(SVLEN);
        }
        return indexOfLen;
    }

    public int getIndexOfType() {
        if (indexOfType == -1) {
            indexOfType = indexedInfoFields.indexOfKey(SVTYPE);
        }
        return indexOfType;
    }

    public int getIndexOfContig2() {
        if (indexOfContig2 == -1) {
            indexOfContig2 = indexedInfoFields.indexOfKey(CONTIG2);
        }
        return indexOfContig2;
    }

    public void clear() {
        initType = false;
        indexedInfoFields.clear();
    }

    public CCFMetaItem save() {
        String[] values = getSavedInfoKeys();
        List<String> validKeys = new List<>();
        for (String value : values) {
            if(value!=null){
                validKeys.add(value);
            }
        }
        return CCFMetaItem.of(name(), validKeys.toArray(new String[0]));
    }

    public static VCFInfoManager load(CCFTable table) {
        if (table == null) {
            return new VCFInfoManager();
        }
        List<CCFMetaItem> ccfMetaItems = table.getMeta().get(name());
        if (ccfMetaItems == null || ccfMetaItems.isEmpty()) {
            return new VCFInfoManager();
        }
        String[] values = (String[]) ccfMetaItems.fastGet(0).getValue();
        List<Bytes> infoFieldSet = new List<>();
        for (String value : values) {
            infoFieldSet.add(new Bytes(value));
        }
        return new VCFInfoManager(infoFieldSet, true);
    }

    public Bytes getAttrValue(Bytes key) {
        return indexedInfoFields.get(key);
    }

    public static String name() {
        return "$INFO";
    }

    public static void addDropField(Bytes dropFieldName) {
        drop_fields.add(dropFieldName);
    }

    public static void addPotentialDropFieldsForCSV() {
        drop_fields.addAll(potentialDropFields);
    }

    public DynamicIndexableMap<Bytes, Bytes> getIndexedInfoFields() {
        return indexedInfoFields;
    }

    private String[] getSavedInfoKeys(){
        int size = indexedInfoFields.size();
        String[] savedInfoKeys = new String[size];
        boolean drop = !(dropFieldIndex == null||dropFieldIndex.isEmpty());
        int currIndexOfDropList = 0, savedIndex = 0;
        for (int i = 0; i < size; i++) {
            if (drop && currIndexOfDropList<dropFieldIndex.size()&&dropFieldIndex.fastGet(currIndexOfDropList) == i){
                currIndexOfDropList++;
                continue;
            }
            savedInfoKeys[savedIndex] = indexedInfoFields.keyOfIndex(i).toString();
            savedIndex++;
        }
        return savedInfoKeys;
    }
}
