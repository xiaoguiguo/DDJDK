package java.security;

import java.io.Serializable;

/**
 * @className: BasicPermission
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: BasicPermission 类扩展了 Permission 类，并且可以用作希望遵循与 BasicPermission 相同的命名约定的权限的基类。
 */
public class BasicPermission extends Permission implements Serializable {
    private static final long serialVersionUID = 411692434087573169L;

    private void init(String name) {
        // TODO
    }

    public BasicPermission(String name) {
        super(name);
        init(name);
    }

    public boolean implies(Permission permission) {
        // TODO
        return false;
    }

    public boolean equals(Object obj) {
        // TODO
        return false;
    }

    public int hashCode() {
        return this.getName().hashCode();
    }

    public String getActions() {
        return "";
    }

    public BasicPermission(String name, String actions) {
        super(name);
        init(name);
    }
}
