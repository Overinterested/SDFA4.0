package edu.sysu.pmglab.sdfa.sv.vcf;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.ccf.meta.CCFMeta;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.ccf.type.basic.BytesBox;
import edu.sysu.pmglab.ccf.type.encoder.DynamicLengthEncoder;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.indexable.IndexableSet;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.sdfa.sv.vcf.format.FormatAttrBox;
import edu.sysu.pmglab.sdfa.sv.vcf.format.FormatAttrType;
import edu.sysu.pmglab.sdfa.sv.vcf.format.GTBox;

/**
 * @author Wenjie Peng
 * @create 2024-12-19 20:45
 * @description
 */
public class VCFFormatManager {
    /**
     * store the filter items
     */
    private IndexableSet<Bytes> filterFieldSet;
    /**
     * map different formats to filter fields
     */
    private final List<IntList> formatFilterIndexesMap = new List<>();

    /**
     * store genotypes
     */
    GTBox genotypeBox;

    // store format attribute boxes for each attribute
    private final List<FormatAttrBox> formatAttrBoxList = new List<>();
    // store names of all attributes
    private final LinkedSet<Bytes> formatFieldSet = new LinkedSet<>();
    // store full `FORMAT` format in a VCF
    private final DynamicIndexableMap<Bytes, IntList> formatIndexesOfStorageMap = new DynamicIndexableMap<>();

    // store tmp format attr list
    List<FormatAttrBox> attrBoxListCache = new List<>();
    // encoder to encode format
    private final DynamicLengthEncoder<BytesBox> metricEncoder = new DynamicLengthEncoder<>();

    private List<Bytes> tmpEncodedAttrs = null;

    private VCFFormatManager() {

    }

    private VCFFormatManager(IndexableSet<Bytes> filterFieldSet) {
        this.filterFieldSet = filterFieldSet;
    }

    public VCFFormatManager loadFormats(String... formats) {
        BytesSplitter splitter = new BytesSplitter(Constant.COLON);
        for (String format : formats) {
            Bytes formatInByteCode = new Bytes(format);
            BytesSplitter items = splitter.init(formatInByteCode);
            IntList indexesOfPure = new IntList();
            while (items.hasNext()) {
                Bytes formatItem = items.next();
                int index = formatFieldSet.indexOf(formatItem);
                if (index == -1) {
                    index = formatFieldSet.size() - 1;
                    formatFieldSet.add(formatItem.detach());
                    formatAttrBoxList.add(FormatAttrType.getByName(formatItem.toString()));
                }
                indexesOfPure.add(index);
            }
            formatIndexesOfStorageMap.put(formatInByteCode, indexesOfPure);
        }
        return this;
    }

    public static VCFFormatManager loadFilterFields(IndexableSet<Bytes> formatFilterFieldSet) {
        return new VCFFormatManager(formatFilterFieldSet);

    }

    /**
     * get the storage index of current format
     *
     * @param format search format
     * @return
     */
    public int getFormatIndex(Bytes format) {
        if (format.length() == 0 || format.length() == 1) {
            // invalid `FORMAT` format
            return -1;
        } else {
            // valid format
            int indexOfFilter, indexOfStorage;
            int index = this.formatIndexesOfStorageMap.indexOfKey(format);
            if (index == -1) {
                format = format.detach();
                // format not register
                boolean existFormatFilter = filterFieldSet != null && !filterFieldSet.isEmpty();
                IntList indexesOfFilter = existFormatFilter ? new IntList() : null;

                BytesSplitter splitter = new BytesSplitter(Constant.COLON);
                IntList indexesOfCurrFormatItems = new IntList();
                BytesSplitter items = splitter.init(format);
                while (items.hasNext()) {
                    Bytes formatAttr = items.next();
                    indexOfStorage = loadOneAttr(formatAttr);
                    indexesOfCurrFormatItems.add(indexOfStorage);
                    // build filter list
                    if (existFormatFilter) {
                        indexOfFilter = filterFieldSet.indexOf(formatAttr);
                        indexesOfFilter.add(indexOfFilter);
                    }
                }
                formatIndexesOfStorageMap.put(format, indexesOfCurrFormatItems);
                formatFilterIndexesMap.add(indexesOfFilter);
                return formatIndexesOfStorageMap.size() - 1;
            } else {
                return index;
            }
        }
    }

    public IndexableSet<Bytes> getFormatFieldSet() {
        return formatFieldSet;
    }

    public DynamicIndexableMap<Bytes, IntList> getFormatIndexesOfStorageMap() {
        return formatIndexesOfStorageMap;
    }

