package java.lang;

/**
 * @className: Void
 * @author: doudou
 * @datetime: 2021/11/1
 * @description: The {@code Void} class is an uninstantiable placeholder class
 *      to hold a reference to the {@code Class} object representing the Java Keyword void.
 */
public final class Void {

    /**
     * Class对象表示关键字 void 对应的伪类型
     */
    public static final Class<Void> TYPE = (Class<Void>) Class.getPrimitiveClass("void");

    private Void() {}
}
