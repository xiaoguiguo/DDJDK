package java.util;

import java.util.function.Consumer;

/**
 * Spliterator是一个可分割迭代器(splitable iterator)
 * since 8,对于并行处理的能力大大增强，Spliterator就是为了并行遍历元素而设计的一个迭代器，
 * jdk1.8+中的集合框架中的数据结构都默认实现了spliterator
 *
 * 遍历和分割一个数据源中的元素的对象。分割迭代器覆盖的数据源可以是数组，集合，IO通道或生成器函数。
 *
 * 一个分割迭代器可以单独地(tryAdvance())或者顺序地按块(forEachRemaining())遍历元素。
 *
 * 分割迭代器还可以将其某些元素分区（使用trySplit（））作为另一个分割迭代器，以用于可能的并行操作中。
 *
 * 一个分割迭代器需要报告一组特性值(characteristics())，特性值是关于数据源的结构，源，元素的。
 * 目前的特性值包含8个：ORDERED, DISTINCT, SORTED, SIZED, NONNULL, IMMUTABLE, CONCURRENT, and SUBSIZED.
 * 这些报告的特性值可能被Spliterator客户端用来特殊处理：特殊化处理或简化计算。
 * 例如：一个Collection的分割迭代器应该报告SIZED特性；一个Set的分割迭代器应该报告DISTINCT特性；
 * 一个SortedSet的分割迭代器应该报告SORTED特性。
 *
 * SORTED特性和ORDERED特性。它们代表的含义是不同的。
 * SORTED特性代表的是这个源是被排过序的(如：按年龄大小顺序排过序)；ORDERED特性代表的是这个源是有序的(如：ArrayList)。
 */
public interface Spliterator<T> {

    public static final int ORDERED     = 0x00000010;
    public static final int DISTINCE    = 0x00000001;
    public static final int SORTED      = 0x00000004;
    public static final int SIZED       = 0x00000040;
    public static final int NONNULL     = 0x00000100;
    public static final int IMMUTABLE   = 0x00000400;
    public static final int CONCURRENT  = 0x00001000;
    public static final int SUBSIZED    = 0x00004000;

    /**
     * 如果存在剩余元素，则对其执行给定的操作，返回 true； 否则返回 false。
     * 如果此 Spliterator 是 ORDERED，则按顺序对下一个元素执行操作。 动作抛出的异常被转发给调用者。
     */
    boolean tryAdvance(Consumer<? super T> action);

    /**
     * 在当前线程中按顺序为每个剩余元素执行给定的操作，直到处理完所有元素或操作引发异常。
     * 如果此 Spliterator 是 ORDERED，则操作按顺序执行。 动作抛出的异常被转发给调用者。
     */
    default void forEachRemaining(Consumer<? super T> action) {
        do {} while (tryAdvance(action));
    }

    /**
     * TODO
     */
    Spliterator<T> trySplit();

    /**
     * TODO
     */
    long estimateSize();

    /**
     *
     */
    int characteristics();


}
