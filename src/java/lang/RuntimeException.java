package java.lang;

/**
 * RuntimeException is the superclass of those exceptions that can be thrown
 * during the normal operation of the Java Virtual Machine.
 * 运行时异常就是在应用程序运行期间抛出的异常。
 */
public class RuntimeException extends Exception {
    private static final long serialVersionUID = -8988075809376521002L;

    public RuntimeException() {
        super();
    }

    public RuntimeException(String message) {
        super(message);
    }

    public RuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeException(Throwable cause) {
        super(cause);
    }

    /**
     * enableSuppression – 是否启用抑制
     * writableStackTrace – 堆栈跟踪是否应该是可写的
     *
     * 构造具有指定详细消息的新throwable，原因是，启用或禁用了suppression ，并启用或禁用了可写堆栈跟踪。
     * 如果禁用抑制， 则此对象的getSuppressed()将返回零长度数组，并且调用addSuppressed(java.lang.Throwable) ，
     * 否则将对抑制列表附加异常将不起作用。 如果写的堆栈跟踪是假的，这个构造不会叫fillInStackTrace() ，
     * 一个null将被写入到stackTrace领域，后续调用fillInStackTrace和setStackTrace(StackTraceElement[])不会设置堆栈跟踪。
     * 如果可写堆栈跟踪为false，则getStackTrace()将返回零长度数组。
     *
     * 请注意， Throwable的其他构造Throwable将抑制视为已启用且堆栈跟踪为可写。
     * Throwable子类应记录禁用抑制的任何条件以及堆栈跟踪不可写的文档条件。
     * 禁用抑制仅应在存在特殊要求的特殊情况下发生，例如虚拟机在低内存情况下重用异常对象。
     * 重复捕获和重新生成给定异常对象的情况，例如在两个子系统之间实现控制流，是另一种情况，其中不可变的throwable对象是合适的。
     */
    protected RuntimeException(String message, Throwable cause, boolean enableSuppression,
                               boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
