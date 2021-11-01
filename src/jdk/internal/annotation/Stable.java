package jdk.internal.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stable注解是JDK内部注解，只对于被Bootstrap加载的类生效；对于被这个注解修饰的变量或者数组，值或其中所有只能被修改一次。
 * 引用类型初始为null，原生类型初始为0，他们能被修改为非null或者非0只能修改一次。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Stable {
}
