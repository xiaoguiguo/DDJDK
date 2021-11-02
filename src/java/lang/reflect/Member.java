package java.lang.reflect;

/**
 * 反映单个成员（字段或方法）或构造函数的标识信息的接口。
 */
public interface Member {

    /**
     * 标识类或接口的所有公共成员的集合，包括继承的成员。
     */
    public static final int PUBLIC = 0;

    /**
     * 标识类或接口的声明成员集。 不包括继承成员。
     */
    public static final int DECLARED = 1;

    /**
     * 返回表示类或接口的 Class 对象，该类或接口声明了此 Member 表示的成员或构造函数。
     */
    public Class<?> getDeclaringClass();

    /**
     * 返回由该成员表示的底层成员或构造函数的简单名称。
     */
    public String getName();

    /**
     * 以整数形式返回此 Member 表示的成员或构造函数的 Java 语言修饰符。
     * Modifier 类应该用于解码整数中的修饰符。
     *
     * 返回底层成员的 Java 语言修饰符
     */
    public int getModifiers();

    /**
     * 如果该成员是由编译器引入的，则返回 true； 否则返回 false。
     */
    public boolean isSynthetic();
}
