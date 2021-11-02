package java.lang.reflect;

/**
 * 这个接口表示的是泛型变量，例如：List<T>中的T就是类型变量；而class C1<T1,T2,T3>{}表示一个类，
 * 这个类中定义了3个泛型变量类型，分别是T1、T2和T2，泛型变量在java中使用TypeVariable接口来表示，
 * 可以通过这个接口提供的方法获取泛型变量类型的详细信息。
 */
public interface TypeVariable<D extends GenericDeclaration> extends Type, AnnotatedElement {

    /** 返回表示此类型变量上限的 Type 对象数组。 如果没有明确声明上限，则上限为 Object。 */
    Type[] getBounds();

    /** 返回表示声明此类型变量的泛型声明的 GenericDeclaration 对象。*/
    D getGenericDeclaration();

    /** 返回泛型变量的名称 */
    String getName();

    /** 返回泛型变量上限的对象数组 */
    AnnotatedType[] getAnnotatedBounds();
}
