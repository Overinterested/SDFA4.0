package edu.sysu.pmglab.sdfa.annotation.source;

import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.ccf.meta.ICCFMeta;
import edu.sysu.pmglab.ccf.type.interval.IntIntervalBox;
import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.indexable.IndexableSet;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.SVContig;

import java.util.*;

/**
 * @author Wenjie Peng
 * @create 2024-09-07 08:54
 * @description
 */
public class SourceMeta {
    LinkedSet<String> columns;
    Map<String, String> properties;
    DynamicIndexableMap<String, IntInterval> contigRanges;

    public SourceMeta() {
        columns = new LinkedSet<>();
        properties = new LinkedHashMap<>();
        contigRanges = new DynamicIndexableMap<>();
    }

    public SourceMeta(DynamicIndexableMap<String, IntInterval> contigRanges) {
        this.contigRanges = contigRanges;
        this.columns = new LinkedSet<>();
    }

    private final IntIntervalBox box = new IntIntervalBox();

    public void buildRanges(IndexableSet<SVContig.Chromosome> contigNames, IntList countList) {
        int fileIndex = 0;
        for (int i = 0; i < contigNames.size(); i++) {
            String name = contigNames.valueOf(i).getName();
            IntInterval range = new IntInterval(fileIndex, fileIndex += countList.get(i));
            contigRanges.put(name, range);
        }
    }

    public List<CCFMetaItem> save() {
        List<CCFMetaItem> metas = new List<>();
        if (properties != null && !properties.isEmpty()) {
            for (Map.Entry<String, String> propertyEntry : properties.entrySet()) {
                metas.add(CCFMetaItem.of(propertyEntry.getKey(), propertyEntry.getValue()));
            }
        }
        if (contigRanges != null && !contigRanges.isEmpty()) {
            LinkedSet<String> chrNameSet = new LinkedSet<>(contigRanges.keySet());
            String[] contigNames = new String[chrNameSet.size()];
            for (int i = 0; i < chrNameSet.size(); i++) {
                contigNames[i] = chrNameSet.valueOf(i);
            }
            metas.add(CCFMetaItem.of("contig", contigNames));
            for (int i = 0; i < chrNameSet.size(); i++) {
                String name = contigRanges.keyOfIndex(i);
                IntInterval range = contigRanges.getByIndex(i);
                metas.add(CCFMetaItem.of(name, box.set(range).toBytes().toString()));
            }
        }
        if (columns != null && !columns.isEmpty()) {
            metas.add(CCFMetaItem.of("columns", columns.toArray(new String[0])));
        }
        return metas;
    }

    public static SourceMeta load(ICCFMeta meta) {
        HashSet<String> loadMetaKey = new HashSet<>();
        SourceMeta sourceMeta = new SourceMeta();
        // contig
        List<CCFMetaItem> contigItems = meta.get("contig");
        loadMetaKey.add("contig");
        if (contigItems != null && !contigItems.isEmpty()) {
            StringArray contigNameSet = contigItems.get(0).getValue();
            DynamicIndexableMap<String, IntInterval> ranges = new DynamicIndexableMap<>(contigNameSet.length());
            for (String contigName : contigNameSet) {
                loadMetaKey.add(contigName);
                String value = meta.get(contigName).get(0).getValue();
                sourceMeta.box.char2Object(value);
                ranges.put(contigName, sourceMeta.box.get());
            }
            sourceMeta.contigRanges = ranges;
        }
        // columns
        List<CCFMetaItem> columnMetas = meta.get("columns");
        if (columnMetas != null && !columnMetas.isEmpty()) {
            loadMetaKey.add("columns");
            StringArray columnNames = columnMetas.get(0).getValue();
            sourceMeta.columns = new LinkedSet<>(columnNames);
        }
        // properties
        Iterator<CCFMetaItem> iterator = meta.iterator();
        HashMap<String, String> properties = new HashMap<>();
        while (iterator.hasNext()) {
            CCFMetaItem next = iterator.next();
            String key = next.getKey();
            if (loadMetaKey.contains(key)) {
                continue;
            }
            String value = (String) next.getValue();
            properties.put(key, value);
        }
        sourceMeta.properties = properties;
        return sourceMeta;
    }

    public SourceMeta setColumns(LinkedSet<String> columns) {
        this.columns = columns;
        return this;
    }

    public SourceMeta bindProperty(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

    public IndexableSet<String> getColumns() {
        return columns;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public IntInterval getRangeByName(String contigName) {
        return contigRanges.get(contigName);
    }

    public int indexOfCol(String col) {
        return columns.indexOf(col);
    }

    public int numOfCols() {
        return columns.size();
    }

    public String nameOfContig(int indexOfContig) {
        return contigRanges.keyOfIndex(indexOfContig);
    }
}
