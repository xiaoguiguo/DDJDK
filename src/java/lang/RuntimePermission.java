package java.lang;

import java.security.BasicPermission;

/**
 * @className: RuntimePermission
 * @author: doudou
 * @datetime: 2021/11/4
 * @description: 类是运行时的权限。通过RuntimePermission包含一个名称(也称为“目标名称”)，但没有动作列表; 要么有指定权限，也可以不用指定。
 */
public final class RuntimePermission extends BasicPermission {
    private static final long serialVersionUID = -2435347725496004562L;

    public RuntimePermission(String name) {
        super(name);
    }

    public RuntimePermission(String name, String actions) {
        super(name, actions);
    }
}
