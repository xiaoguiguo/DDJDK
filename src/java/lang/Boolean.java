package java.lang;

import java.io.Serializable;

/**
 * @className: Boolean
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: Boolean class wraps a value of the primitive type.
 */
public final class Boolean implements Serializable, Comparable<Boolean> {
    private static final long serialVersionUID = 153600892495152050L;

    public static final Boolean TRUE = new Boolean(true);

    public static final Boolean FALSE = new Boolean(false);

    private final boolean value;

    @SuppressWarnings("unchecked")
    public static final Class<Boolean> TYPE = (Class<Boolean>)Class.getPrimitiveClass("boolean");

    @Deprecated(since = "9")
    public Boolean(boolean value) {
        this.value = value;
    }

    @Deprecated(since = "9")
    public Boolean(String s) {
        this(parseBoolean(s));
    }

    public static boolean parseBoolean(String s) {
        return "true".equalsIgnoreCase(s);
    }

    public int compareTo(Boolean b) {
        return compare(this.value, b.value);
    }

    public static int compare(boolean x, boolean y) {
        return (x == y) ? 0 : (x ? 1 : -1);
    }

}
