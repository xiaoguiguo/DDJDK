package java.lang.reflect;

import sun.reflect.annotation.AnnotationSupport;
import sun.reflect.annotation.AnnotationType;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @className: AnnotatedElement
 * @author: doudou
 * @datetime: 2021/10/31
 * @description: AnnotatedElement 接口是所有程序元素（Class、Method和Constructor）的父接口，
 * 所以程序通过反射获取了某个类的AnnotatedElement对象之后，程序就可以调用该对象的方法来访问Annotation信息。
 *
 * 表示当前在此VM中运行的程序的带注释的元素。
 * 该接口允许以反射方式读取注释。 此接口中的方法返回的所有注释都是不可变的和可序列化的。
 * 通过此接口的方法返回的数组可以由调用者修改，而不会影响返回给其他调用者的数组
 */
public interface AnnotatedElement {

    /**
     * 如果此元素上存在指定类型的注解，则返回 true，否则返回 false。
     * 此方法主要是为了方便访问标记注解而设计的。
     */
    default boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    /**
     * 返回指定类型的注解，如果不存在，return null;
     */
    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    /**
     * 返回该元素上的所有注解
     * @return
     */
    Annotation[] getAnnotations();

    default <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        /*
         * Definition of associated: directly or indirectly present OR
         * neither directly nor indirectly present AND the element is
         * a Class, the annotation type is inheritable, and the
         * annotation type is associated with the superclass of the
         * element.
         */
        T[] result = getDeclaredAnnotationsByType(annotationClass);

        if (result.length == 0 && // Neither directly nor indirectly present
                this instanceof Class && // the element is a class
                AnnotationType.getInstance(annotationClass).isInherited()) { // Inheritable
            Class<?> superClass = ((Class<?>) this).getSuperclass();
            if (superClass != null) {
                // Determine if the annotation is associated with the
                // superclass
                result = superClass.getAnnotationsByType(annotationClass);
            }
        }

        return result;
    }

    /**
     * Returns this element's annotation for the specified type if
     * such an annotation is <em>directly present</em>, else null.
     *
     * This method ignores inherited annotations. (Returns null if no
     * annotations are directly present on this element.)
     *
     * @implSpec The default implementation first performs a null check
     * and then loops over the results of {@link
     * #getDeclaredAnnotations} returning the first annotation whose
     * annotation type matches the argument type.
     *
     * @param <T> the type of the annotation to query for and return if directly present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return this element's annotation for the specified annotation type if
     *     directly present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    default <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        // Loop over all directly-present annotations looking for a matching one
        for (Annotation annotation : getDeclaredAnnotations()) {
            if (annotationClass.equals(annotation.annotationType())) {
                // More robust to do a dynamic cast at runtime instead
                // of compile-time only.
                return annotationClass.cast(annotation);
            }
        }
        return null;
    }

    /**
     * Returns this element's annotation(s) for the specified type if
     * such annotations are either <em>directly present</em> or
     * <em>indirectly present</em>. This method ignores inherited
     * annotations.
     *
     * If there are no specified annotations directly or indirectly
     * present on this element, the return value is an array of length
     * 0.
     *
     * The difference between this method and {@link
     * #getDeclaredAnnotation(Class)} is that this method detects if its
     * argument is a <em>repeatable annotation type</em> (JLS 9.6), and if so,
     * attempts to find one or more annotations of that type by "looking
     * through" a container annotation if one is present.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @implSpec The default implementation may call {@link
     * #getDeclaredAnnotation(Class)} one or more times to find a
     * directly present annotation and, if the annotation type is
     * repeatable, to find a container annotation. If annotations of
     * the annotation type {@code annotationClass} are found to be both
     * directly and indirectly present, then {@link
     * #getDeclaredAnnotations()} will get called to determine the
     * order of the elements in the returned array.
     *
     * <p>Alternatively, the default implementation may call {@link
     * #getDeclaredAnnotations()} a single time and the returned array
     * examined for both directly and indirectly present
     * annotations. The results of calling {@link
     * #getDeclaredAnnotations()} are assumed to be consistent with the
     * results of calling {@link #getDeclaredAnnotation(Class)}.
     *
     * @param <T> the type of the annotation to query for and return
     * if directly or indirectly present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return all this element's annotations for the specified annotation type if
     *     directly or indirectly present on this element, else an array of length zero
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    default <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        return AnnotationSupport.
                getDirectlyAndIndirectlyPresent(Arrays.stream(getDeclaredAnnotations()).
                                collect(Collectors.toMap(Annotation::annotationType,
                                        Function.identity(),
                                        ((first,second) -> first),
                                        LinkedHashMap::new)),
                        annotationClass);
    }

    /**
     * Returns annotations that are <em>directly present</em> on this element.
     * This method ignores inherited annotations.
     *
     * If there are no annotations <em>directly present</em> on this element,
     * the return value is an array of length 0.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations directly present on this element
     * @since 1.5
     */
    Annotation[] getDeclaredAnnotations();
}
