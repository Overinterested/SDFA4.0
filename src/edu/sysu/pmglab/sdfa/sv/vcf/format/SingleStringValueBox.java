package edu.sysu.pmglab.sdfa.sv.vcf.format;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.ccf.type.list.StringListBox;
import edu.sysu.pmglab.container.list.List;


/**
 * @author Wenjie Peng
 * @create 2024-12-19 19:54
 * @description
 */
public abstract class SingleStringValueBox extends FormatAttrBox<List<String>, SingleStringValueBox> {
    private StringListBox container = new StringListBox();
    public SingleStringValueBox() {
        value = new List<>();
    }

    @Override
    public abstract String getBriefName();

    @Override
    public abstract String getDescription();

    @Override
    public FieldType getFieldType() {
        return FieldType.bytecodeList;
    }

    @Override
    public int sizeOfItemInEachIndividual() {
        return 1;
    }

    @Override
    public abstract SingleStringValueBox newInstance();

    @Override
    public SingleStringValueBox char2Object(String s) {
        container.char2Object(s);
        value = container.get();
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
    public SingleStringValueBox decode(Bytes iByteCode) {
        container.decode(iByteCode);
        value = container.get();
        return this;
    }

    @Override
    public Bytes toBytes() {
        return container.toBytes();
    }

    @Override
    public int get(int sampleIndex, int valueIndex) {
        throw new UnsupportedOperationException(getBriefName()+" can't be converted into a integer value");
    }

    @Override
    public int get(int sampleIndex) {
        throw new UnsupportedOperationException(getBriefName()+" can't be converted into a integer value");
    }

    public String getStringValue(int sampleIndex){
        return value.fastGet(sampleIndex).toString();
    }

    @Override
    public void loadOne(Bytes item) {
        value.add(item.toString());
    }

    @Override
    public SingleStringValueBox char2Object(Bytes bytes, boolean b) {
        return decode(bytes);
    }

    public static class SingleStringValueBoxInstance extends SingleStringValueBox{
        String briefName;
        String description;
        @Override
        public String getBriefName() {
            return briefName;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public SingleStringValueBox newInstance() {
            return new SingleStringValueBoxInstance();
        }

        public SingleStringValueBoxInstance setBriefName(String briefName) {
            this.briefName = briefName;
            return this;
        }

        public SingleStringValueBoxInstance setDescription(String description) {
            this.description = description;
            return this;
        }

    }
    @Override
    public Object getByIndex(int index) {
        return value.fastGet(index);
    }
    @Override
    public void forceLoadOne(Object item) {
        value.add((String) item);
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
