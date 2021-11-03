package sun.reflect.generics.visitor;

/**
 * 访问 TypeTree 并生成类型 T 的结果。
 */
public interface TypeTreeVisitor<T> {

    T getResult();
}
