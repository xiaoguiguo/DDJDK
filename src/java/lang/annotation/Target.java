package java.lang.annotation;

/**
 * 元注解，注解的注解，用于解释说明
 * 这个注解标识了被修饰注解的作用对象。
 */
@Target(ElementType.ANNOTATION_TYPE)
public @interface Target {

    /**
     * Returns an array of the kinds of elements an annotation type
     * can be applied to.
     */
    ElementType[] value();
}
