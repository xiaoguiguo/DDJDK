package java.lang;

import java.lang.annotation.Native;

/**
 * @className: Long
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: The Long class wraps a value of the primitive type long in an object.
 *      An object of type Long contains a single field whose type is long.
 */
public final class Long extends Number implements Comparable<Long> {
    @Native private static final long serialVersionUID = -6603966084627590344L;

    /**
     * A constant holding the minimum value a long can have, -2^63.
     */
    @Native public static final long MIN_VALUE = 0x8000000000000000L;

    /**
     * A constant holding the maximum value a long can have, 2^63-1.
     */
    @Native public static final long MAX_VALUE = 0x7fffffffffffffffL;

    public static final Class<Long> TYPE = (Class<Long>) Class.getPrimitiveClass("long");

    private final long value;

    @Deprecated(since = "9")
    public Long(long value) {
        this.value = value;
    }

    @Deprecated(since = "9")
    public Long(String s) {
        this.value = parseLong(s, 10);
    }

    public int compareTo(Long anotherLong) {
        return compare(this.value, anotherLong.value);
    }

    public byte byteValue() {
        return (byte) value;
    }

    public short shortValue() {
        return (short) value;
    }

    public int intValue() {
        return (int) value;
    }

    public long longValue() {
        return value;
    }

    public float floatValue() {
        return (float) value;
    }

    public double doubleValue() {
        return (double) value;
    }

    public static int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public static long parseLong(String s, int radix) {
        return 0;
    }
}
