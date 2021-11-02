package sun.security.util;

import java.lang.reflect.ReflectPermission;

/**
 * 用于创建在整个 JDK 中使用的 权限的 "权限常量"和"字符串常量"。
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    public static final ReflectPermission ACCESS_PERMISSION = new ReflectPermission("suppressAccessChecks");

}
