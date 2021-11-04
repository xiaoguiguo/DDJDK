package jdk.internal.reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @className: CallerSensitive
 * @author: doudou
 * @datetime: 2021/11/4
 * @description:
 *      jdk内有些方法，jvm的开发者认为这些方法危险，不希望开发者调用，就把这种危险的方法用 @CallerSensitive修饰，并在“jvm”级别检查。
 *      @CallerSensitive 有个特殊之处，必须由 启动类classloader加载（如rt.jar ），才可以被识别。 所以rt.jar下面的注解可以正常使用。
 *      开发者自己写的@CallerSensitive 不可以被识别。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface CallerSensitive {
}
