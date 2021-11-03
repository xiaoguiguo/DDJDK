package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;

/**
 * StrictMath 类包含用于执行基本数值运算的方法，例如基本指数、对数、平方根和三角函数。
 * strictfp 关键字: strictfp允许程序员强制指定浮点数的运算遵循严格规范。
 *      当运行在不直接支持指令运算的硬件上时，Java会使用软件的形式去进行严格浮点数计算，
 *      这样会大大降低浮点数运行效率，但好处也是显而易见的， 浮点数运算的结果可知可控且各平台一致。
 */
public final class StrictMath {

    private StrictMath() {}

    /** some explanation is in the Math Class */
    public static final double E = 2.7182818284590452354;
    public static final double PI = 3.14159265358979323846;
    private static final double DEGREES_TO_RADIANS = 0.017453292519943295;
    private static final double RADIANS_TO_DEGREES = 57.29577951308232;

    public static native double sin(double a);
    public static native double cos(double a);
    public static native double tan(double a);
    public static native double asin(double a);
    public static native double acos(double a);
    public static native double atan(double a);

    /**
     * 将以度为单位的角度转换为以弧度为单位的近似等效的角度。
     * 从度数到弧度的转换通常是不准确的。
     */
    public static strictfp double toRadians(double angdeg) {
        return angdeg * DEGREES_TO_RADIANS;
    }

    /**
     * 将以弧度为单位的角度转换为以度为单位的近似等效的角度。
     * 从弧度到度的转换通常是不准确的； 用户不应期望 cos(toRadians(90.0)) 恰好等于 0.0。
     */
    public static strictfp double toDegrees(double angrad) {
        return angrad * RADIANS_TO_DEGREES;
    }

    @HotSpotIntrinsicCandidate
    public static native double sqrt(double a);


}
