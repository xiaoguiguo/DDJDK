package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;

import static java.lang.String.*;

/**
 * @className: StringUTF16
 * @author: doudou
 * @datetime: 2021/11/1
 * @description: 双字节 字符串
 */
final class StringUTF16 {

    private static native boolean isBigEndian();

    static final int HI_BYTE_SHIFT;
    static final int LO_BYTE_SHIFT;
    static {
        if (isBigEndian()) {
            HI_BYTE_SHIFT = 8;
            LO_BYTE_SHIFT = 0;
        } else {
            HI_BYTE_SHIFT = 0;
            LO_BYTE_SHIFT = 8;
        }
    }

    /**
     * UTF16编码的字符char转化为byte时将char的低8位和高8位分别拆开来了，
     * 依次按照低位byte在前高位byte在后的顺序进行存放。
     * 所以返回的char[]长度是byte[]的一半。
     *
     * getChar()方法通过一个下标index访问它的2倍与2倍+1，也就是一个char的低位与高位。
     * 低位与高位需要&0xff保持二进制值不变，高位右移指定高位数，最后将高低位按位或"|"组合成一个完整的char。
     *
     * 对于高低位的静态代码块，是根据isBigEndian()方法判断高位与低位哪个在前哪个在后。
     */
    @HotSpotIntrinsicCandidate
    static char getChar(byte[] val, int index) {
        assert index >= 0 && index < length(val) : "Trusted caller missed bounds check";
        index <<= 1;
        return (char)(((val[index++] & 0xff) << HI_BYTE_SHIFT)
                    | ((val[index]  & 0xff) << LO_BYTE_SHIFT));
    }

    public static int length(byte[] value) {
        return value.length >> 1;
    }

    public static char charAt(byte[] value, int index) {
        checkIndex(index, value);
        return getChar(value, index);
    }

    public static void checkIndex(int off, byte[] val) {
        String.checkIndex(off, length(val));
    }

    public static CharSequence newString(byte[] val, int index, int len) {
        if (String.COMPACT_STRINGS) {
            byte[] buf = compress(val, index, len);
            if (buf != null) {
                return new String(buf, LATIN1);
            }
        }
        int last = index + len;
        return new String(Arrays.copyOfRange(val, index << 1, last <<1), UTF16);
    }

    private static byte[] compress(byte[] val, int off, int len) {
        byte[] ret = new byte[len];
        if (compress(val, off, ret, 0, len) == len) {
            return ret;
        }
        return null;
    }

    private static int compress(byte[] src, int srcOff, byte[] dst, int dstOff, int len) {
        checkBoundsOffCount(srcOff, len, src);
        for (int i = 0; i < len; i++) {
            char c = getChar(src, srcOff);
            if (c > 0xFF) {
                len = 0;
                break;
            }
            dst[dstOff] = (byte) c;
            srcOff++;
            dstOff++;
        }
        return len;
    }

    public static void checkBoundsOffCount(int offset, int count, byte[] val) {
        String.checkBoundsOffCount(offset, count, length(val));
    }

}
