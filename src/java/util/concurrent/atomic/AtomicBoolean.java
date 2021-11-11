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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * 提供了可以原子读取和写入的底层布尔值的操作，并且还包含高级原子操作。
 */
public class AtomicBoolean implements java.io.Serializable {
    private static final long serialVersionUID = 4654671469794556979L;
    private static final VarHandle VALUE;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            VALUE = l.findVarHandle(AtomicBoolean.class, "value", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private volatile int value;

    /**
     * 根据指定boolean值，构造函数
     */
    public AtomicBoolean(boolean initialValue) {
        value = initialValue ? 1 : 0;
    }

    /**
     * 构造函数
     */
    public AtomicBoolean() {
    }

    /**
     * 返回当前值
     */
    public final boolean get() {
        return value != 0;
    }

    /**
     * 如果当前值==期望值，则将该值原子设置为给定的更新值。
     */
    public final boolean compareAndSet(boolean expectedValue, boolean newValue) {
        return VALUE.compareAndSet(this,
                                   (expectedValue ? 1 : 0),
                                   (newValue ? 1 : 0));
    }

    /**
     * 如果当前值 {== expectedValue}，则可能将值原子地设置为 {newValue}，具有 {VarHandleakCompareAndSetPlain} 指定的内存效果。
     */
    @Deprecated(since="9")
    public boolean weakCompareAndSet(boolean expectedValue, boolean newValue) {
        return VALUE.weakCompareAndSetPlain(this,
                                            (expectedValue ? 1 : 0),
                                            (newValue ? 1 : 0));
    }

    /**
     * Possibly atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue},
     * with memory effects as specified by {@link VarHandle#weakCompareAndSetPlain}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     * @since 9
     */
    public boolean weakCompareAndSetPlain(boolean expectedValue, boolean newValue) {
        return VALUE.weakCompareAndSetPlain(this,
                                            (expectedValue ? 1 : 0),
                                            (newValue ? 1 : 0));
    }

    /**
     * 无条件地设置为给定的值
     */
    public final void set(boolean newValue) {
        value = newValue ? 1 : 0;
    }

    /**
     * 最终设定为给定值。
     */
    public final void lazySet(boolean newValue) {
        VALUE.setRelease(this, (newValue ? 1 : 0));
    }

    /**
     * 将原子设置为给定值并返回上一个值。
     */
    public final boolean getAndSet(boolean newValue) {
        return (int)VALUE.getAndSet(this, (newValue ? 1 : 0)) != 0;
    }

    /**
     * 当前值的string表示
     */
    public String toString() {
        return Boolean.toString(get());
    }

    // jdk9

    /**
     * 返回当前值，具有读取的内存语义，就像变量被声明为 non-volatile一样。
     * @since 9
     */
    public final boolean getPlain() {
        return (int)VALUE.get(this) != 0;
    }

    /**
     * 将值设置为 {newValue}，具有设置的内存语义，就好像变量被声明为非 {volatile} 和非 {final}。
     */
    public final void setPlain(boolean newValue) {
        VALUE.set(this, newValue ? 1 : 0);
    }

    /**
     * 返回当前值，具有 VarHandle.getOpaque(java.lang.Object...)指定的内存效果。
     * 参见： VarHandle类
     * @since 9
     */
    public final boolean getOpaque() {
        return (int)VALUE.getOpaque(this) != 0;
    }

    /**
     * 将值设置为 newValue，具有 VarHandle#setOpaque 指定的内存效果。
     *
     * @param newValue the new value
     * @since 9
     */
    public final void setOpaque(boolean newValue) {
        VALUE.setOpaque(this, newValue ? 1 : 0);
    }

    /**
     * 返回当前值，具有 VarHandle.getAcquire 指定的内存效果。
     * @since 9
     */
    public final boolean getAcquire() {
        return (int)VALUE.getAcquire(this) != 0;
    }

    /**
     * 将值设置为 newValue，具有 VarHandle.setRelease 指定的内存效果。
     * @since 9
     */
    public final void setRelease(boolean newValue) {
        VALUE.setRelease(this, newValue ? 1 : 0);
    }

    /**
     * 如果当前值（称为见证值，== expectedValue）具有由 VarHandle.compareAndExchange 指定的内存效果，则以原子方式将该值设置为 newValue。
     * @since 9
     */
    public final boolean compareAndExchange(boolean expectedValue, boolean newValue) {
        return (int)VALUE.compareAndExchange(this,
                                             (expectedValue ? 1 : 0),
                                             (newValue ? 1 : 0)) != 0;
    }

    /**
     * 如果当前值（称为见证值，== expectedValue）具有由 VarHandle.compareAndExchangeAcquire 指定的内存效果，则以原子方式将该值设置为 newValue。
     * @since 9
     */
    public final boolean compareAndExchangeAcquire(boolean expectedValue, boolean newValue) {
        return (int)VALUE.compareAndExchangeAcquire(this,
                                                    (expectedValue ? 1 : 0),
                                                    (newValue ? 1 : 0)) != 0;
    }

    /**
     * 如果当前值（称为见证值，== expectedValue）具有由 VarHandle.compareAndExchangeRelease 指定的内存效果，则以原子方式将该值设置为 newValue。
     * @since 9
     */
    public final boolean compareAndExchangeRelease(boolean expectedValue, boolean newValue) {
        return (int)VALUE.compareAndExchangeRelease(this,
                                                    (expectedValue ? 1 : 0),
                                                    (newValue ? 1 : 0)) != 0;
    }

    /**
     * 如果当前值 == 预期值，则可能以原子方式将值设置为 newValue，并具有 VarHandle.weakCompareAndSet 指定的内存效果。
     * @since 9
     */
    public final boolean weakCompareAndSetVolatile(boolean expectedValue, boolean newValue) {
        return VALUE.weakCompareAndSet(this,
                                       (expectedValue ? 1 : 0),
                                       (newValue ? 1 : 0));
    }

    /**
     * 如果当前值 == 预期值，则可能以原子方式将值设置为 newValue，并具有 VarHandle.weakCompareAndSetAcquire 指定的内存效果。
     * @since 9
     */
    public final boolean weakCompareAndSetAcquire(boolean expectedValue, boolean newValue) {
        return VALUE.weakCompareAndSetAcquire(this,
                                              (expectedValue ? 1 : 0),
                                              (newValue ? 1 : 0));
    }

    /**
     * 如果当前值 == 预期值，则可能以原子方式将值设置为 newValue，并具有 VarHandle.weakCompareAndSetRelease 指定的内存效果。
     * @since 9
     */
    public final boolean weakCompareAndSetRelease(boolean expectedValue, boolean newValue) {
        return VALUE.weakCompareAndSetRelease(this,
                                              (expectedValue ? 1 : 0),
                                              (newValue ? 1 : 0));
    }

}
