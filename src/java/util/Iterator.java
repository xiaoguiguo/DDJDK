package java.util;

/**
 * 集合上的迭代器。 Iterator取代了Java Collections Framework中的Enumeration 。
 * 迭代器在两个方面与枚举不同：
 *  迭代器允许调用者在迭代期间使用明确定义的语义从底层集合中删除元素。
 *  方法名称已得到改进。
 */
public interface Iterator<E> {

    boolean hasNext();

    E next();

    default void remove() {
//        throw new
    }
}
