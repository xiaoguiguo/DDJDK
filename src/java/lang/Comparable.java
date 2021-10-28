package java.lang;

/**
 * 自然排序：Comparable接口强行对实现它的类的对象进行整体排序，这样的排序称为该类的自然排序；
 * 自然排序与equals一致：类A 对于每一个 o1 和 o2 来说，当且仅当 ( o1.compareTo( o2 ) )与 o1.equals( o2 )具有相同的 布尔值 时，
 * 类A的自然排序才叫做与equals一致。
 */
public interface Comparable<T> {

    /**
     * 负整数、零或正整数，根据此对象是小于、等于还是大于指定对象
     */
    public int compareTo(T o);
}
