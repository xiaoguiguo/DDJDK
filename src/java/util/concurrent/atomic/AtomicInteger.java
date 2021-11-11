/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.atomic;

import java.lang.invoke.VarHandle;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * 可以原子的更新 int 值。 有关原子访问属性的描述，请参阅 VarHandle 规范。
 * AtomicInteger 用于诸如原子递增计数器之类的应用程序中，并且不能用作 Integer 的替代品。
 * 但是，此类确实扩展了 Number 以允许处理基于数字的类的工具和实用程序进行统一访问。
 */
public class AtomicInteger extends Number implements java.io.Serializable {
    private static final long serialVersionUID = 6214790243416807050L;

    /**
     * 此类旨在使用 VarHandles 实现，但存在未解决的循环启动依赖项。
     */
    private static final jdk.internal.misc.Unsafe U = jdk.internal.misc.Unsafe.getUnsafe();
    private static final long VALUE = U.objectFieldOffset(AtomicInteger.class, "value");

    private volatile int value;

    /**
     * 使用给定的初始值创建一个新的 AtomicInteger。
     */
    public AtomicInteger(int initialValue) {
        value = initialValue;
    }

    /**
     * 构造函数，初始值为0
     */
    public AtomicInteger() {
    }

    /**
     * 返回当前值，具有由 VarHandle.getVolatile 指定的内存效果。
     */
    public final int get() {
        return value;
    }

    /**
     * 将值设置为 newValue，具有 VarHandle.setVolatile 指定的内存效果。
     */
    public final void set(int newValue) {
        value = newValue;
    }

    /**
     * 将值设置为 newValue，具有 VarHandle.setRelease 指定的内存效果。
     */
    public final void lazySet(int newValue) {
        U.putIntRelease(this, VALUE, newValue);
    }

    /**
     * 原子地将值设置为 newValue 并返回旧值，具有 VarHandle.getAndSet 指定的内存效果。
     */
    public final int getAndSet(int newValue) {
        return U.getAndSetInt(this, VALUE, newValue);
    }

    /**
     * 如果当前值 == 预期值，则原子地将值设置为 newValue，具有 VarHandle.compareAndSet 指定的内存效果。
     */
    public final boolean compareAndSet(int expectedValue, int newValue) {
        return U.compareAndSetInt(this, VALUE, expectedValue, newValue);
    }

    /**
     * 如果当前值 == 预期值，则可能以原子方式将该值设置为 newValue，并具有 VarHandle.weakCompareAndSetPlain 指定的内存效果。
     * 已弃用
     * 此方法具有简单的内存效果，但方法名称暗示易失性内存效果（参见 compareAndExchange 和 compareAndSet 等方法）。 
     * 为了避免对普通或易失性内存效果的混淆，建议改用weakCompareAndSetPlain 方法。
     */
    @Deprecated(since="9")
    public final boolean weakCompareAndSet(int expectedValue, int newValue) {
        return U.weakCompareAndSetIntPlain(this, VALUE, expectedValue, newValue);
    }

    /**
     * 如果当前值 == 预期值，则可能以原子方式将该值设置为 newValue，并具有 VarHandle.weakCompareAndSetPlain 指定的内存效果。
     * @since 9
     */
    public final boolean weakCompareAndSetPlain(int expectedValue, int newValue) {
        return U.weakCompareAndSetIntPlain(this, VALUE, expectedValue, newValue);
    }

    /**
     * 原子地递增当前值，具有 VarHandle.getAndAdd 指定的内存效果。
     */
    public final int getAndIncrement() {
        return U.getAndAddInt(this, VALUE, 1);
    }

    /**
     * 原子地递减当前值，具有 VarHandle.getAndAdd 指定的内存效果。
     */
    public final int getAndDecrement() {
        return U.getAndAddInt(this, VALUE, -1);
    }

    /**
     * 以原子方式将给定值添加到当前值，并具有 VarHandle.getAndAdd 指定的内存效果。
     */
    public final int getAndAdd(int delta) {
        return U.getAndAddInt(this, VALUE, delta);
    }

