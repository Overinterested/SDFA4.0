package edu.sysu.pmglab.easytools;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.List;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Constant {
    public static final byte LEFT_BRACE = 123;
    public static final byte RIGHT_BRACE = 125;
    public static final byte LESS_THAN_SIGN = 60;
    public static final byte GREATER_THAN_SIGN = 62;
    public static final byte DOUBLE_QUOTES = 34;
    public static final byte SINGLE_QUOTES = 39;
    public static final byte TAB = 9;
    public static final byte NEWLINE = 10;
    public static final byte CARRIAGE_RETURN = 13;
    public static final byte BLANK = 32;
    public static final byte SLASH = 47;
    public static final byte BACKSLASH = 92;
    public static final byte VERTICAL_BAR = 124;
    public static final byte UNDERLINE = 95;
    public static final byte TILDE = 126;
    public static final byte STAR = 42;
    public static final byte COMMA = 44;
    public static final byte PERIOD = 46;
    public static final byte SEMICOLON = 59;
    public static final byte EQUAL = 61;
    public static final byte ADD = 43;
    public static final byte MINUS = 45;
    public static final byte COLON = 58;
    public static final byte NUMBER_SIGN = 35;
    public static final byte DOLLAR = 36;
    public static final byte ZERO = 48;
    public static final byte ONE = 49;
    public static final byte TWO = 50;
    public static final byte THREE = 51;
    public static final byte FOUR = 52;
    public static final byte FIVE = 53;
    public static final byte SIX = 54;
    public static final byte SEVEN = 55;
    public static final byte EIGHT = 56;
    public static final byte NINE = 57;
    public static final byte[] NUMBER = new byte[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57};
    public static final byte A = 65;
    public static final byte B = 66;
    public static final byte C = 67;
    public static final byte D = 68;
    public static final byte E = 69;
    public static final byte F = 70;
    public static final byte G = 71;
    public static final byte H = 72;
    public static final byte I = 73;
    public static final byte J = 74;
    public static final byte K = 75;
    public static final byte L = 76;
    public static final byte M = 77;
    public static final byte N = 78;
    public static final byte O = 79;
    public static final byte P = 80;
    public static final byte Q = 81;
    public static final byte R = 82;
    public static final byte S = 83;
    public static final byte T = 84;
    public static final byte U = 85;
    public static final byte V = 86;
    public static final byte W = 87;
    public static final byte X = 88;
    public static final byte Y = 89;
    public static final byte Z = 90;
    public static final byte a = 97;
    public static final byte b = 98;
    public static final byte c = 99;
    public static final byte d = 100;
    public static final byte e = 101;
    public static final byte f = 102;
    public static final byte g = 103;
    public static final byte h = 104;
    public static final byte i = 105;
    public static final byte j = 106;
    public static final byte k = 107;
    public static final byte l = 108;
    public static final byte m = 109;
    public static final byte n = 110;
    public static final byte o = 111;
    public static final byte p = 112;
    public static final byte q = 113;
    public static final byte r = 114;
    public static final byte s = 115;
    public static final byte t = 116;
    public static final byte u = 117;
    public static final byte v = 118;
    public static final byte w = 119;
    public static final byte x = 120;
    public static final byte y = 121;
    public static final byte z = 122;
    public static final Bytes EMPTY = new Bytes();
    public static final Bytes TRUE = new Bytes(new byte[]{116, 114, 117, 101});
    public static final Bytes FALSE = new Bytes(new byte[]{102, 97, 108, 115, 101});
    public static final Bytes NaN = new Bytes(new byte[]{78, 97, 78});
    public static final Bytes Na = new Bytes(new byte[]{78, 97});
    public static final Bytes NA = new Bytes(new byte[]{78, 65});
    public static final Bytes BYTES_PERIOD = new Bytes(new byte[]{46});
    public static final Bytes NULL = new Bytes("null");
    public static final byte AT = 64;
    public static final Bytes DOUBLE_NUMBER_SIGN = new Bytes("##");
    public static final List<Bytes> ASCII = (new List<Bytes>(256) {
        {
            for(int i = 0; i < 256; ++i) {
                this.add(new Bytes(new byte[]{(byte)i}));
            }

        }
    }).asUnmodifiable();
    public static final Charset CHAR_SET = StandardCharsets.UTF_8;
    private Constant() {
        throw new UnsupportedOperationException("Cannot instantiate CCFConfig");
    }

    public static final Bytes CODING = new Bytes("coding");
    public static final Bytes NON_CODING = new Bytes("noncoding");
}
