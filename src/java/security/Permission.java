package java.security;

import java.io.Serializable;

/**
 * @className: Permission
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: 表示对系统资源的访问的抽象类。 所有权限都有一个名称（其解释取决于子类），以及用于定义特定权限子类语义的抽象函数。
 *      大多数 Permission 对象还包括一个“操作”列表，该列表告诉该对象允许的操作。
 *      例如，对于 java.io.FilePermission 对象，权限名称是文件（或目录）的路径名，
 *      动作列表（例如“读、写”）指定对指定文件（或 对于指定目录中的文件）。
 *      对于不需要这样的列表的 Permission 对象，例如 java.lang.RuntimePermission，操作列表是可选的；
 *      您要么拥有命名权限（例如“system.exit”），要么没有。
 */
public abstract class Permission implements Guard, Serializable {
    private static final long serialVersionUID = 2890761604664769887L;

    private String name;

    public Permission(String name) {
        this.name = name;
    }

    public void checkGuard(Object object) throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(this);
        }
    }

    /**
     * 校验权限参数对象拥有的权限名和权限操作 和 创建对象时的设置是否一致
     */
    public abstract boolean implies(Permission permission);

    /**
     * 比较两个权限对象的类型、权限名以及权限操作
     */
    public abstract boolean equals(Object obj);

    /**
     * Returns the hash code value for this Permission object.
     */
    public abstract int hashCode();

    /**
     * Returns the name of this Permission.
     * For example, in the case of a java.io.FilePermission, the name will be a pathname.
     */
    public final String getName() {
        return name;
    }

    /**
     * 返回创建对象时设置的权限操作，未设置返回空字符串
     */
    public abstract String getActions();

    /**
     * 为给定的 Permission 对象返回一个空的 PermissionCollection，如果未定义，则返回 null。
     * 如果 Permission 类的子类需要将其权限存储在特定的 PermissionCollection 对象中，
     * 以便在调用 PermissionCollection.implies 方法时提供正确的语义，则它们应该覆盖它。
     * 如果返回 null，则此方法的调用者可以自由地将这种类型的权限存储在他们选择的任何
     * PermissionCollection 中（使用 Hashtable 的一个，使用 Vector 的一个等）。
     */
    public PermissionCollection newPermissionCollection() {
        return null;
    }

    /**
     * 返回描述此权限的字符串。 约定是按以下格式指定类名称、权限名称和动作：'("ClassName" "name" "actions")'，
     * 或 '("ClassName" "name")' 如果动作列表是 空或空。
     */
    public String toString() {
        String actions = getActions();
        if (actions == null || actions.isEmpty()) {
            return "(\"" + getClass().getName() + "\" \"" + name + "\"";
        } else {
            return "(\"" + getClass().getName() + "\" \"" + name +
                    "\" \"" + actions + "\")";
        }
    }

}
