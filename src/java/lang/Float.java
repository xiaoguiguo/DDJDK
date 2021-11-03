package java.lang;

/**
 * @className: Float
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: The Float class wraps a value of primitive type float in an object.
 *      An object of type Float contains a single field whose type is float.
 */
public final class Float extends Number implements Comparable<Float> {
    private static final long serialVersionUID = 6467504598204157122L;

    public static final float POSITIVE_INFINITY = 1.0f / 0.0f;

    public static final Class<Float> TYPE = (Class<Float>) Class.getPrimitiveClass("float");

    private final float value;

    @Deprecated(since="9")
    public Float(float value) {
        this.value = value;
    }

    @Deprecated(since="9")
    public Float(double value) {
        this.value = (float)value;
    }

    public int compareTo(Float anotherFloat) {
        return compare(this.value, anotherFloat.value);
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
        return (long) value;
    }

    public float floatValue() {
        return value;
    }

    public double doubleValue() {
        return (double) value;
    }

    public static int compare(float f1, float f2) {
        if (f1 < f2) {
            return -1;
        }
        if (f1 > f2) {
            return 1;
        }
        // TODo
        return 0;
    }
}
