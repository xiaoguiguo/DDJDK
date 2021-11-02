package java.lang.reflect;

/**
 * A common interface for all entities that declare type variables.
 * 所有声明泛型变量的公共接口
 */
public interface GenericDeclaration extends AnnotatedElement {

    /**
     * 一个 TypeVariable 对象数组，表示由这个泛型变量声明的类型变量
     */
    public TypeVariable<?>[] getTypeParameters();
}
