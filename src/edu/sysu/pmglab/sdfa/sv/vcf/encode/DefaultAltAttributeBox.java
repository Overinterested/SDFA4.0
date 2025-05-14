package edu.sysu.pmglab.sdfa.sv.vcf.encode;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.type.basic.BytesBox;
import edu.sysu.pmglab.ccf.type.decoder.DynamicLengthDecoder;
import edu.sysu.pmglab.ccf.type.encoder.DynamicLengthEncoder;
import edu.sysu.pmglab.easytools.encode.ACGTNSeqHandler;
import edu.sysu.pmglab.objectpool.LinkedObjectPool;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import edu.sysu.pmglab.sdfa.sv.bnd.DetailedBND;
import edu.sysu.pmglab.sdfa.sv.bnd.UnknownBNDType;

/**
 * @author Wenjie Peng
 * @create 2024-08-28 00:54
 * @description
 */
public class DefaultAltAttributeBox extends BytesBox {
    private final ByteStream cache = new ByteStream();
    private final BytesBox box = new BytesBox();
    private static final byte[] RAW_ENCODE = new byte[]{0, 0};
    private static final byte[] TYPE_ENCODE = new byte[]{0, 1};
    private static final byte[] SEQUENCE_ENCODE = new byte[]{1, 0};
    private static final byte[] BND_ENCODE = new byte[]{1, 1};
    private final ACGTNSeqHandler seqHandler = new ACGTNSeqHandler();
    private final LinkedObjectPool list = new LinkedObjectPool(BytesBox::new);
    DynamicLengthEncoder<BytesBox> encode = new DynamicLengthEncoder<>();

    public DefaultAltAttributeBox() {

    }

    public DefaultAltAttributeBox(Bytes value) {
        this.value = value;
    }

    @Override
    public Bytes encode() {
        encode.flush();
        cache.clear();
        int length = value.length();
        if (length < 5) {
            encode.encode(box.set(RAW_ENCODE));
            encode.encode(box.set(value));
        } else if (length == 5 && value.startsWith((byte) 60)) {
            encode.encode(box.set(TYPE_ENCODE));
            SVTypeSign type = SVTypeSign.getByName(value.subBytes(1, 4));
            if (type != null) {
                int typeIndex = type.getIndex();
                encode.encode(box.set(ASCIIUtility.toASCII(typeIndex)));
//                encode.encode(box.set(new byte[]{
//                        (byte) ((int) ((long) typeIndex & 255L)),
//                        (byte) ((int) ((long) (typeIndex >> 8) & 255L)),
//                        (byte) ((int) ((long) (typeIndex >> 16) & 255L)),
//                        (byte) ((int) ((long) (typeIndex >> 24) & 255L)),
//                }));
            } else {
                encode.flush();
                encode.encode(box.set(RAW_ENCODE));
                encode.encode(box.set(value));
            }
        } else {
            try {
                encode.encode(box.set(SEQUENCE_ENCODE));
                encode.encode(box.set(seqHandler.setSeqs(value).encode()));
            } catch (UnsupportedOperationException e) {
                encode.flush();
                DetailedBND bndFormat = DetailedBND.parse(value);
                if (bndFormat.getClass() != UnknownBNDType.class) {
                    encode.encode(box.set(BND_ENCODE));
                    if (cache.length() == 0) {
                        bndFormat.encodeTo(cache);
                        encode.encode(box.set(cache.toBytes().detach()));
                        cache.clear();
                    } else {
                        ByteStream tmp = new ByteStream();
                        bndFormat.encodeTo(tmp);
                        encode.encode(box.set(tmp.toBytes()));
                    }
                } else {
                    encode.encode(box.set(RAW_ENCODE));
                    encode.encode(box.set(value));
                }
            }
        }
        return encode.flush();
    }

    @Override
    public BytesBox decode(Bytes iByteCode) {
        list.clear();
        DynamicLengthDecoder.INSTANCE.decodeTo(iByteCode, list);
        Bytes type = ((BytesBox) list.get(0)).get();
        Bytes encodeValue = ((BytesBox) list.get(1)).get();
        if (type.startsWith(RAW_ENCODE)){
            this.value = encodeValue;
        }else if (type.startsWith(TYPE_ENCODE)) {
            SVTypeSign typeOfSV = SVTypeSign.getByIndex(ASCIIUtility.toInt(encodeValue.bytes()));
            this.value = new Bytes("<" + typeOfSV.getName() + ">");
        } else if (type.startsWith(SEQUENCE_ENCODE)) {
            this.value = seqHandler.decode(encodeValue);
        } else if (type.startsWith(BND_ENCODE)) {
            this.value = DetailedBND.getInstance(encodeValue).decode(encodeValue).build();
        } else {
            return super.decode(encodeValue);
        }
        return this;
    }

    @Override
    public int encodeTo(ByteStream container) {
        Bytes res = encode();
        return container.write(res);
    }

    @Override
    public DefaultAltAttributeBox newInstance() {
        return new DefaultAltAttributeBox(value);
    }

}
