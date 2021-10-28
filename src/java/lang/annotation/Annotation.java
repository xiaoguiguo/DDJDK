package java.lang.annotation;

public interface Annotation {

    /**
     * 如果指定的对象表示一个在逻辑上与此相同的注解，则返回 true。
     * 换句话说，如果指定的对象是与此实例具有相同注解类型的实例，
     * 并且其所有成员都等于此注解的对应成员，则返回 true
     */
    boolean equals(Object obj);

    /** the hash code of this annotation */
    int hashCode();

    /** a string representation of this annotation */
    String toString();

    /** return the annotation type of this annotation */
    Class<? extends Annotation> annotationType();
}
