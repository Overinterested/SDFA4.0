package edu.sysu.pmglab.sdfa;

import edu.sysu.pmglab.ccf.field.FieldGroupMeta;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.ccf.type.IFieldType;

/**
 * @author Wenjie Peng
 * @create 2025-04-27 08:43
 * @description
 */
public enum SDFHeader {
    LOCATION_GROUP(
            "LOCATION",
            new String[]{"coordinate", "length", "type"},
            new FieldType[]{FieldType.int32List, FieldType.int32, FieldType.int32}
    ),
    GENOTYPE_GROUP(
            "GENOTYPE",
            new String[]{"genotype", "metrics"},
            new FieldType[]{FieldType.bytecode, FieldType.bytecodeList}
    ),
    VCF_FIELD_GROUP(
            "VCF_FIELD",
            new String[]{"id", "ref", "alt", "qual", "filter", "info"},
            new FieldType[]{FieldType.bytecode, FieldType.bytecode, FieldType.bytecode, FieldType.bytecode, FieldType.bytecode, FieldType.bytecodeList}
    ),
    CSV_INDEX_GROUP(
            "CSV_LOCATION",
            new String[]{"line", "chr"},
            new FieldType[]{FieldType.varInt32, FieldType.int32List}
    ),
    ANNOTATION_INDEX_GROUP(
            "ANNOTATION_INDEX",
            new String[]{"indexes"},
            new FieldType[]{FieldType.int32List}
    );

    String group;
    String[] groupNames;
    FieldGroupMeta metas;
    FieldType[] groupFields;

    SDFHeader(String group, String[] groupNames, FieldType[] groupFields) {
        this.group = group;
        this.groupNames = groupNames;
        this.groupFields = groupFields;
        FieldGroupMeta fieldMetas = new FieldGroupMeta(group);
        for (int i = 0; i < groupNames.length; i++) {
            fieldMetas.addField(groupNames[i], groupFields[i]);
        }
        this.metas = fieldMetas;
    }

    public FieldGroupMeta getMetas() {
        return metas;
    }
}
