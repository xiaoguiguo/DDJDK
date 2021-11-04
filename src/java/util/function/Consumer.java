package java.util.function;

import java.util.Objects;

/**
 * 代表了一个操作，接收了一个参数，并且不返回结果，
 * 不同于大多数其它的函数式接口，Consumer接口期望通过负作用去操作。（也就是说，它可能会操作传入的参数，这里就是它所说的负作用。）
 */
public interface Consumer<T> {

    /**
     * Performs this operation on the given argument.
     * @param t
     */
    void accept(T t);

    /**
     * 返回一个组合的 Consumer，它依次执行此操作和之后的操作。
     * 如果执行任一操作引发异常，则将其转发给组合操作的调用者。
     * 如果执行此操作抛出异常，则不会执行 after 操作。
     */
    default Consumer<T> andThen(Consumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> {
            accept(t);
            after.accept(t);
        };
    }
}
