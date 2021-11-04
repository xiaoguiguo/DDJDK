package java.security;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Permission 对象的集合
 */
public abstract class PermissionCollection implements Serializable {
    private static final long serialVersionUID = -8499506250507210089L;

    private volatile boolean readOnly;

    public abstract void add(Permission permission);

    /**
     * 检查此 PermissionCollection 中保存的 Permission 对象集合是否隐含了指定的权限。
     */
    public abstract boolean implies(Permission permission);

    /**
     * 返回集合中所有 Permission 对象的枚举。
     */
    public abstract Enumeration<Permission> elements();

    public Stream<Permission> elementsAsStream() {
        int characteristics = isReadOnly()
                ? Spliterator.NONNULL | Spliterator.IMMUTABLE
                : Spliterator.NONNULL;
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(elements().asIterator(), characteristics),
                false);
    }

    public void setReadOnly() {
        readOnly = true;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

}
