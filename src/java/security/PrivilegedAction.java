package java.security;

/**
 * @className: PrivilegedAction
 * @author: doudou
 * @datetime: 2021/11/4
 * @description: 要在启用特权的情况下执行的计算。
 *      计算是通过在 {@code PrivilegedAction} 对象上调用 {@code AccessController.doPrivileged} 来执行的。
 *      此接口仅用于不抛出检查异常的计算； 抛出检查异常的计算必须使用 {@code PrivilegedExceptionAction} 代替。
 */
public interface PrivilegedAction<T> {

    /**
     * 执行计算。 此方法将在启用权限后由 {@code AccessController.doPrivileged} 调用。
     */
    T run();
}
