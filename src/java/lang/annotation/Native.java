package java.lang.annotation;

/**
 * @className: Native
 * @author: doudou
 * @datetime: 2021/11/1
 * @description: 表示可以从本机代码引用定义常量值的字段，表示被本地代码引用。
 *      该注解可以用作生成本机头文件的工具的提示，以确定是否需要头文件，如果需要，应包含哪些声明
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Native {
}
