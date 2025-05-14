package edu.sysu.pmglab.sdfa.sv.bnd;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;

/**
 * @author Wenjie Peng
 * @create 2024-08-29 06:21
 * @description
 */
public class TagBNDType extends DetailedBND {

    private static final BNDFormat type = BNDFormat.TYPE_TAG;
    public static final TagBNDType INSTANCE = new TagBNDType();
    private static final Bytes value = new Bytes("<BND>");

    private TagBNDType() {

    }

    @Override
    public Bytes build() {
        return value;
    }

    @Override
    public void encodeTo(ByteStream cache) {
        cache.putByte(type.getIndex());
    }

    @Override
    public DetailedBND decode(Bytes src) {
        return INSTANCE;
    }
}
