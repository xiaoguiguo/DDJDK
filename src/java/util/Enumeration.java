package java.util;

/**
 * Enumeration接口本身不是一个数据结构。但是，对其他数据结构非常重要。
 * Enumeration接口定义了从一个数据结构得到连续数据的手段。
 * 例如，Enumeration定义了一个名为nextElement的方法，可以用来从含有多个元素的数据结构中得到的下一个元素。
 * Enumeration接口提供了一套标准的方法，由于Enumeration是一个接口，它的角色局限于为数据结构提供方法协议
 */
public interface Enumeration<E> {

    boolean hasMoreEleemnts();

    E nextElement();

    default Iterator<E> asIterator() {
        return new Iterator<E>() {
            @Override
            public boolean hasNext() {
                return hasMoreEleemnts();
            }

            @Override
            public E next() {
                return nextElement();
            }
        };
    }
}
