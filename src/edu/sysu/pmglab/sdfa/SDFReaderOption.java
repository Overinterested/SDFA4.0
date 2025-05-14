package edu.sysu.pmglab.sdfa;

import edu.sysu.pmglab.ccf.ReaderOption;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.loader.CCFChunk;
import edu.sysu.pmglab.ccf.loader.CCFLoader;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Wenjie Peng
 * @create 2025-02-24 02:51
 * @description
 */
public class SDFReaderOption extends ReaderOption {
    private final SDFTable sdfTable;
    private SDFReadType readerMode;
    private final FieldGroupMetas mandatoryFields;
    public final static Set<CCFChunk.Type> CCF_READER_FIELD = new HashSet<>();

    static {
        CCF_READER_FIELD.add(CCFChunk.Type.FIELD_GROUP_DATA);
        CCF_READER_FIELD.add(CCFChunk.Type.FIELD_GROUP_META);
    }

    public final static CCFLoader DATA_LOADER = new CCFLoader(CCF_READER_FIELD);


    public SDFReaderOption(Object file, SDFReadType readerMode) throws IOException {
        super(new SDFTable(file), readerMode == null ? SDFReadType.FULL.getReaderMode().getMandatoryFields() : readerMode.getReaderMode().getMandatoryFields());
        sdfTable = (SDFTable) super.getTable();
        this.readerMode = readerMode == null ? SDFReadType.FULL : readerMode;
        this.mandatoryFields = this.readerMode.getReaderMode().getMandatoryFields();
    }

    public FieldGroupMetas getMandatoryFields() {
        return mandatoryFields;
    }

    public SDFReadType getReaderMode() {
        return readerMode;
    }

    public SDFTable getSDFTable() {
        return sdfTable;
    }

    public void clear() {
        sdfTable.clear();
    }


}
