package java.lang.reflect;

/**
 * @className: Type
 * @author: doudou
 * @datetime: 2021/10/31
 * @description: 这是一个顶层接口，java中的任何类型都可以用这个来表示，这个接口是Java编程语言中所有类型的公共超接口。
 * 这些类型包括原始类型、泛型类型、泛型变量类型、通配符类型、泛型数组类型、数组类型等各种类型。
 */
public interface Type {

    /** Returns a string describing this type, including information about any type parameters */
    default String getTypeName() {
        return toString();
    }
}
