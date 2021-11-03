package java.lang;

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

    @Deprecated(since="9")
    public Byte(byte value) {
        this.value = value;
    }

    @Deprecated(since="9")
    public Byte(String s) throws NumberFormatException {
        this.value = parseByte(s, 10);
    }

    public int compareTo(Byte anotherByte) {
        return compare(this.value, anotherByte.value);
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
}
