package java.lang;

/**
 * Thrown by the security manager to indicate a security violation.
 * 违反安全规定，抛出异常
 */
public class SecurityException extends RuntimeException {
    private static final long serialVersionUID = -8420198617617652754L;

    public SecurityException() {
        super();
    }

    public SecurityException(String s) {
        super(s);
    }

    public SecurityException(String s, Throwable cause) {
        super(s, cause);
    }

    public SecurityException(Throwable cause) {
        super(cause);
    }
}
