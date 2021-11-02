package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;

/**
 * Class类的对象是在当各类被调入时，由 Java 虚拟机自动创建 Class 对象，
 * 或通过类装载器中的 defineClass 方法生成
 */
public final class Class<T> implements Serializable, GenericDeclaration, Type, AnnotatedElement {

    private static final int ANNOTATION = 0x00002000;
    private static final int Enum       = 0x00004000;
    private static final int SYNTHETIC  = 0x00001000;

    private static native void registerNatives();
    static {
        registerNatives();
    }

    private final Class<?> componentType;

    // set by VM
    private transient Module module;

    /**
     * 由JVM初始化，而不是通过私有构造方法
     */
    private final ClassLoader classLoader;

    static native Class<Void> getPrimitiveClass(String name);

    private Class(ClassLoader loader, Class<?> arrayComponentType) {
        classLoader = loader;
        componentType = arrayComponentType;
    }

    public Class<?> getComponentType() {
        if (isArray()) {
            return componentType;
        } else {
            return null;
        }
    }

    /**
     * Determines if this {@code Class} object represents an array class.
     */
    @HotSpotIntrinsicCandidate
    public native boolean isArray();
}
