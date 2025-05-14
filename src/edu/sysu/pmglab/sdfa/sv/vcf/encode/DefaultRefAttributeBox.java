package edu.sysu.pmglab.sdfa.sv.vcf.encode;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.type.basic.BytesBox;
import edu.sysu.pmglab.easytools.encode.ACGTNSeqHandler;

/**
 * @author Wenjie Peng
 * @create 2024-08-30 00:36
 * @description
 */
public class DefaultRefAttributeBox extends BytesBox {
    ByteStream byteStream = new ByteStream();
    private static final byte RAW = (byte) 0;
    private static final byte SEQUENCE = (byte) 1;
    private final ACGTNSeqHandler seqHandler = new ACGTNSeqHandler();

    @Override
    public int encodeTo(ByteStream container) {
        byteStream.clear();
        if (value.length() <= 12) {
            container.putByte(RAW);
            return super.encodeTo(container);
        } else {
            try {
                container.putByte(SEQUENCE);
                return seqHandler.encodeTo(container);
            } catch (UnsupportedOperationException e) {
                container.clear();
                container.putByte(RAW);
                return super.encodeTo(container);
            }
        }
    }

    @Override
    public Bytes encode() {
        byteStream.clear();
        if (value.length() <= 12) {
            byteStream.putByte(RAW);
            byteStream.write(value);
        } else {
            try {
                byteStream.putByte(SEQUENCE);
                byteStream.write(seqHandler.setSeqs(value).encode());
            } catch (UnsupportedOperationException e) {
                byteStream.clear();
                byteStream.putByte(RAW);
                byteStream.write(value);
            }
        }
        return byteStream.toBytes();
    }

    @Override
    public BytesBox decode(Bytes container) {
        if (container.byteAt(0) == RAW) {
            return super.decode(container.subBytes(1));
        } else {
            value = seqHandler.decode(container.subBytes(1));
            return this;
        }
    }

    @Override
    public BytesBox newInstance() {
        return new DefaultRefAttributeBox();
    }
}
