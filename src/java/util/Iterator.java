package java.util;

import java.util.function.Consumer;

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
        throw new UnsupportedOperationException("remove");
    }

    /**
     * 对每个剩余元素执行给定的操作，直到处理完所有元素或操作引发异常。 如果指定了该顺序，则操作按迭代顺序执行。
     * 动作抛出的异常被转发给调用者。
     * 如果操作以任何方式修改集合（即使通过调用 remove 方法或 Iterator 子类型的其他 mutator 方法），迭代器的行为是未指定的，
     * 除非覆盖类已指定并发修改策略。
     * 如果操作引发异常，则迭代器的后续行为是未指定的。
     */
    default void forEachRemaining(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        while (hasNext()) {
            action.accept(next());
        }
    }
}
