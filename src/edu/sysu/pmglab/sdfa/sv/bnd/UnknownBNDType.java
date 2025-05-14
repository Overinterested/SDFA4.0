package edu.sysu.pmglab.sdfa.sv.bnd;

import edu.sysu.pmglab.bytecode.Bytes;

/**
 * @author Wenjie Peng
 * @create 2024-08-29 20:47
 * @description
 */
public class UnknownBNDType extends SingleBNDType {
    private static final BNDFormat type = BNDFormat.UNKNOWN;

    public UnknownBNDType() {

    }

    public UnknownBNDType(Bytes value) {
        super(value);
    }

    @Override
    public BNDFormat getType() {
        return type;
    }

    @Override
    public byte getTypeIndex() {
        return type.getIndex();
    }
}
