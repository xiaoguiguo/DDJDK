package java.lang;

/**
 * @className: Double
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: The Double class wraps a value of the primitive type double in an object.
 *      An object of type Double contains a single field whose type is double.
 */
public final class Double extends Number implements Comparable<Double> {
    private static final long serialVersionUID = -1239712741922110446L;

    /**
     * 一个常量，保持 double 类型的正无穷大。 它等于 Double.longBitsToDouble(0x7ff0000000000000L) 返回的值。
     */
    public static final double POSITIVE_INFINITY = 1.0 / 0.0;

    /**
     * 一个常量，持有 double 类型的负无穷大。 它等于 Double.longBitsToDouble(0xfff0000000000000L) 返回的值。
     */
    public static final double NEGATIVE_INFINITY = -1.0 / 0.0;

    /**
     * 一个常量，包含一个 double 类型的非数字 (NaN) 值。 它相当于 Double.longBitsToDouble(0x7ff8000000000000L) 返回的值。
     */
    public static final double NaN = 0.0d / 0.0;

    /**
     * 一个常量，它持有 double 类型的最大正有限值，(2-2^-52)·2^1023。
     * 它等于十六进制浮点字面量 0x1.fffffffffffffP+1023，也等于 Double.longBitsToDouble(0x7fefffffffffffffL)。
     * 1.7976931348623157e+308
     */
    public static final double MAX_VALUE = 0x1.fffffffffffffP+1023;

    /**
     * 一个常量，持有 double 类型的最小正正常值，2^-1022。
     * 它等于十六进制浮点字面量 0x1.0p-1022，也等于 Double.longBitsToDouble(0x0010000000000000L)。
     * 2.2250738585072014E-308
     */
    public static final double MIN_NORMAL = 0x1.0p-1022;

    /**
     * 一个常量，包含 double 类型的最小正非零值，2^-1074。
     * 它等于十六进制浮点文字 0x0.0000000000001P-1022，也等于 Double.longBitsToDouble(0x1L)。
     * 4.9e-324
     */
    public static final double MIN_VALUE = 0x0.0000000000001P-1022;
    /**
     * 有限双变量可能具有的最大指数。 它等于 Math.getExponent(Double.MAX_VALUE) 返回的值。
     */
    public static final int MAX_EXPONENT = 1023;

    /**
     * 标准化双变量可能具有的最小指数。 它等于 Math.getExponent(Double.MIN_NORMAL) 返回的值。
     */
    public static final int MIN_EXPONENT = -1022;

    /**
     * 用于表示双精度值的位数。
     */
    public static final int SIZE = 64;

    /**
     * 用于表示双精度值的字节数。
     */
    public static final int BYTES = SIZE / Byte.SIZE;

    public static final Class<Double> TYPE = (Class<Double>) Class.getPrimitiveClass("double");

    private final double value;

    @Deprecated(since="9")
    public Double(double value) {
        this.value = value;
    }

    @Override
    public int compareTo(Double anotherDouble) {
        return compare(this.value, anotherDouble.value);
    }

    @Override
    public int intValue() {
        return 0;
    }

    @Override
    public long longValue() {
        return 0;
    }

    @Override
    public float floatValue() {
        return 0;
    }

    @Override
    public double doubleValue() {
        return 0;
    }

    public static int compare(double d1, double d2) {
        if (d1 < d2) {
            return -1;
        }
        if (d1 > d2) {
            return 1;
        }
        // TODO
        return 0;
    }
}