    public IntList getIndexesOfStorage(int indexOfFormat) {
        return formatIndexesOfStorageMap.getByIndex(indexOfFormat);
    }

    public IntList getIndexesOfFilter(int indexOfFormat) {
        return formatFilterIndexesMap.fastGet(indexOfFormat);
    }

    public VCFFormatManager addFormatFields(List<Bytes> fields) {
        for (Bytes field : fields) {
            loadOneAttr(field);
            formatFieldSet.add(field);
        }
        return this;
    }

    public static String name() {
        return "$FORMAT";
    }

    /**
     * store the pure format items
     *
     * @return storage for pure
     */
    public CCFMetaItem save() {
        String[] values = new String[formatFieldSet.size() - 1];
        // exclude GT
        for (int i = 0; i < values.length; i++) {
            values[i] = formatFieldSet.valueOf(i + 1).toString();
        }
        return CCFMetaItem.of(name(), values);
    }


    public void clear() {
        this.formatFieldSet.clear();
        this.formatAttrBoxList.clear();
        this.formatIndexesOfStorageMap.clear();
    }

    public static VCFFormatManager load(CCFMeta meta) {
        VCFFormatManager vcfFormatManager = new VCFFormatManager();
        List<CCFMetaItem> ccfMetaItems = meta.get(name());
        if (ccfMetaItems == null || ccfMetaItems.isEmpty()) {
            return vcfFormatManager;
        }
        CCFMetaItem ccfMetaItem = ccfMetaItems.fastGet(0);
        String[] values = ccfMetaItem.getValue();
        for (String value : values) {
            vcfFormatManager.formatFieldSet.add(new Bytes(value));
        }
        return vcfFormatManager;
    }

    /**
     * encode all format values in a SV
     */
    public List<Bytes> encode() {
        int size = formatAttrBoxList.size();
        checkEncodeAttrs(size - 1);
        for (int i = 1; i < size; i++) {
            FormatAttrBox formatAttrBox = formatAttrBoxList.fastGet(i);
            Bytes encode = formatAttrBox.encode();
            tmpEncodedAttrs.set(i - 1, encode);
        }
        return tmpEncodedAttrs;
    }

    public Bytes getEncodeValue(int encodeIndex) {
//        DynamicLengthEncoder<ByteCodeBox> encoder = gtyMetricEncoderList.fastGet(encodeIndex);
//        return encoder.flush();
        return null;
    }

    public int numOfPureGtyFields() {
        return formatFieldSet.size();
    }

    public int indexOfPureGtyField(Bytes field) {
        return formatFieldSet.indexOf(field);
    }

    public List<FormatAttrBox> decode(Bytes encodeFormatAttrs) {

        return null;
    }

    public FormatAttrBox getByIndex(int index) {
        return formatAttrBoxList.fastGet(index);
    }


    public List<FormatAttrBox> getFormatAttrBoxList() {
        return formatAttrBoxList;
    }

    public List<FormatAttrBox> getAttrsBoxListInCache(int formatIndex) {
        IntList indexesOfStorage = getIndexesOfStorage(formatIndex);
        attrBoxListCache.clear();
        for (int i = 0; i < indexesOfStorage.size(); i++) {
            attrBoxListCache.add(formatAttrBoxList.fastGet(indexesOfStorage.fastGet(i)));
        }
        return attrBoxListCache;
    }

    public static void main(String[] args) {
        VCFFormatManager vcfFormatManager = new VCFFormatManager();

    }

    public List<Bytes> getTmpEncodedAttrs() {
        return tmpEncodedAttrs;
    }

    public void checkEncodeAttrs(int size) {
        if (tmpEncodedAttrs == null) {
            tmpEncodedAttrs = new List<>();
            for (int i = 0; i < size; i++) {
                tmpEncodedAttrs.add(Constant.EMPTY);
            }
        } else if (size <= 0) {
            tmpEncodedAttrs = GenotypeConstant.EMPTY_GTY_METRIC;
        } else {
            for (int i = tmpEncodedAttrs.size(); i < size; i++) {
                tmpEncodedAttrs.add(Constant.EMPTY);
            }
        }
    }

    private int loadOneAttr(Bytes attr) {
        int indexOfStorage = formatFieldSet.indexOf(attr);
        if (indexOfStorage == -1) {
            formatFieldSet.add(attr);
            formatAttrBoxList.add(FormatAttrType.getByName(attr.toString()));
            indexOfStorage = formatFieldSet.size() - 1;
        }
        return indexOfStorage;
    }
}
