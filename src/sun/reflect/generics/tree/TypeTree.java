package sun.reflect.generics.tree;

import sun.reflect.generics.visitor.TypeTreeVisitor;

/**
 * 在通用签名 AST 中表示类型表达式的所有节点的通用超类型。
 */
public interface TypeTree extends Tree {

    /**
     * 访问者模式的接受方法。
     */
    void accept(TypeTreeVisitor<?> v);
}
