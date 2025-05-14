package edu.sysu.pmglab.sdfa.sv.vcf.format;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.ccf.type.GenericBox;

/**
 * @author Wenjie Peng
 * @create 2024-12-18 00:05
 * @description
 */
public abstract class FormatAttrBox<V, T extends FormatAttrBox<V, T>> extends GenericBox<V, T> {
    public static Bytes PERIOD = new Bytes(".");
    public abstract String getBriefName();

    /**
     * here we use description from UKBB
     *
     * @return description for current format attribute
     */
    public abstract String getDescription();

    public abstract FieldType getFieldType();

    public abstract int sizeOfItemInEachIndividual();

    protected int getIntByDefault(Bytes value) {
        try {
            return value.toInt();
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    protected float getFloatByDefault(Bytes value) {
        try {
            return value.toFloat();
        } catch (NumberFormatException e) {
            return -1f;
        }
    }

    @Override
    public abstract T newInstance();

    public abstract int get(int sampleIndex, int valueIndex);

    public abstract int get(int sampleIndex);

    public String getStringValue(int sampleIndex) {
        return null;
    }

    public abstract void loadOne(Bytes item);

    public abstract void forceLoadOne(Object item);

    public abstract Object getByIndex(int index);

    public abstract int sizeOfIndividual();

    public abstract void clear();

    public abstract Object getObjectByIndividualIndex(int index);

}
