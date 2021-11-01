package java.lang;

/**
 * @className: NegativeArraySizeException
 * @author: doudou
 * @datetime: 2021/11/1
 * @description: Thrown if an application tries to create an array with negative size.
 */
public class NegativeArraySizeException extends RuntimeException {
    private static final long serialVersionUID = 300142307167535130L;

    public NegativeArraySizeException() {
        super();
    }

    public NegativeArraySizeException(String s) {
        super(s);
    }
}
