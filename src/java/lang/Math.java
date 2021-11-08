package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;
import jdk.internal.math.DoubleConsts;
import jdk.internal.math.FloatConsts;

import java.math.BigDecimal;
import java.util.Random;

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
    public static double exp(double a) {
        return StrictMath.exp(a); // default impl. delegates to StrictMath
    }

    @HotSpotIntrinsicCandidate
    public static double log(double a) {
        return StrictMath.log(a); // default impl. delegates to StrictMath
    }

    @HotSpotIntrinsicCandidate
    public static double log10(double a) {
        return StrictMath.log10(a); // default impl. delegates to StrictMath
    }

    @HotSpotIntrinsicCandidate
    public static double sqrt(double a) {
        return StrictMath.sqrt(a); // default impl. delegates to StrictMath
    }


    public static double cbrt(double a) {
        return StrictMath.cbrt(a);
    }

    public static double IEEEremainder(double f1, double f2) {
        return StrictMath.IEEEremainder(f1, f2); // delegate to StrictMath
    }

    public static double ceil(double a) {
        return StrictMath.ceil(a); // default impl. delegates to StrictMath
    }

    public static double floor(double a) {
        return StrictMath.floor(a); // default impl. delegates to StrictMath
    }

    public static double rint(double a) {
        return StrictMath.rint(a); // default impl. delegates to StrictMath
    }

    @HotSpotIntrinsicCandidate
    public static double atan2(double y, double x) {
        return StrictMath.atan2(y, x); // default impl. delegates to StrictMath
    }

    @HotSpotIntrinsicCandidate
    public static double pow(double a, double b) {
        return StrictMath.pow(a, b); // default impl. delegates to StrictMath
    }

    public static int round(float a) {
        int intBits = Float.floatToRawIntBits(a);
        int biasedExp = (intBits & FloatConsts.EXP_BIT_MASK)
                >> (FloatConsts.SIGNIFICAND_WIDTH - 1);
        int shift = (FloatConsts.SIGNIFICAND_WIDTH - 2
                + FloatConsts.EXP_BIAS) - biasedExp;
        if ((shift & -32) == 0) { // shift >= 0 && shift < 32
            // a is a finite number such that pow(2,-32) <= ulp(a) < 1
            int r = ((intBits & FloatConsts.SIGNIF_BIT_MASK)
                    | (FloatConsts.SIGNIF_BIT_MASK + 1));
            if (intBits < 0) {
                r = -r;
            }
            // In the comments below each Java expression evaluates to the value
            // the corresponding mathematical expression:
            // (r) evaluates to a / ulp(a)
            // (r >> shift) evaluates to floor(a * 2)
            // ((r >> shift) + 1) evaluates to floor((a + 1/2) * 2)
            // (((r >> shift) + 1) >> 1) evaluates to floor(a + 1/2)
            return ((r >> shift) + 1) >> 1;
        } else {
            // a is either
            // - a finite number with abs(a) < exp(2,FloatConsts.SIGNIFICAND_WIDTH-32) < 1/2
            // - a finite number with ulp(a) >= 1 and hence a is a mathematical integer
            // - an infinity or NaN
            return (int) a;
        }
    }

    public static long round(double a) {
        long longBits = Double.doubleToRawLongBits(a);
        long biasedExp = (longBits & DoubleConsts.EXP_BIT_MASK)
                >> (DoubleConsts.SIGNIFICAND_WIDTH - 1);
        long shift = (DoubleConsts.SIGNIFICAND_WIDTH - 2
                + DoubleConsts.EXP_BIAS) - biasedExp;
        if ((shift & -64) == 0) { // shift >= 0 && shift < 64
            // a is a finite number such that pow(2,-64) <= ulp(a) < 1
            long r = ((longBits & DoubleConsts.SIGNIF_BIT_MASK)
                    | (DoubleConsts.SIGNIF_BIT_MASK + 1));
            if (longBits < 0) {
                r = -r;
            }
            return ((r >> shift) + 1) >> 1;
        } else {
            return (long) a;
        }
    }

    private static final class RandomNumberGeneratorHolder {
        static final Random randomNumberGenerator = new Random();
    }

    public static double random() {
        return RandomNumberGeneratorHolder.randomNumberGenerator.nextDouble();
    }

    @HotSpotIntrinsicCandidate
    public static int addExact(int x, int y) {
        int r = x + y;
        // HD 2-12 Overflow iff both arguments have the opposite sign of the result
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    @HotSpotIntrinsicCandidate
    public static long addExact(long x, long y) {
        long r = x + y;
        // HD 2-12 Overflow iff both arguments have the opposite sign of the result
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    @HotSpotIntrinsicCandidate
    public static int subtractExact(int x, int y) {
        int r = x - y;
        // HD 2-12 Overflow iff the arguments have different signs and
        // the sign of the result is different from the sign of x
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    @HotSpotIntrinsicCandidate
    public static long subtractExact(long x, long y) {
        long r = x - y;
        // HD 2-12 Overflow iff the arguments have different signs and
        // the sign of the result is different from the sign of x
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    @HotSpotIntrinsicCandidate
    public static int multiplyExact(int x, int y) {
        long r = (long)x * (long)y;
        if ((int)r != r) {
            throw new ArithmeticException("integer overflow");
        }
        return (int)r;
    }

    public static long multiplyExact(long x, int y) {
        return multiplyExact(x, (long)y);
    }

    @HotSpotIntrinsicCandidate
    public static long multiplyExact(long x, long y) {
        long r = x * y;
        long ax = Math.abs(x);
        long ay = Math.abs(y);
        if (((ax | ay) >>> 31 != 0)) {
            // Some bits greater than 2^31 that might cause overflow
            // Check the result using the divide operator
            // and check for the special case of Long.MIN_VALUE * -1
            if (((y != 0) && (r / y != x)) ||
                    (x == Long.MIN_VALUE && y == -1)) {
                throw new ArithmeticException("long overflow");
            }
        }
        return r;
    }

    @HotSpotIntrinsicCandidate
    public static int incrementExact(int a) {
        if (a == Integer.MAX_VALUE) {
            throw new ArithmeticException("integer overflow");
        }

        return a + 1;
    }

    @HotSpotIntrinsicCandidate
    public static long incrementExact(long a) {
        if (a == Long.MAX_VALUE) {
            throw new ArithmeticException("long overflow");
        }

        return a + 1L;
    }

    @HotSpotIntrinsicCandidate
    public static int decrementExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }

        return a - 1;
    }

    @HotSpotIntrinsicCandidate
    public static long decrementExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }

        return a - 1L;
    }

    @HotSpotIntrinsicCandidate
    public static int negateExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }

        return -a;
    }

    @HotSpotIntrinsicCandidate
    public static long negateExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }

        return -a;
    }

    public static int toIntExact(long value) {
        if ((int)value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int)value;
    }

    public static long multiplyFull(int x, int y) {
        return (long)x * (long)y;
    }

    @HotSpotIntrinsicCandidate
    public static long multiplyHigh(long x, long y) {
        if (x < 0 || y < 0) {
            // Use technique from section 8-2 of Henry S. Warren, Jr.,
            // Hacker's Delight (2nd ed.) (Addison Wesley, 2013), 173-174.
            long x1 = x >> 32;
            long x2 = x & 0xFFFFFFFFL;
            long y1 = y >> 32;
            long y2 = y & 0xFFFFFFFFL;
            long z2 = x2 * y2;
            long t = x1 * y2 + (z2 >>> 32);
            long z1 = t & 0xFFFFFFFFL;
            long z0 = t >> 32;
            z1 += x2 * y1;
            return x1 * y1 + z0 + (z1 >> 32);
        } else {
            // Use Karatsuba technique with two base 2^32 digits.
            long x1 = x >>> 32;
            long y1 = y >>> 32;
            long x2 = x & 0xFFFFFFFFL;
            long y2 = y & 0xFFFFFFFFL;
            long A = x1 * y1;
            long B = x2 * y2;
            long C = (x1 + x2) * (y1 + y2);
            long K = C - A - B;
            return (((B >>> 32) + K) >>> 32) + A;
        }
    }

    public static int floorDiv(int x, int y) {
        int r = x / y;
        // if the signs are different and modulo not zero, round down
        if ((x ^ y) < 0 && (r * y != x)) {
            r--;
        }
        return r;
    }

    public static long floorDiv(long x, int y) {
        return floorDiv(x, (long)y);
    }

    public static long floorDiv(long x, long y) {
        long r = x / y;
        // if the signs are different and modulo not zero, round down
        if ((x ^ y) < 0 && (r * y != x)) {
            r--;
        }
        return r;
    }

    public static int floorMod(int x, int y) {
        return x - floorDiv(x, y) * y;
    }

    public static int floorMod(long x, int y) {
        // Result cannot overflow the range of int.
        return (int)(x - floorDiv(x, y) * y);
    }

    public static long floorMod(long x, long y) {
        return x - floorDiv(x, y) * y;
    }

    public static int abs(int a) {
        return (a < 0) ? -a : a;
    }

    public static long abs(long a) {
        return (a < 0) ? -a : a;
    }

    public static float abs(float a) {
        return (a <= 0.0F) ? 0.0F - a : a;
    }

    @HotSpotIntrinsicCandidate
    public static double abs(double a) {
        return (a <= 0.0D) ? 0.0D - a : a;
    }

    @HotSpotIntrinsicCandidate
    public static int max(int a, int b) {
        return (a >= b) ? a : b;
    }

    public static long max(long a, long b) {
        return (a >= b) ? a : b;
    }

    // Use raw bit-wise conversions on guaranteed non-NaN arguments.
    private static final long negativeZeroFloatBits  = Float.floatToRawIntBits(-0.0f);
    private static final long negativeZeroDoubleBits = Double.doubleToRawLongBits(-0.0d);

    public static float max(float a, float b) {
        if (a != a)
            return a;   // a is NaN
        if ((a == 0.0f) &&
                (b == 0.0f) &&
                (Float.floatToRawIntBits(a) == negativeZeroFloatBits)) {
            // Raw conversion ok since NaN can't map to -0.0.
            return b;
        }
        return (a >= b) ? a : b;
    }

    public static double max(double a, double b) {
        if (a != a)
            return a;   // a is NaN
        if ((a == 0.0d) &&
                (b == 0.0d) &&
                (Double.doubleToRawLongBits(a) == negativeZeroDoubleBits)) {
            // Raw conversion ok since NaN can't map to -0.0.
            return b;
        }
        return (a >= b) ? a : b;
    }

    @HotSpotIntrinsicCandidate
    public static int min(int a, int b) {
        return (a <= b) ? a : b;
    }

    public static long min(long a, long b) {
        return (a <= b) ? a : b;
    }

    public static float min(float a, float b) {
        if (a != a)
            return a;   // a is NaN
        if ((a == 0.0f) &&
                (b == 0.0f) &&
                (Float.floatToRawIntBits(b) == negativeZeroFloatBits)) {
            // Raw conversion ok since NaN can't map to -0.0.
            return b;
        }
        return (a <= b) ? a : b;
    }

    public static double min(double a, double b) {
        if (a != a)
            return a;   // a is NaN
        if ((a == 0.0d) &&
                (b == 0.0d) &&
                (Double.doubleToRawLongBits(b) == negativeZeroDoubleBits)) {
            // Raw conversion ok since NaN can't map to -0.0.
            return b;
        }
        return (a <= b) ? a : b;
    }

    @HotSpotIntrinsicCandidate
    public static double fma(double a, double b, double c) {

        // First, screen for and handle non-finite input values whose
        // arithmetic is not supported by BigDecimal.
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isNaN(c)) {
            return Double.NaN;
        } else { // All inputs non-NaN
            boolean infiniteA = Double.isInfinite(a);
            boolean infiniteB = Double.isInfinite(b);
            boolean infiniteC = Double.isInfinite(c);
            double result;

            if (infiniteA || infiniteB || infiniteC) {
                if (infiniteA && b == 0.0 ||
                        infiniteB && a == 0.0 ) {
                    return Double.NaN;
                }
                // Store product in a double field to cause an
                // overflow even if non-strictfp evaluation is being
                // used.
                double product = a * b;
                if (Double.isInfinite(product) && !infiniteA && !infiniteB) {
                    // Intermediate overflow; might cause a
                    // spurious NaN if added to infinite c.
                    assert Double.isInfinite(c);
                    return c;
                } else {
                    result = product + c;
                    assert !Double.isFinite(result);
                    return result;
                }
            } else { // All inputs finite
                BigDecimal product = (new BigDecimal(a)).multiply(new BigDecimal(b));
                if (c == 0.0) { // Positive or negative zero
                    if (a == 0.0 || b == 0.0) {
                        return a * b + c;
                    } else {
                        return product.doubleValue();
                    }
                } else {
                    return product.add(new BigDecimal(c)).doubleValue();
                }
            }
        }
    }

    @HotSpotIntrinsicCandidate
    public static float fma(float a, float b, float c) {
        float result = (float)(((double) a * (double) b ) + (double) c);
        return result;
    }

    public static double ulp(double d) {
        int exp = getExponent(d);

        switch(exp) {
            case Double.MAX_EXPONENT + 1:       // NaN or infinity
                return Math.abs(d);

            case Double.MIN_EXPONENT - 1:       // zero or subnormal
                return Double.MIN_VALUE;

            default:
                assert exp <= Double.MAX_EXPONENT && exp >= Double.MIN_EXPONENT;

                // ulp(x) is usually 2^(SIGNIFICAND_WIDTH-1)*(2^ilogb(x))
                exp = exp - (DoubleConsts.SIGNIFICAND_WIDTH-1);
                if (exp >= Double.MIN_EXPONENT) {
                    return powerOfTwoD(exp);
                }
                else {
                    // return a subnormal result; left shift integer
                    // representation of Double.MIN_VALUE appropriate
                    // number of positions
                    return Double.longBitsToDouble(1L <<
                            (exp - (Double.MIN_EXPONENT - (DoubleConsts.SIGNIFICAND_WIDTH-1)) ));
                }
        }
    }

    public static float ulp(float f) {
        int exp = getExponent(f);

        switch(exp) {
            case Float.MAX_EXPONENT+1:        // NaN or infinity
                return Math.abs(f);

            case Float.MIN_EXPONENT-1:        // zero or subnormal
                return Float.MIN_VALUE;

            default:
                assert exp <= Float.MAX_EXPONENT && exp >= Float.MIN_EXPONENT;

                // ulp(x) is usually 2^(SIGNIFICAND_WIDTH-1)*(2^ilogb(x))
                exp = exp - (FloatConsts.SIGNIFICAND_WIDTH-1);
                if (exp >= Float.MIN_EXPONENT) {
                    return powerOfTwoF(exp);
                } else {
                    // return a subnormal result; left shift integer
                    // representation of FloatConsts.MIN_VALUE appropriate
                    // number of positions
                    return Float.intBitsToFloat(1 <<
                            (exp - (Float.MIN_EXPONENT - (FloatConsts.SIGNIFICAND_WIDTH-1)) ));
                }
        }
    }

    public static double signum(double d) {
        return (d == 0.0 || Double.isNaN(d))?d:copySign(1.0, d);
    }

    public static float signum(float f) {
        return (f == 0.0f || Float.isNaN(f))?f:copySign(1.0f, f);
    }

    public static double sinh(double x) {
        return StrictMath.sinh(x);
    }

    public static double cosh(double x) {
        return StrictMath.cosh(x);
    }

    public static double tanh(double x) {
        return StrictMath.tanh(x);
    }

    public static double hypot(double x, double y) {
        return StrictMath.hypot(x, y);
    }

    public static double expm1(double x) {
        return StrictMath.expm1(x);
    }

    public static double log1p(double x) {
        return StrictMath.log1p(x);
    }

    public static double copySign(double magnitude, double sign) {
        return Double.longBitsToDouble((Double.doubleToRawLongBits(sign) &
                (DoubleConsts.SIGN_BIT_MASK)) |
                (Double.doubleToRawLongBits(magnitude) &
                        (DoubleConsts.EXP_BIT_MASK |
                                DoubleConsts.SIGNIF_BIT_MASK)));
    }

    public static float copySign(float magnitude, float sign) {
        return Float.intBitsToFloat((Float.floatToRawIntBits(sign) &
                (FloatConsts.SIGN_BIT_MASK)) |
                (Float.floatToRawIntBits(magnitude) &
                        (FloatConsts.EXP_BIT_MASK |
                                FloatConsts.SIGNIF_BIT_MASK)));
    }

    public static int getExponent(float f) {
        /*
         * Bitwise convert f to integer, mask out exponent bits, shift
         * to the right and then subtract out float's bias adjust to
         * get true exponent value
         */
        return ((Float.floatToRawIntBits(f) & FloatConsts.EXP_BIT_MASK) >>
                (FloatConsts.SIGNIFICAND_WIDTH - 1)) - FloatConsts.EXP_BIAS;
    }

    public static int getExponent(double d) {
        /*
         * Bitwise convert d to long, mask out exponent bits, shift
         * to the right and then subtract out double's bias adjust to
         * get true exponent value.
         */
        return (int)(((Double.doubleToRawLongBits(d) & DoubleConsts.EXP_BIT_MASK) >>
                (DoubleConsts.SIGNIFICAND_WIDTH - 1)) - DoubleConsts.EXP_BIAS);
    }

    public static double nextAfter(double start, double direction) {

        // Branch to descending case first as it is more costly than ascending
        // case due to start != 0.0d conditional.
        if (start > direction) { // descending
            if (start != 0.0d) {
                final long transducer = Double.doubleToRawLongBits(start);
                return Double.longBitsToDouble(transducer + ((transducer > 0L) ? -1L : 1L));
            } else { // start == 0.0d && direction < 0.0d
                return -Double.MIN_VALUE;
            }
        } else if (start < direction) { // ascending
            // Add +0.0 to get rid of a -0.0 (+0.0 + -0.0 => +0.0)
            // then bitwise convert start to integer.
            final long transducer = Double.doubleToRawLongBits(start + 0.0d);
            return Double.longBitsToDouble(transducer + ((transducer >= 0L) ? 1L : -1L));
        } else if (start == direction) {
            return direction;
        } else { // isNaN(start) || isNaN(direction)
            return start + direction;
        }
    }

    public static float nextAfter(float start, double direction) {
        // Branch to descending case first as it is more costly than ascending
        // case due to start != 0.0f conditional.
        if (start > direction) { // descending
            if (start != 0.0f) {
                final int transducer = Float.floatToRawIntBits(start);
                return Float.intBitsToFloat(transducer + ((transducer > 0) ? -1 : 1));
            } else { // start == 0.0f && direction < 0.0f
                return -Float.MIN_VALUE;
            }
        } else if (start < direction) { // ascending
            // Add +0.0 to get rid of a -0.0 (+0.0 + -0.0 => +0.0)
            // then bitwise convert start to integer.
            final int transducer = Float.floatToRawIntBits(start + 0.0f);
            return Float.intBitsToFloat(transducer + ((transducer >= 0) ? 1 : -1));
        } else if (start == direction) {
            return (float)direction;
        } else { // isNaN(start) || isNaN(direction)
            return start + (float)direction;
        }
    }

    public static double nextUp(double d) {
        // Use a single conditional and handle the likely cases first.
        if (d < Double.POSITIVE_INFINITY) {
            // Add +0.0 to get rid of a -0.0 (+0.0 + -0.0 => +0.0).
            final long transducer = Double.doubleToRawLongBits(d + 0.0D);
            return Double.longBitsToDouble(transducer + ((transducer >= 0L) ? 1L : -1L));
        } else { // d is NaN or +Infinity
            return d;
        }
    }

    public static float nextUp(float f) {
        // Use a single conditional and handle the likely cases first.
        if (f < Float.POSITIVE_INFINITY) {
            // Add +0.0 to get rid of a -0.0 (+0.0 + -0.0 => +0.0).
            final int transducer = Float.floatToRawIntBits(f + 0.0F);
            return Float.intBitsToFloat(transducer + ((transducer >= 0) ? 1 : -1));
        } else { // f is NaN or +Infinity
            return f;
        }
    }

    public static double nextDown(double d) {
        if (Double.isNaN(d) || d == Double.NEGATIVE_INFINITY)
            return d;
        else {
            if (d == 0.0)
                return -Double.MIN_VALUE;
            else
                return Double.longBitsToDouble(Double.doubleToRawLongBits(d) +
                        ((d > 0.0d)?-1L:+1L));
        }
    }

    public static float nextDown(float f) {
        if (Float.isNaN(f) || f == Float.NEGATIVE_INFINITY)
            return f;
        else {
            if (f == 0.0f)
                return -Float.MIN_VALUE;
            else
                return Float.intBitsToFloat(Float.floatToRawIntBits(f) +
                        ((f > 0.0f)?-1:+1));
        }
    }

    public static double scalb(double d, int scaleFactor) {
        final int MAX_SCALE = Double.MAX_EXPONENT + -Double.MIN_EXPONENT +
                DoubleConsts.SIGNIFICAND_WIDTH + 1;
        int exp_adjust = 0;
        int scale_increment = 0;
        double exp_delta = Double.NaN;

        // Make sure scaling factor is in a reasonable range

        if(scaleFactor < 0) {
            scaleFactor = Math.max(scaleFactor, -MAX_SCALE);
            scale_increment = -512;
            exp_delta = twoToTheDoubleScaleDown;
        }
        else {
            scaleFactor = Math.min(scaleFactor, MAX_SCALE);
            scale_increment = 512;
            exp_delta = twoToTheDoubleScaleUp;
        }

        // Calculate (scaleFactor % +/-512), 512 = 2^9, using
        // technique from "Hacker's Delight" section 10-2.
        int t = (scaleFactor >> 9-1) >>> 32 - 9;
        exp_adjust = ((scaleFactor + t) & (512 -1)) - t;

        d *= powerOfTwoD(exp_adjust);
        scaleFactor -= exp_adjust;

        while(scaleFactor != 0) {
            d *= exp_delta;
            scaleFactor -= scale_increment;
        }
        return d;
    }

    public static float scalb(float f, int scaleFactor) {
        final int MAX_SCALE = Float.MAX_EXPONENT + -Float.MIN_EXPONENT +
                FloatConsts.SIGNIFICAND_WIDTH + 1;

        scaleFactor = Math.max(Math.min(scaleFactor, MAX_SCALE), -MAX_SCALE);

        return (float)((double)f*powerOfTwoD(scaleFactor));
    }

    // Constants used in scalb
    static double twoToTheDoubleScaleUp = powerOfTwoD(512);
    static double twoToTheDoubleScaleDown = powerOfTwoD(-512);

    /**
     * Returns a floating-point power of two in the normal range.
     */
    static double powerOfTwoD(int n) {
        assert(n >= Double.MIN_EXPONENT && n <= Double.MAX_EXPONENT);
        return Double.longBitsToDouble((((long)n + (long)DoubleConsts.EXP_BIAS) <<
                (DoubleConsts.SIGNIFICAND_WIDTH-1))
                & DoubleConsts.EXP_BIT_MASK);
    }

    /**
     * Returns a floating-point power of two in the normal range.
     */
    static float powerOfTwoF(int n) {
        assert(n >= Float.MIN_EXPONENT && n <= Float.MAX_EXPONENT);
        return Float.intBitsToFloat(((n + FloatConsts.EXP_BIAS) <<
                (FloatConsts.SIGNIFICAND_WIDTH-1))
                & FloatConsts.EXP_BIT_MASK);
    }
}
