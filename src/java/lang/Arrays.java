package java.lang;

import java.lang.reflect.Array;

/**
 * @className: Arrays
 * @author: doudou
 * @datetime: 2021/11/1
 * @description: This class contains various methods for manipulating arrays (such as sorting and searching).
 *      This class also contains a static factory that allows arrays to be viewed as lists.
 */
public class Arrays {

    // 进行并行排序的最小数组长度
    private static final int MIN_ARRAY_SORT_GRAN = 1 << 13;

    private Arrays() {}

//    @SuppressWarnings("unchecked")
//    public static <T> T[] copyOfRange(byte[] original, int from, int to) {
//        return copyOfRange(original, from, to, (Class<? extends T[]>) original.getClass());
//        return null;
//    }

    public static byte[] copyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        byte[] copy = new byte[newLength];
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }
}
