package java.security;

/**
 * @className: ProtectionDomain
 * @author: doudou
 * @datetime: 2021/11/4
 * @description:
 * ProtectionDomain类封装了一个域的特性,它封装了一组类,这些类的实例在代表一组给定的Principals执行时被授予一组权限。
 * 在构建ProtectionDomain时,可以将一组静态的权限绑定到ProtectionDomain上;无论有效的Policy是什么,这种权限都会被授予该域。
 * 然而,为了支持动态的安全策略,也可以构建一个ProtectionDomain,每当检查一个权限时,它就会被当前的策略动态地映射到一组权限上。
 */
public class ProtectionDomain {
}
