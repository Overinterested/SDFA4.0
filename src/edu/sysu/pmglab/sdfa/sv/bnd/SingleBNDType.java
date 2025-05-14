package edu.sysu.pmglab.sdfa.sv.bnd;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.type.basic.BytesBox;
import edu.sysu.pmglab.ccf.type.decoder.DynamicLengthDecoder;
import edu.sysu.pmglab.ccf.type.encoder.DynamicLengthEncoder;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.objectpool.LinkedObjectPool;

/**
 * @author Wenjie Peng
 * @create 2024-08-29 06:21
 * @description
 */
public class SingleBNDType extends DetailedBND {
    Bytes value;
    private static final BNDFormat type = BNDFormat.SINGLE_BREAKENDS;

    public SingleBNDType(){

    }

    public SingleBNDType(Bytes value) {
        this.value = value;
    }

    public BNDFormat getType() {
        return type;
    }
    public byte getTypeIndex(){
        return type.getIndex();
    }
    @Override
    public Bytes build() {
        return value;
    }

    @Override
    public void encodeTo(ByteStream cache) {
        BytesBox box = new BytesBox();
        DynamicLengthEncoder<BytesBox> encoder = new DynamicLengthEncoder<>();
        // type index
        encoder.encode(box.set(Constant.ASCII.fastGet(getTypeIndex() & 255)));
        // value
        encoder.encode(box.set(value));
        cache.write(encoder.flush());
    }

    @Override
    public SingleBNDType decode(Bytes src) {
        LinkedObjectPool boxes = new LinkedObjectPool<>(BytesBox::new);
        DynamicLengthDecoder.INSTANCE.decodeTo(src, boxes);
        this.value = ((BytesBox)boxes.get(1)).get();
        return this;
    }

}
