package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;

/**
 * @className: Short
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: The Short class wraps a value of primitive type short in an object.
 *      An object of type Short contains a single field whose type is short.
 */
public final class Short extends Number implements Comparable<Short> {
    private static final long serialVersionUID = 1808410249810397195L;

    /**
     * A constant holding the minimum value a short can have, -2^15.
     */
    public static final short MIN_VALUE = -32768;
    /**
     * A constant holding the maximum value a short can have, 2^15-1.
     */
    public static final short MAX_VALUE = 32767;

    public static final Class<Short> TYPE = (Class<Short>) Class.getPrimitiveClass("short");

    public final short value;

    @Deprecated(since="9")
    public Short(short value) {
        this.value = value;
    }

    @Deprecated(since="9")
    public Short(String s) throws NumberFormatException {
        this.value = parseShort(s, 10);
    }

    public static short parseShort(String s, int radix) {
        int i = Integer.parseInt(s, radix);
        if (i < MIN_VALUE || i > MAX_VALUE) {
            throw new NumberFormatException("Value out of range. Value:\"" + s + "\" Radix:" + radix);
        }
        return (short) i;
    }

    public static short parseShort(String s) throws NumberFormatException {
        return parseShort(s, 10);
    }

    @Override
    public int compareTo(Short anotherShort) {
        return compare(this.value, anotherShort.value);
    }

    public byte byteValue() {
        return (byte)value;
    }

    @HotSpotIntrinsicCandidate
    public short shortValue() {
        return value;
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

    public static int compare(short x, short y) {
        return x - y;
    }
}
