package edu.sysu.pmglab.sdfa.mode;

import edu.sysu.pmglab.ccf.field.FieldGroupMeta;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.sdfa.SDFHeader;
import edu.sysu.pmglab.sdfa.SDFTable;

/**
 * @author Wenjie Peng
 * @create 2025-04-27 09:21
 * @description
 */
public interface IReaderMode {
    default int indexOfCoordinate() {
        return -1;
    }

    default int indexOfLength() {
        return -1;
    }

    default int indexOfType() {
        return -1;
    }

    default int indexOfGenotype() {
        return -1;
    }

    default int indexOfQualityMetrics() {
        return -1;
    }

    default int indexOfID() {
        return -1;
    }

    default int indexOfRef() {
        return -1;
    }

    default int indexOfAlt() {
        return -1;
    }

    default int indexOfQual() {
        return -1;
    }

    default int indexOfFilter() {
        return -1;
    }

    default int indexOfInfoField() {
        return -1;
    }

    default int indexOfFileIndex() {
        return -1;
    }

    default int indexOfChrIndexes() {
        return -1;
    }

    default int indexOfAnnotationIndex() {
        return -1;
    }

    FieldGroupMetas getMandatoryFields();
}

class FullMode implements IReaderMode {
    private static final FullMode instance = new FullMode();

    private FullMode() {

    }

    public static final FullMode getInstance() {
        return instance;
    }

    final FieldGroupMetas metas = new FieldGroupMetas()
            .addFields(SDFHeader.LOCATION_GROUP.getMetas())
            .addFields(SDFHeader.GENOTYPE_GROUP.getMetas())
            .addFields(SDFHeader.VCF_FIELD_GROUP.getMetas())
            .addFields(SDFHeader.CSV_INDEX_GROUP.getMetas())
            .addFields(SDFHeader.ANNOTATION_INDEX_GROUP.getMetas());

    @Override
    public FieldGroupMetas getMandatoryFields() {
        return metas;
    }

    public int indexOfCoordinate() {
        return 0;
    }

    public int indexOfLength() {
        return 1;
    }

    public int indexOfType() {
        return 2;
    }

    public int indexOfGenotype() {
        return 3;
    }

    public int indexOfQualityMetrics() {
        return 4;
    }

    public int indexOfID() {
        return 5;
    }

    public int indexOfRef() {
        return 6;
    }

    public int indexOfAlt() {
        return 7;
    }

    public int indexOfQual() {
        return 8;
    }

    public int indexOfFilter() {
        return 9;
    }

    public int indexOfInfoField() {
        return 10;
    }

    public int indexOfFileIndex() {
        return 11;
    }

    public int indexOfChrIndexes() {
        return 12;
    }

    public int indexOfAnnotationIndex() {
        return 13;
    }
}

class CoordinateMode implements IReaderMode {
    private static final CoordinateMode instance = new CoordinateMode();

    final FieldGroupMetas metas = new FieldGroupMetas()
            .addFields(SDFHeader.LOCATION_GROUP.getMetas())
            .addFields(SDFHeader.CSV_INDEX_GROUP.getMetas());

    @Override
    public FieldGroupMetas getMandatoryFields() {
        return metas;
    }

    @Override
    public int indexOfFileIndex() {
        return 3;
    }

    @Override
    public int indexOfChrIndexes() {
        return 4;
    }

    public static CoordinateMode getInstance() {
        return instance;
    }
}

class PlinkMode implements IReaderMode{
    private static final PlinkMode instance = new PlinkMode();
    final FieldGroupMetas metas = new FieldGroupMetas()
            .addFields(SDFHeader.LOCATION_GROUP.getMetas())
            .addField(SDFHeader.GENOTYPE_GROUP.getMetas().getField(0))
            .addFields(SDFHeader.VCF_FIELD_GROUP.getMetas().getField(0))
            .addFields(SDFHeader.VCF_FIELD_GROUP.getMetas().getField(1))
            .addFields(SDFHeader.VCF_FIELD_GROUP.getMetas().getField(2))
            .addFields(SDFHeader.CSV_INDEX_GROUP.getMetas());
    @Override
    public FieldGroupMetas getMandatoryFields() {
        return metas;
    }

    public static PlinkMode getInstance() {
        return instance;
    }

    @Override
    public int indexOfID() {
        return 4;
    }

    @Override
    public int indexOfRef() {
        return 5;
    }

    @Override
    public int indexOfAlt() {
        return 6;
    }

    @Override
    public int indexOfFileIndex() {
        return 7;
    }

    @Override
    public int indexOfChrIndexes() {
        return 8;
    }
}

class MergeMode implements IReaderMode{
    private static final MergeMode instance = new MergeMode();
    final FieldGroupMetas metas = new FieldGroupMetas()
            .addFields(SDFHeader.LOCATION_GROUP.getMetas())
            .addField(SDFHeader.GENOTYPE_GROUP.getMetas().getField(0))
            .addFields(SDFHeader.CSV_INDEX_GROUP.getMetas());
    @Override
    public FieldGroupMetas getMandatoryFields() {
        return metas;
    }

    public static MergeMode getInstance() {
        return instance;
    }

    @Override
    public int indexOfFileIndex() {
        return 4;
    }

    @Override
    public int indexOfChrIndexes() {
        return 5;
    }
}

class AnnotationMode implements IReaderMode{
    private static final AnnotationMode instance = new AnnotationMode();
    final FieldGroupMetas metas = new FieldGroupMetas()
            .addFields(SDFHeader.LOCATION_GROUP.getMetas())
            .addFields(SDFHeader.CSV_INDEX_GROUP.getMetas())
            .addFields(SDFHeader.ANNOTATION_INDEX_GROUP.getMetas());
    @Override
    public FieldGroupMetas getMandatoryFields() {
        return metas;
    }

    public static AnnotationMode getInstance() {
        return instance;
    }

    @Override
    public int indexOfFileIndex() {
        return 3;
    }

    @Override
    public int indexOfChrIndexes() {
        return 4;
    }

    @Override
    public int indexOfAnnotationIndex() {
        return 5;
    }
}

class AnnotationGTMode implements IReaderMode {
    private static final AnnotationGTMode instance = new AnnotationGTMode();
    final FieldGroupMetas metas = new FieldGroupMetas()
            .addFields(SDFHeader.LOCATION_GROUP.getMetas())
            .addField(SDFHeader.GENOTYPE_GROUP.getMetas().getField(0))
            .addFields(SDFHeader.CSV_INDEX_GROUP.getMetas())
            .addFields(SDFHeader.ANNOTATION_INDEX_GROUP.getMetas());
    @Override
    public FieldGroupMetas getMandatoryFields() {
        return metas;
    }

    public static AnnotationGTMode getInstance() {
        return instance;
    }

    @Override
    public int indexOfFileIndex() {
        return 4;
    }

    @Override
    public int indexOfChrIndexes() {
        return 5;
    }

    @Override
    public int indexOfAnnotationIndex() {
        return 6;
    }
}