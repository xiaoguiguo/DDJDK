package java.lang;

import java.security.Permission;

/**
 * 当运行未知的 Java 程序的时候，该程序可能有恶意代码（删除系统文件、重启系统等），
 * 为了防止运行恶意代码对系统产生影响，需要对运行的代码的权限进行控制，这时候就要启用 Java 安全管理器。
 * $JAVA_HOME/lib/security/default.policy
 */
public class SecurityManager {

    private boolean initialized = false;

    public SecurityManager() {
        synchronized (SecurityManager.class) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(new RuntimePermission("createSecurityManager"));
            }
            initialized = true;
        }
    }

    public void checkPermission(Permission perm) {
        java.security.AccessController.checkPermission(perm);
    }

    public void checkExit(int status) {
        checkPermission(new RuntimePermission("exitVM." + status));
    }
}
