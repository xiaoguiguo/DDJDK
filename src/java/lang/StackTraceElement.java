package java.lang;

import java.io.Serializable;

/**
 * 堆栈跟踪中的元素，由 Throwable.getStackTrace()返回。堆栈跟踪中的元素，由Throwable.getStackTrace()返回。
 * 每个元素代表一个堆栈帧。 除堆栈顶部的堆栈帧之外的所有堆栈帧表示方法调用。 堆栈顶部的框架表示生成堆栈跟踪的执行点。
 * 通常，这是创建与堆栈跟踪相对应的throwable的点。
 */
public class StackTraceElement implements Serializable {
    private static final long serialVersionUID = -5158865646761162074L;

    private transient Class<?> declaringClassObject;

    // Normally initialized by VM
    private String classLoaderName;
    private String moduleName;
    private String moduleVersion;
    private String declaringClass;
    private String methodName;
    private String fileName;
    private int    lineNumber;
    private byte   format = 0; // Default to show all

    public StackTraceElement(String declaringClass, String methodName, String fileName, int lineNumber) {
        this(null, null, null, declaringClass, methodName, fileName, lineNumber);
    }

    public StackTraceElement(String classLoaderName, String moduleName, String moduleVersion, String declaringClass,
                             String methodName, String fileName, int lineNumber) {
        this.classLoaderName = classLoaderName;
        this.moduleName      = moduleName;
        this.moduleVersion   = moduleVersion;
//        this.declaringClass  = Objects
    }
}
