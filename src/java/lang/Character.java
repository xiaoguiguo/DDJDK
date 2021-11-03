package java.lang;

import java.io.Serializable;

/**
 * @className: Character
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: The Character class wraps a value of the primitive type char in an object.
 *      An object of class Character contains a single field whose type is char.
 */
public final class Character implements Serializable, Comparable<Character> {
    private static final long serialVersionUID = -4301462514225195042L;

    /**
     * 可用于与字符串相互转换的最小基数。 此字段的常量值是基数转换方法
     * （例如 digit 方法、forDigit 方法和 Integer 类的 toString 方法）中基数参数所允许的最小值。
     */
    public static final int MIN_RADIX = 2;

    /**
     * 可用于与字符串相互转换的最大基数。 该字段的常量值是在基数转换方法中，
     * 如 digit 方法、forDigit 方法和 Integer 类的 toString 方法中基数参数允许的最大值。
     */
    public static final int MAX_RADIX = 36;

    /** 该字段的常量值是 char 类型的最小值，'\u0000'。 */
    public static final char MIN_VALUE = '\u0000';
    /** 该字段的常量值是 char 类型的最大值，'\uFFFF'。 */
    public static final char MAX_VALUE = '\uFFFF';

    public static final Class<Character> TYPE = (Class<Character>) Class.getPrimitiveClass("char");

    private final char value;

    @Deprecated(since="9")
    public Character(char value) {
        this.value = value;
    }

    /** Unicode规范中的常规类别“Cn”。*/
    public static final byte UNASSIGNED = 0;

    public int compareTo(Character anotherCharacter) {
        return compare(this.value, anotherCharacter.value);
    }

    public static int compare(char x, char y) {
        return x - y;
    }
}
