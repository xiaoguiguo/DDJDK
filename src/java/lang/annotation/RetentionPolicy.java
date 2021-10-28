package java.lang.annotation;

public enum RetentionPolicy {

    /**
     * Annotations are to be discarded by the compiler.
     *
     * 注解保留在源代码中，但是编译的时候会被编译器所丢弃。比如@Override, @SuppressWarnings
     */
    SOURCE,

    /**
     * Annotations are to be recorded in the class file by the compiler
     * but need not be retained by the VM at run time. This is the default behavior.
     *
     * 这是默认的policy。注解会被保留在class文件中，但是在运行时期间就不会识别这个注解。
     */
    CLASS,

    /**
     * Annotations are to be recorded in the class file by the compiler and
     * retained by the VM at run time, so they may be read reflectively.
     *
     * 注解会被保留在class文件中，同时运行时期间也会被识别。所以可以使用反射机制获取注解信息。比如@Deprecated
     */
    RUNTIME
}