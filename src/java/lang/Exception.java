package java.lang;

public class Exception extends Throwable {
    private static final long serialVersionUID = 5130716894769011670L;

    /** 构造一个新的异常，并设置detail message为null */
    public Exception() {
        super();
    }

    public Exception(String message) {
        super(message);
    }

    public Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public Exception(Throwable cause) {
        super(cause);
    }

    protected Exception(String message, Throwable cause, boolean enableSuppression,
                        boolean writableStackTrack) {
        super(message, cause, enableSuppression, writableStackTrack);
    }

}
