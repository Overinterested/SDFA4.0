package edu.sysu.pmglab.sdfa.sv.vcf.format;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.ccf.type.list.Int32ListBox;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.easytools.Constant;

import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2024-12-19 19:49
 * @description
 */
public abstract class TwoIntValueBox extends FormatAttrBox<IntList, TwoIntValueBox> {
    private Int32ListBox container = new Int32ListBox();
    private BytesSplitter splitter = new BytesSplitter(Constant.COMMA);

    public TwoIntValueBox() {
        value = new IntList();
    }

    @Override
    public abstract String getBriefName();

    @Override
    public abstract String getDescription();

    @Override
    public FieldType getFieldType() {
        return FieldType.int32List;
    }

    @Override
    public int sizeOfItemInEachIndividual() {
        return 2;
    }

    @Override
    public abstract TwoIntValueBox newInstance();

    @Override
    public TwoIntValueBox char2Object(String s) {
        if (value == null) {
            value = new IntList();
        } else {
            value.clear();
        }
        Bytes var = new Bytes(s);
        Iterator<Bytes> iterator = var.split(Constant.SEMICOLON);
        while (iterator.hasNext()) {
            Bytes item = iterator.next();
            Iterator<Bytes> iterator1 = item.split(Constant.COMMA);
            int count = 0;
            while (iterator1.hasNext()) {
                Bytes next = iterator1.next();
                value.add(next.startsWith(Constant.PERIOD) ? -1 : getIntByDefault(next));
                count++;
                if (count >= 3) {
                    throw new UnsupportedOperationException("Nonstandard AD format in VCF.");
                }
            }
        }
        container.set(value);
        return this;
    }

    @Override
    public Bytes encode() {
        if (value == null || value.size() == 0) {
            return Bytes.EMPTY;
        }
        Bytes res = container.set(value).encode();
        value.clear();
        return res;
    }

    @Override
    public TwoIntValueBox decode(Bytes iByteCode) {
        if (iByteCode.equals(Bytes.EMPTY)) {
            return null;
        }
        container.decode(iByteCode);
        value = container.get();
        return this;
    }

    @Override
    public TwoIntValueBox char2Object(Bytes bytes, boolean b) {
        return decode(bytes);
    }

    @Override
    public Bytes toBytes() {
        ByteStream cache = new ByteStream();
        if (value == null) {
            value = new IntList();
        } else {
            value.clear();
        }
        int size = value.size();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                if (i % 2 == 0) {
                    cache.write(Constant.SEMICOLON);
                } else {
                    cache.write(Constant.COMMA);
                }
            }
            int tmp = value.fastGet(i);
            if (tmp != -1) {
                cache.putVarInt32(tmp);
            } else {
                cache.write(Constant.PERIOD);
            }
            if (i != size - 1) {
                cache.write(Constant.COMMA);
            }
        }
        cache.write(Constant.SEMICOLON);
        return cache.toBytes();
    }

    public int size() {
        return value.size() / 2;
    }

    @Override
    public int get(int sampleIndex, int valueIndex) {
        return value.fastGet(sampleIndex * 2 + valueIndex);
    }

    @Override
    public int get(int sampleIndex) {
        throw new UnsupportedOperationException(getBriefName() + " must assign the value index, only sample index can't obtain the all values");
    }

    @Override
    public void loadOne(Bytes item) {
        splitter.init(item.trim());
        while (splitter.hasNext()) {
            try {
                value.add(splitter.next().toInt());
            } catch (NumberFormatException e) {
                value.add(-1);
            }
        }
    }

    @Override
    public void forceLoadOne(Object item) {
        value.add((int) item);
    }

    @Override
    public Object getByIndex(int index) {
        return value.fastGet(index);
    }

    @Override
    public int sizeOfIndividual() {
        return value.size() / 2;
    }

    @Override
    public void clear() {
        value.clear();
    }

    @Override
    public Object getObjectByIndividualIndex(int index) {
        return IntList.wrap(
                value.fastGet(2 * index),
                value.fastGet(2 * index + 1)
        );
    }
}
