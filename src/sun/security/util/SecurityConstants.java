package sun.security.util;

import java.lang.reflect.ReflectPermission;
import java.security.AllPermission;

/**
 * 用于创建在整个 JDK 中使用的 权限的 "权限常量"和"字符串常量"。
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    /**
     * SecurityManager 使用的权限操作的常用字符串常量。
     * 在 FilePermission、SocketPermission 和 PropertyPermission 中检查权限时在此处声明快捷方式。
     */

    /**
     * 文件的 删除、执行、可读、可写、读取链接 的权限
     * readlink：读取链接权限，允许通过调用Java.nio.file.Files中的readSymbolicLink方法来读取链接对象
     */
    public static final String FILE_DELETE_ACTION           = "delete";
    public static final String FILE_EXECUTE_ACTION          = "execute";
    public static final String FILE_READ_ACTION             = "read";
    public static final String FILE_WRITE_ACTION            = "write";
    public static final String FILE_READLINK_ACTION         = "readlink";

    /**
     * socket 解析、连接、监听、接收 的权限
     */
    public static final String SOCKET_RESOLVE_ACTION        = "resolve";
    public static final String SOCKET_CONNECT_ACTION        = "connect";
    public static final String SOCKET_LISTEN_ACTION         = "listen";
    public static final String SOCKET_ACCEPT_ACTION         = "accept";
    public static final String SOCKET_CONNECT_ACCEPT_ACTION = "connect,accept";

    /**
     * 属性的 读、写 的权限
     */
    public static final String PROPERTY_RW_ACTION           = "read,write";
    public static final String PROPERTY_READ_ACTION         = "read";
    public static final String PROPERTY_WRITE_ACTION        = "write";

    /**
     * JDK 中各种 checkPermission() 调用中使用的权限常量。
     * java.lang.Class, java.lang.SecurityManager, java.lang.System,
     * java.net.URLConnection
     * java.security.AllPermission, java.security.Policy, java.security.provider.PolicyFile
     */
    public static final AllPermission ALL_PERMISSION = new AllPermission();

    public static final NetPermission SPECIFY_HANDLER_PERMISSION = new NetPermission("setProxySelector");

    public static final ReflectPermission ACCESS_PERMISSION = new ReflectPermission("suppressAccessChecks");

}
