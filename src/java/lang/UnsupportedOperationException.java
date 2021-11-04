package java.lang;

/**
 * 请求操作不支持
 */
public class UnsupportedOperationException extends RuntimeException {
    private static final long serialVersionUID = -8916241159644254403L;

    public UnsupportedOperationException() {}

    public UnsupportedOperationException(String s) {
        super(s);
    }

    public UnsupportedOperationException(String s, Throwable cause) {
        super(s, cause);
    }

    public UnsupportedOperationException(Throwable cause) {
        super(cause);
    }

}
