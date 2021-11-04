package java.security;

import sun.security.util.Debug;

/**
 * @className: AccessControlContext
 * @author: doudou
 * @datetime: 2021/11/4
 * @description: AccessControlContext 用于根据它封装的上下文做出系统资源访问决策。
 */
public final class AccessControlContext {

    private ProtectionDomain[] context;

    private boolean isPrivileged;
    private boolean isAuthorized = false;


    private AccessControlContext privilegedContext;

    private DomainCombiner combiner = null;

    private Permission[] permissions;
    private AccessControlContext parent;
    private boolean isWrapped;

    private boolean isLimited;
    private ProtectionDomain[] limitedContext;

    private static boolean debugInit = false;
    private static Debug debug = null;

    static Debug getDebug() {
        if (debugInit) return debug;
        else {
            if (Policy.isSet()) {
                debug = Debug.getInstance("access");
                debugInit = true;
            }
            return debug;
        }
    }
}
