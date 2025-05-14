package edu.sysu.pmglab.sdfa.sv.bnd;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.sdfa.sv.SVContig;

/**
 * @author Wenjie Peng
 * @create 2024-08-29 06:09
 * @description
 */
public abstract class DetailedBND {
    private static final byte[] LEFT_JOIN = new byte[]{93, 93};
    private static final byte[] RIGHT_JOIN = new byte[]{91, 91};

    /**
     * build raw alt field bytecode
     *
     * @return raw alt
     */
    abstract public Bytes build();

    /**
     * check whether the next bnd can bind current bnd
     *
     * @param next the other bnd
     * @return binding result
     */

    public boolean bind(DetailedBND next) {
        return false;
    }

    public boolean recordOtherBND() {
        return false;
    }

    public String getOtherBNDContig() {
        return null;
    }

    public int getOtherBNDPosition() {
        return -1;
    }

    /**
     * get t when the bnd format is in one of t[p[, t]p], ]p]t, [p[t
     *
     * @return get p
     */
    public Bytes getT() {
        return null;
    }


    abstract public void encodeTo(ByteStream cache);

    abstract public DetailedBND decode(Bytes src);

    public DetailedBND retrieveContigName(SVContig contig) {
        return null;
    }

    public static DetailedBND parse(Bytes alt) {
        int size = alt.length();
        if (size == 5 && alt.startsWith((byte) 60) && alt.endsWith((byte) 62)) {
            return TagBNDType.INSTANCE;
        }
        // check whether complexity
        int indexOfMulti = alt.indexOf((byte) 44);
        if (indexOfMulti != -1) {
            return parseMulti(alt,indexOfMulti);
        }
        if (alt.startsWith((byte) 91)) {
            // REVERSE_RIGHT_JOIN_BEFORE
            int contigEnd = 1;
            int secondIndex = -1;
            for (int i = 1; i < size; i++) {
                if (alt.byteAt(i) == (byte) 91 && secondIndex == -1) {
                    secondIndex = i;
                }
                if (alt.byteAt(i) == (byte) 58) {
                    contigEnd = i;
                }
            }
            return new JointBNDType().setJointType(BNDFormat.REVERSE_RIGHT_JOIN_BEFORE)
                    .setContig(alt.subBytes(1, contigEnd).toString())
                    .setPosition(alt.subBytes(contigEnd + 1, secondIndex).toInt())
                    .setT(alt.subBytes(secondIndex + 1, size));
        } else if (alt.startsWith((byte) 93)) {
            // LEFT_JOIN_BEFORE
            int contigEnd = 1;
            int secondIndex = -1;
            for (int i = 1; i < size; i++) {
                if (alt.byteAt(i) == (byte) 93 && secondIndex == -1) {
                    secondIndex = i;
                }
                if (alt.byteAt(i) == (byte) 58) {
                    contigEnd = i;
                }
            }
            return new JointBNDType().setJointType(BNDFormat.LEFT_JOIN_BEFORE)
                    .setContig(alt.subBytes(1, contigEnd).toString())
                    .setPosition(alt.subBytes(contigEnd + 1, secondIndex).toInt())
                    .setT(alt.subBytes(secondIndex + 1, size));
        } else if (alt.endsWith((byte) 91)) {
            // RIGHT_JOIN_AFTER t[p[
            int contigEnd = 1;
            int firstIndex = -1;
            for (int i = 1; i < size; i++) {
                if (alt.byteAt(i) == (byte) 91 && firstIndex == -1) {
                    firstIndex = i;
                }
                if (alt.byteAt(i) == (byte) 58) {
                    contigEnd = i;
                }
            }
            return new JointBNDType().setJointType(BNDFormat.RIGHT_JOIN_AFTER)
                    .setContig(alt.subBytes(firstIndex + 1, contigEnd).toString())
                    .setPosition(alt.subBytes(contigEnd + 1, size - 1).toInt())
                    .setT(alt.subBytes(0, firstIndex));
        } else if (alt.endsWith((byte) 93)) {
            // REVERSE_LEFT_JOIN_AFTER
            int contigEnd = 1;
            int firstIndex = -1;
            for (int i = 1; i < size; i++) {
                if (alt.byteAt(i) == (byte) 93 && firstIndex == -1) {
                    firstIndex = i;
                }
                if (alt.byteAt(i) == (byte) 58) {
                    contigEnd = i;
                }
            }
            return new JointBNDType().setJointType(BNDFormat.REVERSE_LEFT_JOIN_AFTER)
                    .setContig(alt.subBytes(firstIndex + 1, contigEnd).toString())
                    .setPosition(alt.subBytes(contigEnd + 1, size - 1).toInt())
                    .setT(alt.subBytes(0, firstIndex));
        } else {
            if (alt.startsWith((byte) 46)|| alt.endsWith((byte)46)){
                return new SingleBNDType(alt);
            }
            return new UnknownBNDType(alt);
        }

    }

    private static MixtureBNDType parseMulti(Bytes alt, int indexOfMulti) {
        return new MixtureBNDType().add(parse(alt.subBytes(0, indexOfMulti)))
                .add(parse(alt.subBytes(indexOfMulti + 1, alt.length())));
    }

    @Override
    public String toString() {
        return build().toString();
    }

    public static DetailedBND getInstance(Bytes src){
        byte type = src.byteAt(1);
        return BNDFormat.createInstance(type);
    }
}
