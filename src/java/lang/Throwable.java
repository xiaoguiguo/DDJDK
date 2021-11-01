package java.lang;

import java.io.Serializable;

/**
 * 异常相关类和接口的关联关系，可以参考 /docs/images/exception.png.
 * Throwable类是Java语言中所有错误(Error)和异常(Exception)的超类。
 * 只有作为此类（或其子类之一）的实例的对象throw Java虚拟机抛出，
 * 或者可以由Java throw语句抛出。 类似地，只有这个类或其子类之一可以是catch子句中的参数类型。 出于编译时检查异常的目的， Throwable和任何子类Throwable （也不是RuntimeException或Error的子类）都被视为已检查的例外。
 */
public class Throwable implements Serializable {
    private static final long serialVersionUID = -5483213135545696013L;

    /** The JVM saves some indication of the stack backtrace in this slot. */
    private transient Object backtrace;

    /** Specific details about the Throwable. */
    private String detailMessage;

    private Throwable cause = this;

    private static final StackTraceElement[] UNASSIGNED_STACK = new StackTraceElement[0];

    /**
     * Throwable类的setStackTrace(StackTraceElement [] stackTrace)方法用于将堆栈跟踪元素设置为此可抛出对象，
     * 并且该堆栈跟踪将由getStackTrace()返回并由printStackTrace()和相关方法打印。
     * 使用此方法，用户可以覆盖默认的堆栈跟踪，该默认堆栈跟踪在构造throwable时由fillInStackTrace()生成，
     * 或者在从序列化流中读取throwable时反序列化。
     */
    private StackTraceElement[] stackTrace = UNASSIGNED_STACK;

    private static class SentineHolder {

    }

    public Throwable() {
        fillInStackTrace();
    }

    public Throwable(String message) {
        fillInStackTrace();
        detailMessage = message;
    }

    public Throwable(String message, Throwable cause) {
        fillInStackTrace();
        detailMessage = message;
        this.cause = cause;
    }

    public Throwable(Throwable cause) {
        fillInStackTrace();
        detailMessage = (cause == null ? null : cause.toString());
        this.cause = cause;
    }

    public Throwable(String message, Throwable cause, boolean enableSuppression,
                     boolean writableStackTrace) {
        if (writableStackTrace) {
            fillInStackTrace();
        } else {
            stackTrace = null;
        }
        detailMessage = message;
        this.cause = cause;
//        if (!enableSuppression) {
//            suppressedExceptions = null;
//        }
    }

    public synchronized Throwable fillInStackTrace() {
        if (stackTrace != null || backtrace != null) {
            fillInStackTrace(0);
            stackTrace = UNASSIGNED_STACK;
        }
        return this;
    }

    private native Throwable fillInStackTrace(int dummy);



    public String toString() {
//        String s = getClass()
        return "";
    }

}
