package java.security;

/**
 * AllPermission 包含了所有其他的权限
 */
public final class AllPermission extends Permission {
    private static final long serialVersionUID = -3893922004836967809L;

    public AllPermission() {
        super("<all permissions>");
    }

    public AllPermission(String name, String actions) {
        this();
    }

    public boolean implies(Permission permission) {
        return true;
    }

    public boolean equals(Object obj) {
        return obj instanceof AllPermission;
    }

    public int hashCode() {
        return 1;
    }

    public String getActions() {
        return "<all actions>";
    }
}
