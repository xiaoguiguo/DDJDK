package java.util;

/**
 * 用于操作或创建 Spliterator 及其原始特化 Spliterator.OfInt、Spliterator.OfLong 和 Spliterator.OfDouble 实例的静态类和方法。
 */
public final class Spliterators {


    public static <T> Spliterator<T> spliteratorUnkonwnSize(Iterator<? extends T> iterator, int characteristics) {
        return new IteratorSpliterator<>(Objects.requireNonNull(iterator), characteristics);
    }
}
