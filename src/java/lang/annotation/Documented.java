package java.lang.annotation;

/**
 * 元注解，注解的注解，用于解释说明
 * 被 @Documented 修饰的注解将包含在 JavaDoc 中 。此注解会被javadoc工具提取成文档。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Documented {
}
