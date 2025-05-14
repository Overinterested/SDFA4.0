package edu.sysu.pmglab.sdfa;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFTable;
import edu.sysu.pmglab.ccf.field.FieldGroupMeta;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.field.IFieldCollection;
import edu.sysu.pmglab.ccf.loader.CCFChunk;
import edu.sysu.pmglab.ccf.loader.CCFLoader;
import edu.sysu.pmglab.ccf.meta.CCFMeta;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.ccf.meta.ICCFMeta;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.container.indexable.IndexableMap;
import edu.sysu.pmglab.container.indexable.IndexableSet;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.sdfa.base.SDFFormatManager;
import edu.sysu.pmglab.sdfa.base.SDFInfoManager;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFFormatManager;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInfoManager;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

/**
 * @author Wenjie Peng
 * @create 2025-02-23 14:58
 * @description
 */
public class SDFTable extends CCFTable {
    final SVContig contig;
    final FieldGroupMetas fields;
    final SDFInfoManager infoManager;
    final LinkedSet<String> individuals;
    final SDFFormatManager formatManager;

    public static final String SDF_INDIVIDUAL_TAG = "$SDF_INDIVIDUAL";
    private static final LinkedSet<String> EMPTY_INDIVIDUALS = new LinkedSet<>();
    private static final HashSet<CCFChunk.Type> EMPTY_CCF_CHUNK_FOR_RAW = new HashSet<>();

    // parse all
    public SDFTable(Object input) throws IOException {
        this(LiveFile.of(input.toString()));
    }


    public SDFTable(LiveFile file) throws IOException {
        super(file);
        fields = new FieldGroupMetas(super.getAllFields().asUnmodifiable());
        ICCFMeta meta = super.getMeta();
        if (meta.contains(SDF_INDIVIDUAL_TAG)) {
            List<CCFMetaItem> individualMetaItems = meta.get(SDF_INDIVIDUAL_TAG);
            if (individualMetaItems != null && !individualMetaItems.isEmpty()) {
                CCFMetaItem individualMetaItem = individualMetaItems.fastGet(0);
                IndexableSet samples = individualMetaItem.getValue();
                individuals = new LinkedSet<>();
                for (int i = 0; i < samples.size(); i++) {
                    individuals.add((String) samples.valueOf(i));
                }
            } else {
                this.individuals = EMPTY_INDIVIDUALS;
            }
        } else {
            this.individuals = EMPTY_INDIVIDUALS;
        }
        this.contig = SVContig.load(meta);
        this.infoManager = new SDFInfoManager(meta);
        this.formatManager = new SDFFormatManager(meta);
    }


    public SVContig getContig() {
        return contig;
    }


    public void clear() {
        contig.clear();
        individuals.clear();
    }

    public CCFMeta getMetaExceptContig() {
        CCFMeta meta = new CCFMeta();
        meta.add(new CCFMetaItem(SDF_INDIVIDUAL_TAG, FieldType.stringIndexableSet, individuals));
        // info
        meta.add(CCFMetaItem.of(VCFInfoManager.name(), getInfoKeys()));
        // format
        meta.add(CCFMetaItem.of(VCFFormatManager.name(), getFormatNames()));
        return meta;
    }

    @Override
    public CCFMeta getMeta() {
        CCFMeta meta = new CCFMeta();
        List<CCFMetaItem> contigMetas = contig.save();
        for (CCFMetaItem contigMeta : contigMetas) {
            meta.add(contigMeta);
        }
        meta.add(new CCFMetaItem(SDF_INDIVIDUAL_TAG, FieldType.stringIndexableSet, individuals));
        // info
        meta.add(CCFMetaItem.of(VCFInfoManager.name(), getInfoKeys()));
        // format
        meta.add(CCFMetaItem.of(VCFFormatManager.name(), getFormatNames()));
        return meta;
    }

    public SDFInfoManager getInfoManager() {
        return infoManager;
    }

    public LinkedSet<String> getIndividuals() {
        return individuals;
    }

    public SDFFormatManager getFormatManager() {
        return formatManager;
    }

    private String[] getInfoKeys() {
        int count = 0;
        if (infoManager != null) {
            IndexableSet<Bytes> infoKeys = infoManager.getInfoKeys();
            if (infoKeys != null && !infoKeys.isEmpty()) {
                int size = infoKeys.size();
                String[] infoNames = new String[size];
                for (Bytes infoKey : infoKeys) {
                    infoNames[count++] = infoKey.toString();
                }
                return infoNames;
            }
            return new String[0];
        }
        return new String[0];
    }

    private String[] getFormatNames() {
        int count = 0;
        if (formatManager == null) {
            IndexableSet<Bytes> formatAttrNameList = formatManager.getFormatAttrNameList();
            if (formatAttrNameList != null && !formatAttrNameList.isEmpty()) {
                int size = formatAttrNameList.size();
                String[] formatNames = new String[size];
                for (int i = 0; i < size; i++) {
                    formatNames[count++] = formatAttrNameList.valueOf(i).toString();
                }
                return formatNames;
            }
            return new String[0];
        }
        return new String[0];
    }

    @Override
    public IFieldCollection getAllFields() {
        return fields;
    }
}
