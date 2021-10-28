package java.lang.annotation;

/**
 * 元注解，注解的注解，用于解释说明
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Retention {

    /** Returns the retention policy */
    RetentionPolicy value();
}
