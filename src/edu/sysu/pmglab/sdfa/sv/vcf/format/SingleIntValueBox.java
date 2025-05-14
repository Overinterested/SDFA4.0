package edu.sysu.pmglab.sdfa.sv.vcf.format;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.ccf.type.list.Int32ListBox;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.easytools.Constant;

import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2024-12-18 00:09
 * @description
 */
public abstract class SingleIntValueBox extends FormatAttrBox<IntList, SingleIntValueBox> {
    private Int32ListBox container = new Int32ListBox();

    public SingleIntValueBox() {
        value = new IntList();
    }

    @Override
    public abstract String getBriefName();

    @Override
    public abstract String getDescription();

    @Override
    public FieldType getFieldType() {
        return FieldType.varInt32;
    }

    @Override
    public int sizeOfItemInEachIndividual() {
        return 1;
    }

    @Override
    public abstract SingleIntValueBox newInstance();

    @Override
    public SingleIntValueBox char2Object(String s) {
        if (value == null) {
            value = new IntList();
        } else {
            value.clear();
        }
        Bytes bytes = new Bytes(s);
        Iterator<Bytes> iterator = bytes.split(Constant.SEMICOLON);
        while (iterator.hasNext()){
            Bytes item = iterator.next();
            if (item.length() == 0) {
                continue;
            }
            if (item.startsWith(Constant.PERIOD)) {
                value.add(-1);
            } else {
                value.add(getIntByDefault(item));
            }
        }
        container.set(value);
        return this;
    }

    @Override
    public Bytes encode() {
        if (value == null||value.size() == 0){
            return Bytes.EMPTY;
        }
        Bytes res = container.set(value).encode();
        value.clear();
        return res;
    }

    @Override
    public SingleIntValueBox decode(Bytes iByteCode) {
        if (iByteCode.equals(Bytes.EMPTY)) {
            return null;
        }
        container.decode(iByteCode);
        value = container.get();
        return this;
    }

    @Override
    public Bytes toBytes() {
        ByteStream cache = new ByteStream();
        int size = value.size();
        for (int i = 0; i < size; i++) {
            cache.putVarInt32(value.fastGet(i));
            cache.write(Constant.SEMICOLON);
        }
        return cache.toBytes();
    }

    @Override
    public int get(int sampleIndex, int valueIndex) {
        return value.fastGet(sampleIndex);
    }

    @Override
    public int get(int sampleIndex) {
        return value.fastGet(sampleIndex);
    }

    @Override
    public void loadOne(Bytes item) {
        if (item.startsWith(Constant.PERIOD)) {
            value.add(-1);
        } else {
            value.add(getIntByDefault(item));
        }
    }

    @Override
    public SingleIntValueBox char2Object(Bytes bytes, boolean b) {
        return decode(bytes);
    }

    @Override
    public void forceLoadOne(Object item) {
        value.add((int)item);
    }

    @Override
    public Object getByIndex(int index) {
        return value.fastGet(index);
    }

    @Override
    public int sizeOfIndividual() {
        return value.size();
    }

    @Override
    public void clear() {
        value.clear();
    }

    @Override
    public Object getObjectByIndividualIndex(int index) {
        return value.get(index);
    }
}
