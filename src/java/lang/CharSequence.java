package java.lang;

import java.util.stream.IntStream;

/**
 * CharSequence是char值的可读序列。 该接口提供对许多不同类型的char序列的统一，只读访问。
 * @since 1.4
 */
public interface CharSequence {

    int length();

    char charAt(int index);

    CharSequence subSequence(int start, int end);

    public String toString();

//    public default IntStream chars() {
//
//    }
}
