package java.security;

import jdk.internal.reflect.CallerSensitive;

/**
 * @className: AccessController
 * @author: doudou
 * @datetime: 2021/11/4
 * @description: AccessController 类用于访问控制操作和决策。
 *
 * AccessController类的构造器是私有的，因此不能对其进行实例化。
 * 它向外部提供了一些静态方法，其中最关键的就是checkPermission(Permission p)，
 * 该方法基于当前安装的Policy对象，判定当前保护欲是否拥有指定权限。安全管理器SecurityManager提供的一系列check***的方法，
 * 最后基本都是通过AccessController.checkPermission(Permission p)完成。
 */
public final class AccessController {

    private AccessController() {}

    /**
     * 在启用权限的情况下执行指定的 {@code PrivilegedAction}。 使用调用者保护域拥有的所有权限执行该操作。
     */
    @CallerSensitive
    public static native <T> T doPrivileged(PrivilegedAction<T> action);

    /**
     * 在启用权限的情况下执行指定的 {@code PrivilegedAction}。 使用调用者保护域拥有的所有权限执行该操作。
     */
    @CallerSensitive
    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action) {
        AccessControlContext acc = getStackAccessControlContext();
        if (acc == null) {
            return AccessController.doPrivileged(action);
        }
        DomainCombiner dc = acc.getAssignedCombiner();
        return AccessController.doPrivileged(action, preserveCombiner(dc, Reflection.getCallerClass()));
    }

    @CallerSensitive
    public static native <T> T doPrivileged(PrivilegedAcion<T> action, AccessControlContext context);
}
