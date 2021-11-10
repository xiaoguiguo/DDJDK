/*
 * Copyright (c) 1994, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang;

import jdk.internal.math.FloatingDecimal;
import jdk.internal.math.DoubleConsts;
import jdk.internal.HotSpotIntrinsicCandidate;

/**
 * double基本类型的封装类
 */
public final class Double extends Number implements Comparable<Double> {
    /**
     * 保持 double类型的正无穷大的 double 。
     */
    public static final double POSITIVE_INFINITY = 1.0 / 0.0;

    /**
     * 持有 double类型的负无穷大的 double 。
     */
    public static final double NEGATIVE_INFINITY = -1.0 / 0.0;

    /**
     * 保持类型为 double非数字（NaN）值的 double 。
     */
    public static final double NaN = 0.0d / 0.0;

    /**
     * 持有 double类型的最大正有限值的 double ，（2-2^-52 ）·2^1023 。
     */
    public static final double MAX_VALUE = 0x1.fffffffffffffP+1023; // 1.7976931348623157e+308

    /**
     * 保持 double^-1022类型的最小正正常值的常量。
     */
    public static final double MIN_NORMAL = 0x1.0p-1022; // 2.2250738585072014E-308

    /**
     * 保持 double类型的最小正非零值的 常量 。
     */
    public static final double MIN_VALUE = 0x0.0000000000001P-1022; // 4.9e-324

    /**
     * 最大指数有限 double变量可能有。
     */
    public static final int MAX_EXPONENT = 1023;

    /**
     * 标准化 double变量的最小指数可能有。
     */
    public static final int MIN_EXPONENT = -1022;

    /**
     * 用于表示 double值的位数。
     */
    public static final int SIZE = 64;

    /**
     * 用于表示 double值的位数。The number of bytes used to represent a {@code double} value.
     *
     * @since 1.8
     */
    public static final int BYTES = SIZE / Byte.SIZE;

    /**
     * 基本类型double的Class实例
     */
    @SuppressWarnings("unchecked")
    public static final Class<Double>   TYPE = (Class<Double>) Class.getPrimitiveClass("double");

    /**
     * 指定double的字符串表示
     */
    public static String toString(double d) {
        return FloatingDecimal.toJavaFormatString(d);
    }

    /**
     * 返回 指定double参数的十六进制字符串表示形式。
     */
    public static String toHexString(double d) {
        /*
         * Modeled after the "a" conversion specifier in C99, section
         * 7.19.6.1; however, the output of this method is more
         * tightly specified.
         */
        if (!isFinite(d) )
            // For infinity and NaN, use the decimal output.
            return Double.toString(d);
        else {
            // Initialized to maximum size of output.
            StringBuilder answer = new StringBuilder(24);

            if (Math.copySign(1.0, d) == -1.0)    // value is negative,
                answer.append("-");                  // so append sign info

            answer.append("0x");

            d = Math.abs(d);

            if(d == 0.0) {
                answer.append("0.0p0");
            } else {
                boolean subnormal = (d < Double.MIN_NORMAL);

                // Isolate significand bits and OR in a high-order bit
                // so that the string representation has a known
                // length.
                long signifBits = (Double.doubleToLongBits(d)
                                   & DoubleConsts.SIGNIF_BIT_MASK) |
                    0x1000000000000000L;

                // Subnormal values have a 0 implicit bit; normal
                // values have a 1 implicit bit.
                answer.append(subnormal ? "0." : "1.");

                // Isolate the low-order 13 digits of the hex
                // representation.  If all the digits are zero,
                // replace with a single 0; otherwise, remove all
                // trailing zeros.
                String signif = Long.toHexString(signifBits).substring(3,16);
                answer.append(signif.equals("0000000000000") ? // 13 zeros
                              "0":
                              signif.replaceFirst("0{1,12}$", ""));

                answer.append('p');
                // If the value is subnormal, use the E_min exponent
                // value for double; otherwise, extract and report d's
                // exponent (the representation of a subnormal uses
                // E_min -1).
                answer.append(subnormal ?
                              Double.MIN_EXPONENT:
                              Math.getExponent(d));
            }
            return answer.toString();
        }
    }

    /**
     * 返回 Double对象，其中 double由参数字符串 s表示的 double值。
     */
    public static Double valueOf(String s) throws NumberFormatException {
        return new Double(parseDouble(s));
    }

    /**
     * 返回表示指定的 double值的 Double实例。
     */
    @HotSpotIntrinsicCandidate
    public static Double valueOf(double d) {
        return new Double(d);
    }

