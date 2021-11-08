package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;

/**
 * @className: Byte
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: The Byte class wraps a value of primitive type byte in an object.
 *      An object of type Byte contains a single field whose type is byte.
 *      In addition, this class provides several methods for converting a byte to a String and a String to a byte,
 *      as well as other constants and methods useful when dealing with a byte.
 */
public final class Byte extends Number implements Comparable<Byte> {

    /** A constant holding the minimum value a byte can have, -2^7. */
    public static final byte MIN_VALUE = -128;
    /** A constant holding the maximum value a byte can have, 2^7-1. */
    public static final byte MAX_VALUE = 127;

    public static final Class<Byte> TYPE = (Class<Byte>) Class.getPrimitiveClass("byte");

    private final byte value;

    private static class ByteCache {
        private ByteCache() {}

        static final Byte[] cache = new Byte[-(-128) + 127 + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new Byte((byte) (i - 128));
            }
        }
    }

    /**
     * 用于以二进制补码形式表示字节值的位数。
     * tag: 好好理解这句话，很重要哦
     */
    public static final int SIZE = 8;

    /**
     * 用于以二进制补码形式表示字节值的字节数。
     */
    public static final int BYTES = SIZE / Byte.SIZE;

    @Deprecated(since="9")
    public Byte(byte value) {
        this.value = value;
    }

    @Deprecated(since="9")
    public Byte(String s) throws NumberFormatException {
        this.value = parseByte(s, 10);
    }

    /**
     * 返回表示指定字节的新 String 对象。 假设基数为 10，即10进制
     */
    public static String toString(byte b) {
        return Integer.toString((int) b, 10);
    }


    @Override
    public int hashCode() {
        return Byte.hashCode(value);
    }

    public static int hashCode(byte value) {
        return (int) value;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Byte) {
            return value == ((Byte) obj).byteValue();
        }
        return false;
    }

    public static Byte valueOf(byte b) {
        final int offset = 128;
        return ByteCache.cache[(int) b + offset];
    }

    public static Byte valueOf(String s, int radix) throws NumberFormatException {
        return valueOf(parseByte(s, radix));
    }

    public static Byte valueOf(String s) throws NumberFormatException {
        return valueOf(s, 10);
    }

    public static Byte decode(String nm) throws NumberFormatException {
        int i = Integer.decode(nm);
        if (i < MIN_VALUE || i > MAX_VALUE) {
            throw new NumberFormatException("Value " + i + " out of range from input " + nm);
        }
        return valueOf((byte) i);
    }

    public int compareTo(Byte anotherByte) {
        return compare(this.value, anotherByte.value);
    }

    @HotSpotIntrinsicCandidate
    public byte byteValue() {
        return value;
    }

    public short shortValue() {
        return (short) value;
    }

    public int intValue() {
        return (int) value;
    }

    public long longValue() {
        return (long) value;
    }

    public float floatValue() {
        return (float) value;
    }

    public double doubleValue() {
        return (double) value;
    }

    public static byte parseByte(String s, int radix) throws NumberFormatException {
        int i = Integer.parseInt(s, radix);
        if (i < MIN_VALUE || i > MAX_VALUE) {
            throw new NumberFormatException("Value out of range. Value:\"" + s + "\" Radix:" + radix);
        }
        return (byte) i;
    }

    public static byte parseByte(String s) throws NumberFormatException {
        return parseByte(s, 10);
    }

    public static int compare(byte x, byte y) {
        return x - y;
    }

    public static int compareUnsigned(byte x, byte y) {
        return Byte.toUnsignedInt(x) - Byte.toUnsignedInt(y);
    }

    /**
     * 无符号字节数据类型的范围从 0 到 255；但是，Java 没有无符号字节。
     * 你可以做的是将字节转换为 int 以生成一个无符号字节，并使用 0xff 掩码（按位）新 int 以获得最后 8 位或防止符号扩展。
     * 当字节值与 & 运算符一起使用时，它会自动将字节转换为整数。0x000000FF 的十六进制值表示为 0xFF。
     * 为什么做 & 0xff操作，可参考：https://www.cnblogs.com/think-in-java/p/5527389.html
     */
    private static int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    /**
     * 通过无符号转换将参数转换为 long。 在对 long 的无符号转换中，long 的高 56 位为零，低 8 位等于 byte 参数的位。
     * 因此，零和正字节值被映射到一个数字相等的 long 值，负字节值被映射到一个等于输入加上 2^8 的 long 值。
     */
    public static long toUnsignedLong(byte x) {
        return ((long) x) & 0xffL;
    }
}
