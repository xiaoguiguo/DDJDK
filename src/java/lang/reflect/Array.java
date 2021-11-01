package java.lang.reflect;

import jdk.internal.HotSpotIntrinsicCandidate;

/**
 * @className: Array
 * @author: doudou
 * @datetime: 2021/11/1
 * @description: The {@code Array} class provides static methods to dynamically create and access Java arrays.
 */
public final class Array {

    private Array() {}

    public static Object newInstance(Class<?> componentType, int length) throws NegativeArraySizeException {
        return newArray(componentType, length);
    }

    @HotSpotIntrinsicCandidate
    private static native Object newArray(Class<?> componentType, int length) throws NegativeArraySizeException;
}
