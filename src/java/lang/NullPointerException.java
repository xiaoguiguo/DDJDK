package java.lang;

/**
 * Thrown when an application attempts to use null in a case where an object is required.
 */
public class NullPointerException extends RuntimeException {
    private static final long serialVersionUID = -98353407328152586L;

    public NullPointerException() {
        super();
    }

    public NullPointerException(String s) {
        super(s);
    }
}
