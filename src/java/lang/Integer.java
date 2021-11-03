package java.lang;

import java.lang.annotation.Native;

/**
 * @className: Integer
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: The Integer class wraps a value of the primitive type int in an object.
 * An object of type Integer contains a single field whose type is int.
 */
public final class Integer extends Number implements Comparable<Integer> {

    /** A constant holding the minimum value an int can have, -2^31. */
    @Native public static final int     MIN_VALUE = 0x80000000;
    /** A constant holding the maximum value an int can have, 2^31-1. */
    @Native public static final int     MAX_VALUE = 0x7fffffff;

    public static final Class<Integer>  TYPE = (Class<Integer>) Class.getPrimitiveClass("int");

    static final char[] digits = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
        'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
    };

    private final int value;

    @Deprecated(since = "9")
    public Integer(int value) {
        this.value = value;
    }

    @Deprecated(since = "9")
    public Integer(String value) {
        this.value = parseInt(value, 10);
    }



    public int compareTo(Integer anotherInteger) {
        return compare(this.value, anotherInteger.value);
    }

    public byte byteValue() {
        return (byte) value;
    }

    public short shortValue() {
        return (short) value;
    }

    public int intValue() {
        return value;
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

    public static int compare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public static int parseInt(String s, int radix) {
        return 0;
    }
}
