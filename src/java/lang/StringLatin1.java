package java.lang;

import static java.lang.String.LATIN1;

/**
 * @className: StringLatin1
 * @author: doudou
 * @datetime: 2021/11/1
 * @description: 单字节 字符串
 */
final class StringLatin1 {

    /**
     * value[index] & 0xff: 结果为int,即byte -> int
     * 16进制数 0xff 二进制就是11111111 ,高位都为0
     * 因为&操作中，超过0xff的部分，全部都会变成0，而对于0xff以内的数据，
     * 它不会影响原来的值,取其最低8位,即一个字节的值,此操作也能保证负数补码不变
     */
    public static char charAt(byte[] value, int index) {
        if (index < 0 || index >= value.length) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return (char)(value[index] & 0xff);
    }

    public static String newString(byte[] val, int index, int len) {
        return new String(Arrays.copyOfRange(val, index, index + len), LATIN1);
    }

    public static char getChar(byte[] val, int index) {
        return (char)(val[index] & 0xff);
    }
}
