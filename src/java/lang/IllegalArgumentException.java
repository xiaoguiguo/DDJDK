package java.lang;

/**
 * Thrown to indicate that a method has been passed an illegal or inappropriate argument.
 * 非法或不合适的参数 异常
 */
public class IllegalArgumentException extends RuntimeException {
    private static final long serialVersionUID = -2357438994450316900L;

    public IllegalArgumentException() {
        super();
    }

    public IllegalArgumentException(String s) {
        super(s);
    }

    public IllegalArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalArgumentException(Throwable cause) {
        super(cause);
    }
}
