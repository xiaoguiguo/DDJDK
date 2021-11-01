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
public class Math {

    @HotSpotIntrinsicCandidate
    public static int min(int a, int b) {
        return (a <= b) ? a : b;
    }
}
