package edu.sysu.pmglab.sdfa.sv.bnd;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
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
 * @description one of four joint bnd types: t[p[, t]p], ]p]t, [p[t
 */
public class JointBNDType extends DetailedBND {
    Bytes t;
    int position;
    String contig;
    BNDFormat jointType;

    @Override
    public Bytes build() {
        ByteStream cache = new ByteStream();
        switch (jointType) {
            case RIGHT_JOIN_AFTER:
                cache.write(t);
                // [
                cache.write(91);
                cache.write(ASCIIUtility.toASCII(contig,Constant.CHAR_SET));
                // :
                cache.write(58);
                cache.write(ASCIIUtility.toASCII(position));
                // [
                cache.write(ASCIIUtility.toASCII(91));
                return cache.toBytes();
            case LEFT_JOIN_BEFORE:
                // ]
                cache.write(93);
                cache.write(ASCIIUtility.toASCII(contig,Constant.CHAR_SET));
                // :
                cache.write(58);
                cache.write(ASCIIUtility.toASCII(position));
                // ]
                cache.write(93);
                return cache.toBytes();
            case REVERSE_LEFT_JOIN_AFTER:
                cache.write(t);
                // ]
                cache.write(93);
                cache.write(ASCIIUtility.toASCII(contig,Constant.CHAR_SET));
                // :
                cache.write(58);
                cache.write(ASCIIUtility.toASCII(position));
                // ]
                cache.write(93);
                return cache.toBytes();
            default:
                // REVERSE_RIGHT_JOIN_BEFORE
                // [
                cache.write(91);
                cache.write(ASCIIUtility.toASCII(contig,Constant.CHAR_SET));
                // :
                cache.write(58);
                cache.write(ASCIIUtility.toASCII(position));
                cache.write(91);
                cache.write(t);
                return cache.toBytes();
        }
    }

    @Override
    public boolean recordOtherBND() {
        return true;
    }

    @Override
    public String getOtherBNDContig() {
        return contig;
    }

    @Override
    public Bytes getT() {
        return t;
    }

    @Override
    public int getOtherBNDPosition() {
        return position;
    }

    @Override
    public void encodeTo(ByteStream cache) {
        DynamicLengthEncoder<BytesBox> encoder = new DynamicLengthEncoder<>();
        BytesBox box = new BytesBox();
        // bnd format
        box.set(Constant.ASCII.fastGet(jointType.getIndex() & 255));
        encoder.encode(box);
        // bnd contig
        box.set(contig);
        encoder.encode(box);
        // bnd position
        box.set(ASCIIUtility.toASCII(position));
        encoder.encode(box);
        // bnd t
        encoder.encode(box.set(t));
        cache.write(encoder.flush());
    }

    public JointBNDType setT(Bytes t) {
        this.t = t;
        return this;
    }

    public JointBNDType setPosition(int position) {
        this.position = position;
        return this;
    }

    public JointBNDType setContig(String contig) {
        this.contig = contig;
        return this;
    }

    public JointBNDType setJointType(BNDFormat jointType) {
        this.jointType = jointType;
        return this;
    }

    @Override
    public JointBNDType decode(Bytes src) {
        LinkedObjectPool boxes = new LinkedObjectPool(BytesBox::new);
        DynamicLengthDecoder.INSTANCE.decodeTo(src, boxes);
        BytesBox format = (BytesBox) boxes.get(0);
        BytesBox contig = (BytesBox) boxes.get(1);
        BytesBox position = (BytesBox) boxes.get(2);
        BytesBox t = (BytesBox) boxes.get(3);

        return new JointBNDType()
                .setJointType(BNDFormat.getInstanceByIndex(format.get().byteAt(0)))
                .setContig(contig.get().toString())
                .setPosition(ASCIIUtility.toInt(position.get().bytes()))
                .setT(t.get());
    }


}
