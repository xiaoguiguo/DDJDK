package java.security;

import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import sun.security.util.Debug;

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

    @CallerSensitive
    public static <T> T doPrivileged(PrivilegedAction<T> action,
                                     AccessControlContext context, Permission... perms) {

        AccessControlContext parent = getContext();
        if (perms == null) {
            throw new NullPointerException("null permissions parameter");
        }
        Class <?> caller = Reflection.getCallerClass();
        DomainCombiner dc = (context == null) ? null : context.getCombiner();
        return AccessController.doPrivileged(action, createWrapper(dc,
                caller, parent, context, perms));
    }


    /**
     * Performs the specified {@code PrivilegedAction} with privileges
     * enabled and restricted by the specified
     * {@code AccessControlContext} and with a privilege scope limited
     * by specified {@code Permission} arguments.
     *
     * The action is performed with the intersection of the permissions
     * possessed by the caller's protection domain, and those possessed
     * by the domains represented by the specified
     * {@code AccessControlContext}.
     * <p>
     * If the action's {@code run} method throws an (unchecked) exception,
     * it will propagate through this method.
     *
     * <p> This method preserves the current AccessControlContext's
     * DomainCombiner (which may be null) while the action is performed.
     * <p>
     * If a security manager is installed and the specified
     * {@code AccessControlContext} was not created by system code and the
     * caller's {@code ProtectionDomain} has not been granted the
     * {@literal "createAccessControlContext"}
     * {@link java.security.SecurityPermission}, then the action is performed
     * with no permissions.
     *
     * @param <T> the type of the value returned by the PrivilegedAction's
     *                  {@code run} method.
     * @param action the action to be performed.
     * @param context an <i>access control context</i>
     *                representing the restriction to be applied to the
     *                caller's domain's privileges before performing
     *                the specified action.  If the context is
     *                {@code null},
     *                then no additional restriction is applied.
     * @param perms the {@code Permission} arguments which limit the
     *              scope of the caller's privileges. The number of arguments
     *              is variable.
     *
     * @return the value returned by the action's {@code run} method.
     *
     * @throws NullPointerException if action or perms or any element of
     *         perms is {@code null}
     *
     * @see #doPrivileged(PrivilegedAction)
     * @see #doPrivileged(PrivilegedExceptionAction,AccessControlContext)
     * @see java.security.DomainCombiner
     *
     * @since 1.8
     */
    @CallerSensitive
    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action,
                                                 AccessControlContext context, Permission... perms) {

        AccessControlContext parent = getContext();
        DomainCombiner dc = parent.getCombiner();
        if (dc == null && context != null) {
            dc = context.getCombiner();
        }
        if (perms == null) {
            throw new NullPointerException("null permissions parameter");
        }
        Class <?> caller = Reflection.getCallerClass();
        return AccessController.doPrivileged(action, createWrapper(dc, caller,
                parent, context, perms));
    }

    /**
     * Performs the specified {@code PrivilegedExceptionAction} with
     * privileges enabled.  The action is performed with <i>all</i> of the
     * permissions possessed by the caller's protection domain.
     *
     * <p> If the action's {@code run} method throws an <i>unchecked</i>
     * exception, it will propagate through this method.
     *
     * <p> Note that any DomainCombiner associated with the current
     * AccessControlContext will be ignored while the action is performed.
     *
     * @param <T> the type of the value returned by the
     *                  PrivilegedExceptionAction's {@code run} method.
     *
     * @param action the action to be performed
     *
     * @return the value returned by the action's {@code run} method
     *
     * @exception PrivilegedActionException if the specified action's
     *         {@code run} method threw a <i>checked</i> exception
     * @exception NullPointerException if the action is {@code null}
     *
     * @see #doPrivileged(PrivilegedAction)
     * @see #doPrivileged(PrivilegedExceptionAction,AccessControlContext)
     * @see #doPrivilegedWithCombiner(PrivilegedExceptionAction)
     * @see java.security.DomainCombiner
     */
    @CallerSensitive
    public static native <T> T
    doPrivileged(PrivilegedExceptionAction<T> action)
            throws PrivilegedActionException;


    /**
     * Performs the specified {@code PrivilegedExceptionAction} with
     * privileges enabled.  The action is performed with <i>all</i> of the
     * permissions possessed by the caller's protection domain.
     *
     * <p> If the action's {@code run} method throws an <i>unchecked</i>
     * exception, it will propagate through this method.
     *
     * <p> This method preserves the current AccessControlContext's
     * DomainCombiner (which may be null) while the action is performed.
     *
     * @param <T> the type of the value returned by the
     *                  PrivilegedExceptionAction's {@code run} method.
     *
     * @param action the action to be performed.
     *
     * @return the value returned by the action's {@code run} method
     *
     * @exception PrivilegedActionException if the specified action's
     *         {@code run} method threw a <i>checked</i> exception
     * @exception NullPointerException if the action is {@code null}
     *
     * @see #doPrivileged(PrivilegedAction)
     * @see #doPrivileged(PrivilegedExceptionAction,AccessControlContext)
     * @see java.security.DomainCombiner
     *
     * @since 1.6
     */
    @CallerSensitive
    public static <T> T doPrivilegedWithCombiner(PrivilegedExceptionAction<T> action)
            throws PrivilegedActionException
    {
        AccessControlContext acc = getStackAccessControlContext();
        if (acc == null) {
            return AccessController.doPrivileged(action);
        }
        DomainCombiner dc = acc.getAssignedCombiner();
        return AccessController.doPrivileged(action,
                preserveCombiner(dc, Reflection.getCallerClass()));
    }

    /**
     * preserve the combiner across the doPrivileged call
     */
    private static AccessControlContext preserveCombiner(DomainCombiner combiner,
                                                         Class<?> caller)
    {
        return createWrapper(combiner, caller, null, null, null);
    }

    /**
     * Create a wrapper to contain the limited privilege scope data.
     */
    private static AccessControlContext
    createWrapper(DomainCombiner combiner, Class<?> caller,
                  AccessControlContext parent, AccessControlContext context,
                  Permission[] perms)
    {
        ProtectionDomain callerPD = getCallerPD(caller);
        // check if caller is authorized to create context
        if (context != null && !context.isAuthorized() &&
                System.getSecurityManager() != null &&
                !callerPD.impliesCreateAccessControlContext())
        {
            return getInnocuousAcc();
        } else {
            return new AccessControlContext(callerPD, combiner, parent,
                    context, perms);
        }
    }

    private static class AccHolder {
        // An AccessControlContext with no granted permissions.
        // Only initialized on demand when getInnocuousAcc() is called.
        static final AccessControlContext innocuousAcc =
                new AccessControlContext(new ProtectionDomain[] {
                        new ProtectionDomain(null, null) });
    }
    private static AccessControlContext getInnocuousAcc() {
        return AccHolder.innocuousAcc;
    }

    private static ProtectionDomain getCallerPD(final Class <?> caller) {
        ProtectionDomain callerPd = doPrivileged
                (new PrivilegedAction<>() {
                    public ProtectionDomain run() {
                        return caller.getProtectionDomain();
                    }
                });

        return callerPd;
    }

    /**
     * Performs the specified {@code PrivilegedExceptionAction} with
     * privileges enabled and restricted by the specified
     * {@code AccessControlContext}.  The action is performed with the
     * intersection of the permissions possessed by the caller's
     * protection domain, and those possessed by the domains represented by the
     * specified {@code AccessControlContext}.
     * <p>
     * If the action's {@code run} method throws an <i>unchecked</i>
     * exception, it will propagate through this method.
     * <p>
     * If a security manager is installed and the specified
     * {@code AccessControlContext} was not created by system code and the
     * caller's {@code ProtectionDomain} has not been granted the
     * {@literal "createAccessControlContext"}
     * {@link java.security.SecurityPermission}, then the action is performed
     * with no permissions.
     *
     * @param <T> the type of the value returned by the
     *                  PrivilegedExceptionAction's {@code run} method.
     * @param action the action to be performed
     * @param context an <i>access control context</i>
     *                representing the restriction to be applied to the
     *                caller's domain's privileges before performing
     *                the specified action.  If the context is
     *                {@code null}, then no additional restriction is applied.
     *
     * @return the value returned by the action's {@code run} method
     *
     * @exception PrivilegedActionException if the specified action's
     *         {@code run} method threw a <i>checked</i> exception
     * @exception NullPointerException if the action is {@code null}
     *
     * @see #doPrivileged(PrivilegedAction)
     * @see #doPrivileged(PrivilegedAction,AccessControlContext)
     */
    @CallerSensitive
    public static native <T> T
    doPrivileged(PrivilegedExceptionAction<T> action,
                 AccessControlContext context)
            throws PrivilegedActionException;


    /**
     * Performs the specified {@code PrivilegedExceptionAction} with
     * privileges enabled and restricted by the specified
     * {@code AccessControlContext} and with a privilege scope limited by
     * specified {@code Permission} arguments.
     *
     * The action is performed with the intersection of the permissions
     * possessed by the caller's protection domain, and those possessed
     * by the domains represented by the specified
     * {@code AccessControlContext}.
     * <p>
     * If the action's {@code run} method throws an (unchecked) exception,
     * it will propagate through this method.
     * <p>
     * If a security manager is installed and the specified
     * {@code AccessControlContext} was not created by system code and the
     * caller's {@code ProtectionDomain} has not been granted the
     * {@literal "createAccessControlContext"}
     * {@link java.security.SecurityPermission}, then the action is performed
     * with no permissions.
     *
     * @param <T> the type of the value returned by the
     *                  PrivilegedExceptionAction's {@code run} method.
     * @param action the action to be performed.
     * @param context an <i>access control context</i>
     *                representing the restriction to be applied to the
     *                caller's domain's privileges before performing
     *                the specified action.  If the context is
     *                {@code null},
     *                then no additional restriction is applied.
     * @param perms the {@code Permission} arguments which limit the
     *              scope of the caller's privileges. The number of arguments
     *              is variable.
     *
     * @return the value returned by the action's {@code run} method.
     *
     * @throws PrivilegedActionException if the specified action's
     *         {@code run} method threw a <i>checked</i> exception
     * @throws NullPointerException if action or perms or any element of
     *         perms is {@code null}
     *
     * @see #doPrivileged(PrivilegedAction)
     * @see #doPrivileged(PrivilegedAction,AccessControlContext)
     *
     * @since 1.8
     */
    @CallerSensitive
    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action,
                                     AccessControlContext context, Permission... perms)
            throws PrivilegedActionException
    {
        AccessControlContext parent = getContext();
        if (perms == null) {
            throw new NullPointerException("null permissions parameter");
        }
        Class <?> caller = Reflection.getCallerClass();
        DomainCombiner dc = (context == null) ? null : context.getCombiner();
        return AccessController.doPrivileged(action, createWrapper(dc, caller, parent, context, perms));
    }


    /**
     * Performs the specified {@code PrivilegedExceptionAction} with
     * privileges enabled and restricted by the specified
     * {@code AccessControlContext} and with a privilege scope limited by
     * specified {@code Permission} arguments.
     *
     * The action is performed with the intersection of the permissions
     * possessed by the caller's protection domain, and those possessed
     * by the domains represented by the specified
     * {@code AccessControlContext}.
     * <p>
     * If the action's {@code run} method throws an (unchecked) exception,
     * it will propagate through this method.
     *
     * <p> This method preserves the current AccessControlContext's
     * DomainCombiner (which may be null) while the action is performed.
     * <p>
     * If a security manager is installed and the specified
     * {@code AccessControlContext} was not created by system code and the
     * caller's {@code ProtectionDomain} has not been granted the
     * {@literal "createAccessControlContext"}
     * {@link java.security.SecurityPermission}, then the action is performed
     * with no permissions.
     *
     * @param <T> the type of the value returned by the
     *                  PrivilegedExceptionAction's {@code run} method.
     * @param action the action to be performed.
     * @param context an <i>access control context</i>
     *                representing the restriction to be applied to the
     *                caller's domain's privileges before performing
     *                the specified action.  If the context is
     *                {@code null},
     *                then no additional restriction is applied.
     * @param perms the {@code Permission} arguments which limit the
     *              scope of the caller's privileges. The number of arguments
     *              is variable.
     *
     * @return the value returned by the action's {@code run} method.
     *
     * @throws PrivilegedActionException if the specified action's
     *         {@code run} method threw a <i>checked</i> exception
     * @throws NullPointerException if action or perms or any element of
     *         perms is {@code null}
     *
     * @see #doPrivileged(PrivilegedAction)
     * @see #doPrivileged(PrivilegedAction,AccessControlContext)
     * @see java.security.DomainCombiner
     *
     * @since 1.8
     */
    @CallerSensitive
    public static <T> T doPrivilegedWithCombiner(PrivilegedExceptionAction<T> action,
                                                 AccessControlContext context,
                                                 Permission... perms)
            throws PrivilegedActionException
    {
        AccessControlContext parent = getContext();
        DomainCombiner dc = parent.getCombiner();
        if (dc == null && context != null) {
            dc = context.getCombiner();
        }
        if (perms == null) {
            throw new NullPointerException("null permissions parameter");
        }
        Class <?> caller = Reflection.getCallerClass();
        return AccessController.doPrivileged(action, createWrapper(dc, caller,
                parent, context, perms));
    }

    /**
     * Returns the AccessControl context. i.e., it gets
     * the protection domains of all the callers on the stack,
     * starting at the first class with a non-null
     * ProtectionDomain.
     *
     * @return the access control context based on the current stack or
     *         null if there was only privileged system code.
     */

    private static native AccessControlContext getStackAccessControlContext();


    /**
     * Returns the "inherited" AccessControl context. This is the context
     * that existed when the thread was created. Package private so
     * AccessControlContext can use it.
     */

    static native AccessControlContext getInheritedAccessControlContext();

    /**
     * This method takes a "snapshot" of the current calling context, which
     * includes the current Thread's inherited AccessControlContext and any
     * limited privilege scope, and places it in an AccessControlContext object.
     * This context may then be checked at a later point, possibly in another thread.
     *
     * @see AccessControlContext
     *
     * @return the AccessControlContext based on the current context.
     */

    public static AccessControlContext getContext()
    {
        AccessControlContext acc = getStackAccessControlContext();
        if (acc == null) {
            // all we had was privileged system code. We don't want
            // to return null though, so we construct a real ACC.
            return new AccessControlContext(null, true);
        } else {
            return acc.optimize();
        }
    }

    /**
     * Determines whether the access request indicated by the
     * specified permission should be allowed or denied, based on
     * the current AccessControlContext and security policy.
     * This method quietly returns if the access request
     * is permitted, or throws an AccessControlException otherwise. The
     * getPermission method of the AccessControlException returns the
     * {@code perm} Permission object instance.
     *
     * @param perm the requested permission.
     *
     * @exception AccessControlException if the specified permission
     *            is not permitted, based on the current security policy.
     * @exception NullPointerException if the specified permission
     *            is {@code null} and is checked based on the
     *            security policy currently in effect.
     */

    public static void checkPermission(Permission perm)
            throws AccessControlException
    {
        //System.err.println("checkPermission "+perm);
        //Thread.currentThread().dumpStack();

        if (perm == null) {
            throw new NullPointerException("permission can't be null");
        }

        AccessControlContext stack = getStackAccessControlContext();
        // if context is null, we had privileged system code on the stack.
        if (stack == null) {
            Debug debug = AccessControlContext.getDebug();
            boolean dumpDebug = false;
            if (debug != null) {
                dumpDebug = !Debug.isOn("codebase=");
                dumpDebug &= !Debug.isOn("permission=") ||
                        Debug.isOn("permission=" + perm.getClass().getCanonicalName());
            }

            if (dumpDebug && Debug.isOn("stack")) {
                Thread.dumpStack();
            }

            if (dumpDebug && Debug.isOn("domain")) {
                debug.println("domain (context is null)");
            }

            if (dumpDebug) {
                debug.println("access allowed "+perm);
            }
            return;
        }

        AccessControlContext acc = stack.optimize();
        acc.checkPermission(perm);
    }
}
