package java.lang.reflect;

import java.lang.annotation.Annotation;

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
}