    /**
     * 原子地递增当前值，具有 VarHandle.getAndAdd 指定的内存效果。
     */
    public final int incrementAndGet() {
        return U.getAndAddInt(this, VALUE, 1) + 1;
    }

    /**
     * 原子地递减当前值，具有 VarHandle.getAndAdd 指定的内存效果。
     */
    public final int decrementAndGet() {
        return U.getAndAddInt(this, VALUE, -1) - 1;
    }

    /**
     * 以原子方式将给定值添加到当前值，并具有 VarHandle.getAndAdd 指定的内存效果。
     */
    public final int addAndGet(int delta) {
        return U.getAndAddInt(this, VALUE, delta) + delta;
    }

    /**
     * 原子更新（具有 VarHandle.compareAndSet 指定的内存效果）当前值与应用给定函数的结果，返回前一个值。
     * 该函数应该是无副作用的，因为当尝试更新由于线程之间的争用而失败时，它可能会被重新应用。
     */
    public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) {
                next = updateFunction.applyAsInt(prev);
            }
            if (weakCompareAndSetVolatile(prev, next)) {
                return prev;
            }
            haveNext = (prev == (prev = get()));
        }
    }

    /**
     * 使用给定函数的应用结果原子地更新（使用 VarHandle.compareAndSet 指定的内存效应）当前值，返回更新后的值。 
     * 该函数应该是无副作用的，因为当尝试更新由于线程之间的争用而失败时，它可能会被重新应用。
     * @since 1.8
     */
    public final int updateAndGet(IntUnaryOperator updateFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) {
                next = updateFunction.applyAsInt(prev);
            }
            if (weakCompareAndSetVolatile(prev, next)) {
                return next;
            }
            haveNext = (prev == (prev = get()));
        }
    }

    /**
     * 将给定函数应用于当前值和给定值的结果以原子方式更新（使用 VarHandle.compareAndSet 指定的内存效果）当前值，返回前一个值。
     * 该函数应该是无副作用的，因为当尝试更新由于线程之间的争用而失败时，它可能会被重新应用。
     * 该函数以当前值作为第一个参数，给定的更新作为第二个参数。
     */
    public final int getAndAccumulate(int x, IntBinaryOperator accumulatorFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) {
                next = accumulatorFunction.applyAsInt(prev, x);
            }
            if (weakCompareAndSetVolatile(prev, next)) {
                return prev;
            }
            haveNext = (prev == (prev = get()));
        }
    }

    /**
     * 将给定函数应用于当前值和给定值的结果以原子方式更新（使用 VarHandle.compareAndSet 指定的内存效应）当前值，返回更新后的值。
     * 该函数应该是无副作用的，因为当尝试更新由于线程之间的争用而失败时，它可能会被重新应用。
     * 该函数以当前值作为第一个参数，给定的更新作为第二个参数。
     */
    public final int accumulateAndGet(int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) {
                next = accumulatorFunction.applyAsInt(prev, x);
            }
            if (weakCompareAndSetVolatile(prev, next)) {
                return next;
            }
            haveNext = (prev == (prev = get()));
        }
    }

    /**
     * 当前值的string表示
     * @return the String representation of the current value
     */
    public String toString() {
        return Integer.toString(get());
    }

    /**
     * 以 int 形式返回此 AtomicInteger 的当前值，具有 VarHandle.getVolatile 指定的内存效果。 相当于 get()。
     */
    public int intValue() {
        return get();
    }

    /**
     * 在扩展原始转换后将此 AtomicInteger 的当前值作为 Long 返回，具有 VarHandle.getVolatile 指定的内存效果。
     */
    public long longValue() {
        return (long)get();
    }

    /**
     * 在扩展原始转换后，将此 AtomicInteger 的当前值作为 Float 返回，具有 VarHandle.getVolatile 指定的内存效果。
     */
    public float floatValue() {
        return (float)get();
    }

    /**
     * 在扩展原始转换后将此 AtomicInteger 的当前值作为 Double 返回，具有 VarHandle.getVolatile 指定的内存效果。
     */
    public double doubleValue() {
        return (double)get();
    }

    // jdk9

    /**
     * 返回当前值，具有读取的内存语义，就像变量被声明为 non-volatile。
     * @since 9
     */
    public final int getPlain() {
        return U.getInt(this, VALUE);
    }

    /**
     * 将值设置为 newValue，具有设置的内存语义，就好像变量被声明为 non-volatile和 non-final。
     * @since 9
     */
    public final void setPlain(int newValue) {
        U.putInt(this, VALUE, newValue);
    }

    /**
     * 返回当前值，具有 VarHandle.getOpaque 指定的内存效果。
     * @since 9
     */
    public final int getOpaque() {
        return U.getIntOpaque(this, VALUE);
    }

    /**
     * 将值设置为 newValue，具有 VarHandle.setOpaque 指定的内存效果。
     * @since 9
     */
    public final void setOpaque(int newValue) {
        U.putIntOpaque(this, VALUE, newValue);
    }

    /**
     * 返回当前值，具有 VarHandle.getAcquire 指定的内存效果。
     * @since 9
     */
    public final int getAcquire() {
        return U.getIntAcquire(this, VALUE);
    }

    /**
     * 将值设置为 newValue，具有 VarHandle.setRelease 指定的内存效果。
     * @since 9
     */
    public final void setRelease(int newValue) {
        U.putIntRelease(this, VALUE, newValue);
    }

    /**
     * 如果当前值（称为见证值，== expectedValue）具有由 VarHandle.compareAndExchange 指定的内存效果，
     * 则以原子方式将该值设置为 newValue。
     * @since 9
     */
    public final int compareAndExchange(int expectedValue, int newValue) {
        return U.compareAndExchangeInt(this, VALUE, expectedValue, newValue);
    }

    /**
     * 如果当前值（称为见证值，== expectedValue）具有由 VarHandle.compareAndExchangeAcquire 指定的内存效果，
     * 则以原子方式将该值设置为 newValue。
     * @since 9
     */
    public final int compareAndExchangeAcquire(int expectedValue, int newValue) {
        return U.compareAndExchangeIntAcquire(this, VALUE, expectedValue, newValue);
    }

    /**
     * 如果当前值（称为见证值，== expectedValue）具有由 VarHandle.compareAndExchangeRelease 指定的内存效果，
     * 则以原子方式将该值设置为 newValue。
     * @since 9
     */
    public final int compareAndExchangeRelease(int expectedValue, int newValue) {
        return U.compareAndExchangeIntRelease(this, VALUE, expectedValue, newValue);
    }

    /**
     * 如果当前值 == expectedValue，则可能以原子方式将值设置为 newValue，并具有 VarHandle.weakCompareAndSet 指定的记忆效应。
     * @since 9
     */
    public final boolean weakCompareAndSetVolatile(int expectedValue, int newValue) {
        return U.weakCompareAndSetInt(this, VALUE, expectedValue, newValue);
    }

    /**
     * 如果当前值 == 预期值，则可能以原子方式将值设置为 newValue，并具有 VarHandle.weakCompareAndSetAcquire 指定的记忆效应。
     * @since 9
     */
    public final boolean weakCompareAndSetAcquire(int expectedValue, int newValue) {
        return U.weakCompareAndSetIntAcquire(this, VALUE, expectedValue, newValue);
    }

    /**
     * 如果当前值 == 预期值，则可能以原子方式将值设置为 newValue，并具有 VarHandle.weakCompareAndSetRelease 指定的内存效果。
     * @since 9
     */
    public final boolean weakCompareAndSetRelease(int expectedValue, int newValue) {
        return U.weakCompareAndSetIntRelease(this, VALUE, expectedValue, newValue);
    }

}
