package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;

/**
 * @className: Math
 * @author: doudou
 * @datetime: 2021/11/1
 * @description: The class {@code Math} contains methods for performing basic numeric operations such as
 *      the elementary exponential, logarithm, square root, and trigonometric functions.
 *      Math 类包含执行基本数值运算的方法，例如基本指数、对数、平方根和三角函数。
 */
public final class Math {

    /**
     * Don't let anyone instantiate this class.
     */
    private Math() {}

    /**
     * 比任何其他值都更接近 e（自然对数的底）的双精度值。
     */
    public static final double E = 2.7182818284590452354;

    /**
     * 比任何其他值都更接近 pi 的双精度值，即圆的周长与其直径的比值
     */
    public static final double PI = 3.14159265358979323846;

    /**
     * 以度为单位的角度值乘以该常数以获得以弧度为单位的角度值。
     */
    public static final double DEGREES_TO_RADIANS = 0.017453292519943295;

    /**
     * 以弧度为单位的角度值乘以该常数以获得以度为单位的角度值。
     */
    public static final double RADIANS_TO_DEGREES = 57.29577951308232;

    @HotSpotIntrinsicCandidate
    public static double sin(double a) {
        return StrictMath.sin(a);
    }

    @HotSpotIntrinsicCandidate
    public static double cos(double a) {
        return StrictMath.cos(a);
    }

    @HotSpotIntrinsicCandidate
    public static double tan(double a) {
        return StrictMath.tan(a);
    }

    public static double asin(double a) {
        return StrictMath.asin(a);
    }

    public static double acos(double a) {
        return StrictMath.acos(a);
    }

    public static double atan(double a) {
        return StrictMath.atan(a);
    }

    public static double toRadians(double angdeg) {
        return StrictMath.toRadians(angdeg);
    }

    public static double toDegrees(double angrad) {
        return StrictMath.toDegrees(angrad);
    }

    @HotSpotIntrinsicCandidate
    public static int min(int a, int b) {
        return (a <= b) ? a : b;
    }


}