    /**
     * 返回一个新 double初始化为指定的代表的值 String ，如通过执行 valueOf类的方法 Double 。
     */
    public static double parseDouble(String s) throws NumberFormatException {
        return FloatingDecimal.parseDouble(s);
    }

    /**
     * 返回 true如果此 Double值是不是非数字（NAN）， false否则。
     */
    public static boolean isNaN(double v) {
        return (v != v);
    }

    /**
     * 返回 true如果指定的数是无限大， false否则。
     */
    public static boolean isInfinite(double v) {
        return (v == POSITIVE_INFINITY) || (v == NEGATIVE_INFINITY);
    }

    /**
     * 如果参数是有限浮点值，则返回true ; 否则返回false （对于NaN和无穷大参数）。
     */
    public static boolean isFinite(double d) {
        return Math.abs(d) <= Double.MAX_VALUE;
    }

    /**
     * 当前Double的值
     */
    private final double value;

    /**
     * 构造函数
     */
    @Deprecated(since="9")
    public Double(double value) {
        this.value = value;
    }

    /**
     * 构造函数
     */
    @Deprecated(since="9")
    public Double(String s) throws NumberFormatException {
        value = parseDouble(s);
    }

    /**
     * 返回 true如果此 Double值是不是非数字（NAN）， false否则。
     */
    public boolean isNaN() {
        return isNaN(value);
    }

    /**
     * 返回 true如果此 Double值是无限大， false否则。
     */
    public boolean isInfinite() {
        return isInfinite(value);
    }

    /**
     * 返回此 Double对象的字符串表示形式。
     */
    public String toString() {
        return toString(value);
    }

    /**
     * 返回此Double对象的byte表示
     */
    public byte byteValue() {
        return (byte)value;
    }

    /**
     * 返回此Double对象的short表示
     */
    public short shortValue() {
        return (short)value;
    }

    /**
     * 返回此Double对象的int表示
     */
    public int intValue() {
        return (int)value;
    }

    /**
     * 返回此Double对象的long表示
     */
    public long longValue() {
        return (long)value;
    }

    /**
     * 返回此Double对象的float表示
     */
    public float floatValue() {
        return (float)value;
    }

    /**
     * 返回此Double对象的值
     */
    @HotSpotIntrinsicCandidate
    public double doubleValue() {
        return value;
    }

    /**
     * 返回此对象的hashcode
     */
    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    /**
     * 返回指定的double数据的hashcode值
     */
    public static int hashCode(double value) {
        long bits = doubleToLongBits(value);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * 将此对象与指定的对象进行比较是否相等
     */
    public boolean equals(Object obj) {
        return (obj instanceof Double)
               && (doubleToLongBits(((Double)obj).value) ==
                      doubleToLongBits(value));
    }

    /**
     * 返回与给定位表示相对应的 double值。
     */
    @HotSpotIntrinsicCandidate
    public static long doubleToLongBits(double value) {
        if (!isNaN(value)) {
            return doubleToRawLongBits(value);
        }
        return 0x7ff8000000000000L;
    }

    /**
     * 根据IEEE 754浮点“双格式”位布局返回指定浮点值的表示，保留非数字（NaN）值。
     */
    @HotSpotIntrinsicCandidate
    public static native long doubleToRawLongBits(double value);

    /**
     * 返回与给定位表示相对应的 double值。
     */
    @HotSpotIntrinsicCandidate
    public static native double longBitsToDouble(long bits);

    /**
     * 与此对象进行比较大小
     */
    public int compareTo(Double anotherDouble) {
        return Double.compare(value, anotherDouble.value);
    }

    /**
     * 比较两个double值的大小
     */
    public static int compare(double d1, double d2) {
        if (d1 < d2)
            return -1;           // Neither val is NaN, thisVal is smaller
        if (d1 > d2)
            return 1;            // Neither val is NaN, thisVal is larger

        // Cannot use doubleToRawLongBits because of possibility of NaNs.
        long thisBits    = Double.doubleToLongBits(d1);
        long anotherBits = Double.doubleToLongBits(d2);

        return (thisBits == anotherBits ?  0 : // Values are equal
                (thisBits < anotherBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
                 1));                          // (0.0, -0.0) or (NaN, !NaN)
    }

    /**
     * 求和
     */
    public static double sum(double a, double b) {
        return a + b;
    }

    /**
     * 取最大值
     */
    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    /**
     * 取最小值
     */
    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -9172774392245257468L;
}
