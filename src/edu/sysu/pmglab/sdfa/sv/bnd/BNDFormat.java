package edu.sysu.pmglab.sdfa.sv.bnd;

/**
 * @author Wenjie Peng
 * @create 2024-08-29 05:20
 * @description
 */
public enum BNDFormat {
    TYPE_TAG("<BND>: Usage of the type name.", (byte) 0),
    RIGHT_JOIN_AFTER("t[p[: Piece extending to the right of p is joined after t.", (byte) 1),
    LEFT_JOIN_BEFORE("]p]t: Piece extending to the left of p is joined before t.", (byte) 2),
    REVERSE_LEFT_JOIN_AFTER("t]p]: Reverse comp piece extending left of p is joined after t.", (byte) 3),
    REVERSE_RIGHT_JOIN_BEFORE("[p[t: Reverse comp piece extending right of p is joined before t.", (byte) 4),
    SINGLE_BREAKENDS("\".<Seq>\" or \"<Seq>.\": Definition of a breakend that is not part of a novel adjacency.", (byte) 5),
    MULTI_TYPE("A mixture of many types", (byte) 6),
    UNKNOWN("Unknown type", (byte) 7);

    final private byte index;
    final private String fullDescription;

    BNDFormat(String fullDescription, byte index) {
        this.index = index;
        this.fullDescription = fullDescription;
    }

    public byte getIndex() {
        return index;
    }

    public static BNDFormat getInstanceByIndex(byte index) {
        switch (index) {
            case (byte) 0:
                return TYPE_TAG;
            case (byte) 1:
                return RIGHT_JOIN_AFTER;
            case (byte) 2:
                return LEFT_JOIN_BEFORE;
            case (byte) 3:
                return REVERSE_LEFT_JOIN_AFTER;
            case (byte) 4:
                return REVERSE_RIGHT_JOIN_BEFORE;
            case (byte) 5:
                return SINGLE_BREAKENDS;
            default:
                return UNKNOWN;
        }
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public static DetailedBND createInstance(byte index){
        switch (index) {
            case (byte) 0:
                return TagBNDType.INSTANCE;
            case (byte) 1:
                return new JointBNDType().setJointType(RIGHT_JOIN_AFTER);
            case (byte) 2:
                return new JointBNDType().setJointType(LEFT_JOIN_BEFORE);
            case (byte) 3:
                return new JointBNDType().setJointType(REVERSE_LEFT_JOIN_AFTER);
            case (byte) 4:
                return new JointBNDType().setJointType(REVERSE_RIGHT_JOIN_BEFORE);
            case (byte) 5:
                return new SingleBNDType();
            default:
                return new UnknownBNDType();
        }
    }

}
