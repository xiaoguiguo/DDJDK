package java.lang.reflect;

import sun.security.util.SecurityConstants;

/**
 * AccessibleObject是一个普通Java类，实现了AnnotatedElement接口，
 * 但是对应AnnotatedElement的非默认方法的实现都是直接抛异常，
 * 也就是AnnotatedElement的接口方法必须由AccessibleObject的子类去实现，
 *
 * AccessibleObject 类是 Field、Method 和 Constructor 对象（称为反射对象）的基类。
 * 它提供了在使用时将反射对象标记为禁止检查 Java 语言访问控制的能力。
 * 这允许具有足够特权的复杂应用程序（例如 Java 对象序列化或其他持久性机制）以通常被禁止的方式操作对象。
 *
 * 一般而言，我们需要通过getModifiers()方法判断修饰符是否public，
 * 如果是非public，则需要调用setAccessible(true)进行修饰符抑制，否则会因为无权限访问会抛出异常
 */
public class AccessibleObject implements AnnotatedElement {

    static void checkPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SecurityConstants.ACCESS_PERMISSION);
        }
    }

}
