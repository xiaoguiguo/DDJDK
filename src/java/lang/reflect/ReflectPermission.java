package java.lang.reflect;

import java.security.BasicPermission;

/**
 * @className: ReflectPermission
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: 反射操作的权限类。
 */
public final class ReflectPermission extends BasicPermission {
    private static final long serialVersionUID = -5129041562248879607L;

    public ReflectPermission(String name) {
        super(name);
    }


}
