package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;

/**
 * 基本类型boolean的包装类，只包含一个类型为boolean的属性
 */
public final class Boolean implements java.io.Serializable,
                                      Comparable<Boolean>
{
    /**
     * 基本类型的boolean值：true
     */
    public static final Boolean TRUE = new Boolean(true);

    /**
     * 基本类型的boolean值：false
     */
    public static final Boolean FALSE = new Boolean(false);

    /**
     * 基本类型boolean的包装类Boolean
     */
    @SuppressWarnings("unchecked")
    public static final Class<Boolean> TYPE = (Class<Boolean>) Class.getPrimitiveClass("boolean");

    /**
     * 唯一的值
     */
    private final boolean value;

    private static final long serialVersionUID = -3665804199014368530L;

    @Deprecated(since="9")
    public Boolean(boolean value) {
        this.value = value;
    }

    @Deprecated(since="9")
    public Boolean(String s) {
        this(parseBoolean(s));
    }

    /**
     * 字符串解析成boolean值
     */
    public static boolean parseBoolean(String s) {
        return "true".equalsIgnoreCase(s);
    }

    /**
     * 返回基本类型boolean值
     */
    @HotSpotIntrinsicCandidate
    public boolean booleanValue() {
        return value;
    }

    @HotSpotIntrinsicCandidate
    public static Boolean valueOf(boolean b) {
        return (b ? TRUE : FALSE);
    }

    /**
     * 返回指定字符串标识的boolean值
     */
    public static Boolean valueOf(String s) {
        return parseBoolean(s) ? TRUE : FALSE;
    }

    public static String toString(boolean b) {
        return b ? "true" : "false";
    }

    public String toString() {
        return value ? "true" : "false";
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(value);
    }

    /**
     * 至于true和false的hashcode值为什么是1231和1237，是因为要取一个不大不小的素数来避免hash碰撞，
     * 太大的素数不利于计算，太小的素数碰撞几率增大
     */
    public static int hashCode(boolean value) {
        return value ? 1231 : 1237;
    }

   /**
     * 判断两个对象是否相等
     */
    public boolean equals(Object obj) {
        if (obj instanceof Boolean) {
            return value == ((Boolean)obj).booleanValue();
        }
        return false;
    }

    /**
     * 当且仅当由参数命名的系统属性存在且等于字符串 {@code "true"}（忽略大小写）时，才返回 {@code true}。
     * 系统属性可通过 {@code getProperty} 访问，该方法由 {@code System} 类定义。
     * 如果没有指定名称的属性，或者指定名称为空或空，则返回{@code false}。
     */
    public static boolean getBoolean(String name) {
        boolean result = false;
        try {
            result = parseBoolean(System.getProperty(name));
        } catch (IllegalArgumentException | NullPointerException e) {
        }
        return result;
    }

    /**
     * 比较函数
     */
    public int compareTo(Boolean b) {
        return compare(this.value, b.value);
    }

    /**
     * 1. x == y => 0
     * 2. x == true => 1
     * 3. x == false => -1
     */
    public static int compare(boolean x, boolean y) {
        return (x == y) ? 0 : (x ? 1 : -1);
    }

    /**
     * 逻辑 并
     */
    public static boolean logicalAnd(boolean a, boolean b) {
        return a && b;
    }

    /**
     * 逻辑 或
     */
    public static boolean logicalOr(boolean a, boolean b) {
        return a || b;
    }

    /**
     * 逻辑 异或
     * 当两个操作数不相同时返回true，返回false
     */
    public static boolean logicalXor(boolean a, boolean b) {
        return a ^ b;
    }
}
