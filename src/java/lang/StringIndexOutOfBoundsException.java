package java.lang;

/**
 * @className: StringIndexOutOfBoundsException
 * @author: doudou
 * @datetime: 2021/11/1
 * @description: Thrown by {@code String} methods to indicate that an index is either negative
 *      or greater than the size of the string.
 *      For some methods such as the {@link String#charAt(int)} method.
 */
public class StringIndexOutOfBoundsException extends IndexOutOfBoundsException {
    private static final long serialVersionUID = -3630824386436977814L;

    public StringIndexOutOfBoundsException() {
        super();
    }

    public StringIndexOutOfBoundsException(String s) {
        super(s);
    }

    public StringIndexOutOfBoundsException(int index) {
        super("String index out of range: " + index);
    }
}
