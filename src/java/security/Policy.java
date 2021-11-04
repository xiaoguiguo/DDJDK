package java.security;

import sun.security.util.Debug;

import java.util.Enumeration;

/**
 * @className: Policy
 * @author: doudou
 * @datetime: 2021/11/4
 * @description: Policy 对象负责确定在 Java 运行时环境中执行的代码是否具有执行安全敏感操作的权限。
 */
public abstract class Policy {

    public static final PermissionCollection UNSUPPORTED_EMPTY_COLLECTION = new UnsupportedEmptyCollection();

    private static class PolicyInfo {
        final Policy policy;
        final boolean initialized;

        PolicyInfo(Policy policy, boolean initialized) {
            this.policy = policy;
            this.initialized = initialized;
        }
    }

    private static volatile PolicyInfo policyInfo = new PolicyInfo(null, false);

    private static final Debug debug = Debug.getInstance("policy");

    static boolean isSet() {
        PolicyInfo pi = policyInfo;
        return pi.policy != null && pi.initialized == true;
    }

    private static class UnsupportedEmptyCollection extends PermissionCollection {
        private static final long serialVersionUID = -8200585139853482454L;

        private Permissions perms;

        public UnsupportedEmptyCollection() {
            this.perms = new Permissions();
            perms.setReadOnly();
        }

        @Override
        public void add(Permission permission) {
            // TODO
        }

        @Override
        public boolean implies(Permission permission) {
            // TODO
            return false;
        }

        @Override
        public Enumeration<Permission> elements() {
            // TODO
            return null;
        }
    }
}
