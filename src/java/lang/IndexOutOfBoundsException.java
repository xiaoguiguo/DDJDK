package java.lang;

/**
 * @className: IndexOutOfBoundsException
 * @author: doudou
 * @datetime: 2021/11/1
 * @description: Thrown to indivate that an index of some sort (such as to an array, to a string, or to a vector)
 *      is out of range.
 *      Applications can subclass this class to indicate similar exceptions.
 *      比如：StringIndexOutOfBoundsException
 */
public class IndexOutOfBoundsException extends RuntimeException {
    private static final long serialVersionUID = 2192343167556759436L;

    public IndexOutOfBoundsException() {
        super();
    }

    public IndexOutOfBoundsException(String s) {
        super(s);
    }

    public IndexOutOfBoundsException(int index) {
        super("Index out of range: " + index);
    }
}
