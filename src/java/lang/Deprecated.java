package java.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * @className: Deprecated
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: 带有 {@code @Deprecated} 注释的程序元素是不鼓励程序员使用的。
 *      一个元素可能由于多种原因中的任何一个而被弃用，
 *      例如，它的使用可能会导致错误；
 *      它可能会在未来版本中不兼容地更改或删除；
 *      它已被更新的、通常更可取的替代方案所取代；
 *      或者它已经过时了。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PACKAGE, MODULE, PARAMETER, TYPE})
public @interface Deprecated {

    String since() default "";

    /**
     * 指示带注释的元素在未来版本中是否会被删除。 默认值为 {@code false}。
     */
    boolean forRemoval() default false;
}
