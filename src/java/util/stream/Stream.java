package java.util.stream;

import java.util.function.Predicate;

/**
 * 支持顺序和并行聚合操作的元素序列。
 */
public interface Stream<T> extends BaseStream<T, Stream<T>> {

    /**
     * 返回由与给定 predicate 匹配的此流的元素组成的流。
     */
    Stream<T> filter(Predicate<? super T> predicate);


}
