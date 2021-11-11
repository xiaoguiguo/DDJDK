/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import jdk.internal.HotSpotIntrinsicCandidate;
import jdk.internal.util.Preconditions;
import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.Stable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.lang.invoke.MethodHandleStatics.UNSAFE;
import static java.lang.invoke.MethodHandleStatics.newInternalError;

/**
 * VarHandle是对变量或参数定义的变量系列的动态强类型引用，包括静态字段，非静态字段，数组元素或堆外数据结构的组件。
 * 在各种访问模式下都支持访问这些变量，包括普通读/写访问，易失性读/写访问以及比较和设置。
 * VarHandles是不可变的，没有可见状态。
 *
 * 访问模式控制原子性和一致性属性。 普通读取（ get ）和写入（ set ）访问保证仅对于引用和最多32位的原始值是按位原子的，
 * 并且对于除执行线程之外的线程没有强加可观察的排序约束。 对于访问同一变量， 不透明操作是按位原子和相干有序的。
 * 除了遵守不透明属性之外，在匹配释放模式写入及其先前的访问之后，还会对获取模式读取及其后续访问进行排序。
 * 除了遵守Acquire和Release属性之外，所有Volatile操作都是相互完全排序的。
 *
 * 访问模式分为以下几类：
 *   1. 读取访问模式，获取指定内存排序效果下的变量值。
 *          该组对应属于该组的访问模式的方法的组成的方法get ， getVolatile ， getAcquire ， getOpaque 。
 *   2. 写入访问模式，在指定的内存排序效果下设置变量的值。
 *          该组对应属于该组的访问模式的方法的组成的方法set ， setVolatile ， setRelease ， setOpaque 。
 *   3. 原子更新访问模式，例如，在指定的内存排序效果下，原子地比较和设置变量的值。
 *          该组对应属于该组的访问模式的方法的组成的方法compareAndSet ， weakCompareAndSetPlain ， weakCompareAndSet ，
 *          weakCompareAndSetAcquire ， weakCompareAndSetRelease ， compareAndExchangeAcquire ， compareAndExchange ，
 *          compareAndExchangeRelease ， getAndSet ， getAndSetAcquire ， getAndSetRelease 。
 *   4. 数字原子更新访问模式，例如，通过在指定的内存排序效果下添加变量的值，以原子方式获取和设置。
 *          该组的属于该组的相应的访问模式的方法由方法getAndAdd ， getAndAddAcquire ， getAndAddRelease ，
 *   5. 按位原子更新访问模式，例如，在指定的内存排序效果下，以原子方式获取和按位OR变量的值。
 *          该组对应属于该组的访问模式的方法的组成的方法getAndBitwiseOr ， getAndBitwiseOrAcquire ， getAndBitwiseOrRelease ，
 *          getAndBitwiseAnd ， getAndBitwiseAndAcquire ， getAndBitwiseAndRelease ， getAndBitwiseXor ，
 *          getAndBitwiseXorAcquire ， getAndBitwiseXorRelease 。
 * @since 9
 */
public abstract class VarHandle {
    final VarForm vform;

    VarHandle(VarForm vform) {
        this.vform = vform;
    }

    RuntimeException unsupported() {
        return new UnsupportedOperationException();
    }

    // Plain accessors

    /**
     * 返回变量的值，读取的内存语义就像变量声明为非 volatile 。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object get(Object... args);

    /**
     * 将变量的值设置为 newValue ，其内存语义设置为将变量声明为非 volatile final 。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    void set(Object... args);


    // Volatile accessors

    /**
     * 返回变量的值，读取的内存语义就像声明变量 volatile 。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getVolatile(Object... args);

    /**
     * 将变量的值设置为 newValue ，设置的内存语义就像声明变量 volatile 。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    void setVolatile(Object... args);


    /**
     * 返回以程序顺序访问的变量的值，但不保证相对于其他线程的内存排序效果。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getOpaque(Object... args);

    /**
     * 按程序顺序将变量的值设置为 newValue ，但不保证相对于其他线程的内存排序效果。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    void setOpaque(Object... args);


    // Lazy accessors

    /**
     * 返回变量的值，并确保在此访问之前不会重新排序后续加载和存储。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAcquire(Object... args);

    /**
     * 将变量的值设置为 newValue ，并确保在此访问后不重新排序先前的加载和存储。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    void setRelease(Object... args);


    // Compare and set accessors

    /**
     * 如果变量的当前值（称为见证值）{==} {expectedValue} 以内存语义访问，
     * 则以 {#setVolatile} 的内存语义原子地将变量的值设置为 {newValue}。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    boolean compareAndSet(Object... args);

    /**
     * 如果变量的当前值（称为见证值）== expectedValue（使用 getVolatile 的内存语义访问），
     * 则使用 setVolatile 的内存语义原子地将变量的值设置为 newValue。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object compareAndExchange(Object... args);

    /**
     * 如果变量的当前值（称为见证值）== expectedValue（使用 getAcquire 的内存语义访问），
     * 则使用 set 的内存语义原子地将变量的值设置为 newValue。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object compareAndExchangeAcquire(Object... args);

    /**
     * 如果变量的当前值（称为见证值）== expectedValue（使用 get 的内存语义访问），
     * 则使用 setRelease 的内存语义原子地将变量的值设置为 newValue。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object compareAndExchangeRelease(Object... args);

    // Weak (spurious failures allowed)

    /**
     * 如果变量的当前值（称为见证值）== expectedValue（使用 get 的内存语义访问），
     * 则可能以 set 的语义将变量的值原子地设置为 newValue。
     * 即使见证值确实与预期值匹配，此操作也可能会错误地失败（通常是由于内存争用）。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    boolean weakCompareAndSetPlain(Object... args);

    /**
     * 如果变量的当前值（称为见证值）== 预期值（使用 getVolatile 的内存语义访问），则可能使用 setVolatile 的内存语义将变量的值原子地设置为 newValue。
     * 即使见证值确实与预期值匹配，此操作也可能会错误地失败（通常是由于内存争用）。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    boolean weakCompareAndSet(Object... args);

    /**
     * 如果变量的当前值（称为见证值）== expectedValue（使用 getAcquire 的内存语义访问），
     * 则可能使用 set 语义将变量的值原子地设置为 newValue。
     * 即使见证值确实与预期值匹配，此操作也可能会错误地失败（通常是由于内存争用）。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    boolean weakCompareAndSetAcquire(Object... args);

    /**
     * 如果变量的当前值（称为见证值）== 预期值（使用 get 的内存语义访问），
     * 则可能使用 setRelease 的语义将变量的值原子地设置为 newValue。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    boolean weakCompareAndSetRelease(Object... args);

    /**
     * 使用 setVolatile 的内存语义以原子方式将变量的值设置为 newValue 并返回变量的先前值，如使用 getVolatile 的内存语义访问的那样。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndSet(Object... args);

    /**
     * 使用 set 的内存语义以原子方式将变量的值设置为 newValue 并返回变量的先前值，如使用 getAcquire 的内存语义访问的那样。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndSetAcquire(Object... args);

    /**
     * 使用 setRelease 的内存语义以原子方式将变量的值设置为 newValue 并返回变量的先前值，如使用 get 的内存语义访问。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndSetRelease(Object... args);

    // Primitive adders
    // Throw UnsupportedOperationException for refs

    /**
     * 使用 setVolatile 的内存语义将值原子地添加到变量的当前值，并返回变量的先前值，如使用 getVolatile 的内存语义访问的那样。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndAdd(Object... args);

    /**
     * 使用 set 的内存语义将值原子地添加到变量的当前值，并返回变量的先前值，如使用 getAcquire 的内存语义访问。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndAddAcquire(Object... args);

    /**
     * 使用 setRelease 的内存语义将值原子地添加到变量的当前值，并返回变量的先前值，如使用 get 的内存语义访问。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndAddRelease(Object... args);


    // Bitwise operations
    // Throw UnsupportedOperationException for refs

    /**
     * 原子地将变量的值设置为变量当前值与具有 setVolatile 内存语义的掩码之间的按位 OR 的结果，
     * 并返回变量的先前值，如使用 getVolatile 的内存语义访问。
     *
     * 如果变量类型是非整型布尔类型，则执行逻辑 OR 而不是按位 OR。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseOr(Object... args);

    /**
     * 原子地将变量的值设置为变量当前值与具有 set 内存语义的掩码之间的按位 OR 的结果，并返回变量的先前值，如使用 getAcquire 的内存语义访问。
     * 如果变量类型是非整型布尔类型，则执行逻辑 OR 而不是按位 OR。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseOrAcquire(Object... args);

    /**
     * 使用 setRelease 的内存语义将变量的值原子地设置为变量当前值和掩码之间按位或的结果，并返回变量的先前值，如使用 get 的内存语义访问。
     * 如果变量类型是非整型布尔类型，则执行逻辑 OR 而不是按位 OR。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseOrRelease(Object... args);

    /**
     * 原子地将变量的值设置为变量当前值与具有 setVolatile 内存语义的掩码之间的按位 AND 结果，并返回变量的先前值，如使用 getVolatile 内存语义访问的那样。
     * 如果变量类型是非整型布尔类型，则执行逻辑与而不是按位与。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseAnd(Object... args);

    /**
     * 原子地将变量的值设置为变量当前值与具有 set 内存语义的掩码之间的按位 AND 的结果，并返回变量的先前值，如使用 getAcquire 的内存语义访问。
     * 如果变量类型是非整型布尔类型，则执行逻辑与而不是按位与。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseAndAcquire(Object... args);

    /**
     * 使用 setRelease 的内存语义将变量的值原子地设置为变量当前值与掩码之间的按位 AND 结果，并返回变量的先前值，如使用 get 的内存语义访问。
     * 如果变量类型是非整型布尔类型，则执行逻辑与而不是按位与。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseAndRelease(Object... args);

    /**
     * 使用 setVolatile 的内存语义将变量的值原子地设置为变量当前值与掩码之间的按位异或的结果，并返回变量的先前值，如使用 getVolatile 的内存语义访问。
     * 如果变量类型是非整型布尔类型，则执行逻辑异或而不是按位异或。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseXor(Object... args);

    /**
     * 原子地将变量的值设置为变量当前值与具有 set 内存语义的掩码之间的按位异或的结果，并返回变量的先前值，如使用 getAcquire 的内存语义访问。
     * 如果变量类型是非整型布尔类型，则执行逻辑异或而不是按位异或。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseXorAcquire(Object... args);

    /**
     * 使用 setRelease 的内存语义将变量的值原子地设置为变量当前值与掩码之间按位异或的结果，并返回变量的先前值，如使用 get 的内存语义访问。
     * 如果变量类型是非整型布尔类型，则执行逻辑异或而不是按位异或。
     */
    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseXorRelease(Object... args);


    enum AccessType {
        GET(Object.class),
        SET(void.class),
        COMPARE_AND_SET(boolean.class),
        COMPARE_AND_EXCHANGE(Object.class),
        GET_AND_UPDATE(Object.class);

        final Class<?> returnType;
        final boolean isMonomorphicInReturnType;

        AccessType(Class<?> returnType) {
            this.returnType = returnType;
            isMonomorphicInReturnType = returnType != Object.class;
        }

        MethodType accessModeType(Class<?> receiver, Class<?> value,
                                  Class<?>... intermediate) {
            Class<?>[] ps;
            int i;
            switch (this) {
                case GET:
                    ps = allocateParameters(0, receiver, intermediate);
                    fillParameters(ps, receiver, intermediate);
                    return MethodType.methodType(value, ps);
                case SET:
                    ps = allocateParameters(1, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    ps[i] = value;
                    return MethodType.methodType(void.class, ps);
                case COMPARE_AND_SET:
                    ps = allocateParameters(2, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    ps[i++] = value;
                    ps[i] = value;
                    return MethodType.methodType(boolean.class, ps);
                case COMPARE_AND_EXCHANGE:
                    ps = allocateParameters(2, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    ps[i++] = value;
                    ps[i] = value;
                    return MethodType.methodType(value, ps);
                case GET_AND_UPDATE:
                    ps = allocateParameters(1, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    ps[i] = value;
                    return MethodType.methodType(value, ps);
                default:
                    throw new InternalError("Unknown AccessType");
            }
        }

        private static Class<?>[] allocateParameters(int values,
                                                     Class<?> receiver, Class<?>... intermediate) {
            int size = ((receiver != null) ? 1 : 0) + intermediate.length + values;
            return new Class<?>[size];
        }

        private static int fillParameters(Class<?>[] ps,
                                          Class<?> receiver, Class<?>... intermediate) {
            int i = 0;
            if (receiver != null)
                ps[i++] = receiver;
            for (int j = 0; j < intermediate.length; j++)
                ps[i++] = intermediate[j];
            return i;
        }
    }

    /**
     * 一组访问模式，指定如何访问由 VarHandle 引用的变量。
     */
    public enum AccessMode {
        /**
         * 访问方式由对应方法指定的访问方式 VarHandle.get
         */
        GET("get", AccessType.GET),
        /**
         * 访问方式由对应方法指定的访问方式 VarHandle.set
         */
        SET("set", AccessType.SET),
        /**
         * 访问方式由对应方法指定的访问方式 VarHandle.getVolatile}
         */
        GET_VOLATILE("getVolatile", AccessType.GET),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#setVolatile VarHandle.setVolatile}
         */
        SET_VOLATILE("setVolatile", AccessType.SET),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAcquire VarHandle.getAcquire}
         */
        GET_ACQUIRE("getAcquire", AccessType.GET),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#setRelease VarHandle.setRelease}
         */
        SET_RELEASE("setRelease", AccessType.SET),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getOpaque VarHandle.getOpaque}
         */
        GET_OPAQUE("getOpaque", AccessType.GET),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#setOpaque VarHandle.setOpaque}
         */
        SET_OPAQUE("setOpaque", AccessType.SET),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#compareAndSet VarHandle.compareAndSet}
         */
        COMPARE_AND_SET("compareAndSet", AccessType.COMPARE_AND_SET),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#compareAndExchange VarHandle.compareAndExchange}
         */
        COMPARE_AND_EXCHANGE("compareAndExchange", AccessType.COMPARE_AND_EXCHANGE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#compareAndExchangeAcquire VarHandle.compareAndExchangeAcquire}
         */
        COMPARE_AND_EXCHANGE_ACQUIRE("compareAndExchangeAcquire", AccessType.COMPARE_AND_EXCHANGE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#compareAndExchangeRelease VarHandle.compareAndExchangeRelease}
         */
        COMPARE_AND_EXCHANGE_RELEASE("compareAndExchangeRelease", AccessType.COMPARE_AND_EXCHANGE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#weakCompareAndSetPlain VarHandle.weakCompareAndSetPlain}
         */
        WEAK_COMPARE_AND_SET_PLAIN("weakCompareAndSetPlain", AccessType.COMPARE_AND_SET),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#weakCompareAndSet VarHandle.weakCompareAndSet}
         */
        WEAK_COMPARE_AND_SET("weakCompareAndSet", AccessType.COMPARE_AND_SET),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#weakCompareAndSetAcquire VarHandle.weakCompareAndSetAcquire}
         */
        WEAK_COMPARE_AND_SET_ACQUIRE("weakCompareAndSetAcquire", AccessType.COMPARE_AND_SET),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#weakCompareAndSetRelease VarHandle.weakCompareAndSetRelease}
         */
        WEAK_COMPARE_AND_SET_RELEASE("weakCompareAndSetRelease", AccessType.COMPARE_AND_SET),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndSet VarHandle.getAndSet}
         */
        GET_AND_SET("getAndSet", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndSetAcquire VarHandle.getAndSetAcquire}
         */
        GET_AND_SET_ACQUIRE("getAndSetAcquire", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndSetRelease VarHandle.getAndSetRelease}
         */
        GET_AND_SET_RELEASE("getAndSetRelease", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndAdd VarHandle.getAndAdd}
         */
        GET_AND_ADD("getAndAdd", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndAddAcquire VarHandle.getAndAddAcquire}
         */
        GET_AND_ADD_ACQUIRE("getAndAddAcquire", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndAddRelease VarHandle.getAndAddRelease}
         */
        GET_AND_ADD_RELEASE("getAndAddRelease", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndBitwiseOr VarHandle.getAndBitwiseOr}
         */
        GET_AND_BITWISE_OR("getAndBitwiseOr", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndBitwiseOrRelease VarHandle.getAndBitwiseOrRelease}
         */
        GET_AND_BITWISE_OR_RELEASE("getAndBitwiseOrRelease", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndBitwiseOrAcquire VarHandle.getAndBitwiseOrAcquire}
         */
        GET_AND_BITWISE_OR_ACQUIRE("getAndBitwiseOrAcquire", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndBitwiseAnd VarHandle.getAndBitwiseAnd}
         */
        GET_AND_BITWISE_AND("getAndBitwiseAnd", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndBitwiseAndRelease VarHandle.getAndBitwiseAndRelease}
         */
        GET_AND_BITWISE_AND_RELEASE("getAndBitwiseAndRelease", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndBitwiseAndAcquire VarHandle.getAndBitwiseAndAcquire}
         */
        GET_AND_BITWISE_AND_ACQUIRE("getAndBitwiseAndAcquire", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndBitwiseXor VarHandle.getAndBitwiseXor}
         */
        GET_AND_BITWISE_XOR("getAndBitwiseXor", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndBitwiseXorRelease VarHandle.getAndBitwiseXorRelease}
         */
        GET_AND_BITWISE_XOR_RELEASE("getAndBitwiseXorRelease", AccessType.GET_AND_UPDATE),
        /**
         * 访问方式由对应方法指定的访问方式
         * {@link VarHandle#getAndBitwiseXorAcquire VarHandle.getAndBitwiseXorAcquire}
         */
        GET_AND_BITWISE_XOR_ACQUIRE("getAndBitwiseXorAcquire", AccessType.GET_AND_UPDATE),
        ;

        static final Map<String, AccessMode> methodNameToAccessMode;
        static {
            AccessMode[] values = AccessMode.values();
            // Initial capacity of # values divided by the load factor is sufficient
            // to avoid resizes for the smallest table size (64)
            int initialCapacity = (int)(values.length / 0.75f) + 1;
            methodNameToAccessMode = new HashMap<>(initialCapacity);
            for (AccessMode am : values) {
                methodNameToAccessMode.put(am.methodName, am);
            }
        }

        final String methodName;
        final AccessType at;

        AccessMode(final String methodName, AccessType at) {
            this.methodName = methodName;
            this.at = at;
        }

        /**
         * 返回与此 AccessMode 值关联的 VarHandle 签名多态方法名称。
         */
        public String methodName() {
            return methodName;
        }

        /**
         * 返回与指定的 VarHandle 签名多态方法名称关联的 AccessMode 值。
         */
        public static AccessMode valueFromMethodName(String methodName) {
            AccessMode am = methodNameToAccessMode.get(methodName);
            if (am != null) {
                return am;
            }
            throw new IllegalArgumentException("No AccessMode value for method name " + methodName);
        }

        @ForceInline
        static MemberName getMemberName(int ordinal, VarForm vform) {
            return vform.memberName_table[ordinal];
        }
    }

    static final class AccessDescriptor {
        final MethodType symbolicMethodTypeErased;
        final MethodType symbolicMethodTypeInvoker;
        final Class<?> returnType;
        final int type;
        final int mode;

        public AccessDescriptor(MethodType symbolicMethodType, int type, int mode) {
            this.symbolicMethodTypeErased = symbolicMethodType.erase();
            this.symbolicMethodTypeInvoker = symbolicMethodType.insertParameterTypes(0, VarHandle.class);
            this.returnType = symbolicMethodType.returnType();
            this.type = type;
            this.mode = mode;
        }
    }

    /**
     * 返回此 VarHandle 引用的变量的变量类型。
     */
    public final Class<?> varType() {
        MethodType typeSet = accessModeType(AccessMode.SET);
        return typeSet.parameterType(typeSet.parameterCount() - 1);
    }

    /**
     * 返回此 VarHandle 的坐标类型。
     */
    public final List<Class<?>> coordinateTypes() {
        MethodType typeGet = accessModeType(AccessMode.GET);
        return typeGet.parameterList();
    }

    /**
     * 获取此 VarHandle 的访问模式类型和给定的访问模式。
     * 访问模式类型的参数类型将由一个前缀组成，该前缀是此 VarHandle 的坐标类型，后跟访问模式方法定义的其他类型。
     * 访问模式类型的返回类型由访问模式方法的返回类型定义。
     */
    public final MethodType accessModeType(AccessMode accessMode) {
        TypesAndInvokers tis = getTypesAndInvokers();
        MethodType mt = tis.methodType_table[accessMode.at.ordinal()];
        if (mt == null) {
            mt = tis.methodType_table[accessMode.at.ordinal()] =
                    accessModeTypeUncached(accessMode);
        }
        return mt;
    }
    abstract MethodType accessModeTypeUncached(AccessMode accessMode);

    /**
     * 如果支持给定的访问模式，则返回 true，否则返回 false。
     * 给定访问模式返回 false 值表示在调用相应访问模式方法时抛出 UnsupportedOperationException。
     */
    public final boolean isAccessModeSupported(AccessMode accessMode) {
        return AccessMode.getMemberName(accessMode.ordinal(), vform) != null;
    }

    /**
     * 获取绑定到此 VarHandle 的方法句柄和给定的访问模式。
     */
    public final MethodHandle toMethodHandle(AccessMode accessMode) {
        MemberName mn = AccessMode.getMemberName(accessMode.ordinal(), vform);
        if (mn != null) {
            MethodHandle mh = getMethodHandle(accessMode.ordinal());
            return mh.bindTo(this);
        }
        else {
            // Ensure an UnsupportedOperationException is thrown
            return MethodHandles.varHandleInvoker(accessMode, accessModeType(accessMode)).
                    bindTo(this);
        }
    }

    @Stable
    TypesAndInvokers typesAndInvokers;

    static class TypesAndInvokers {
        final @Stable
        MethodType[] methodType_table =
                new MethodType[AccessType.values().length];

        final @Stable
        MethodHandle[] methodHandle_table =
                new MethodHandle[AccessMode.values().length];
    }

    @ForceInline
    private final TypesAndInvokers getTypesAndInvokers() {
        TypesAndInvokers tis = typesAndInvokers;
        if (tis == null) {
            tis = typesAndInvokers = new TypesAndInvokers();
        }
        return tis;
    }

    @ForceInline
    final MethodHandle getMethodHandle(int mode) {
        TypesAndInvokers tis = getTypesAndInvokers();
        MethodHandle mh = tis.methodHandle_table[mode];
        if (mh == null) {
            mh = tis.methodHandle_table[mode] = getMethodHandleUncached(mode);
        }
        return mh;
    }
    private final MethodHandle getMethodHandleUncached(int mode) {
        MethodType mt = accessModeType(AccessMode.values()[mode]).
                insertParameterTypes(0, VarHandle.class);
        MemberName mn = vform.getMemberName(mode);
        DirectMethodHandle dmh = DirectMethodHandle.make(mn);
        // Such a method handle must not be publically exposed directly
        // otherwise it can be cracked, it must be transformed or rebound
        // before exposure
        MethodHandle mh = dmh.copyWith(mt, dmh.form);
        assert mh.type().erase() == mn.getMethodType().erase();
        return mh;
    }


    /*non-public*/
    final void updateVarForm(VarForm newVForm) {
        if (vform == newVForm) return;
        UNSAFE.putObject(this, VFORM_OFFSET, newVForm);
        UNSAFE.fullFence();
    }

    static final BiFunction<String, List<Integer>, ArrayIndexOutOfBoundsException>
            AIOOBE_SUPPLIER = Preconditions.outOfBoundsExceptionFormatter(
            new Function<String, ArrayIndexOutOfBoundsException>() {
                @Override
                public ArrayIndexOutOfBoundsException apply(String s) {
                    return new ArrayIndexOutOfBoundsException(s);
                }
            });

    private static final long VFORM_OFFSET;

    static {
        VFORM_OFFSET = UNSAFE.objectFieldOffset(VarHandle.class, "vform");

        // The VarHandleGuards must be initialized to ensure correct
        // compilation of the guard methods
        UNSAFE.ensureClassInitialized(VarHandleGuards.class);
    }


    // Fence methods

    /**
     * 确保围栏之前的加载和存储不会与围栏之后的加载和存储重新排序。
     * API注意事项：
     * 忽略与 C 和 C++ 的许多语义差异，该方法具有与 atomic_thread_fence(memory_order_seq_cst) 兼容的内存排序效果
     */
    @ForceInline
    public static void fullFence() {
        UNSAFE.fullFence();
    }

    /**
     * 确保围栏之前的加载和存储不会与围栏之后的加载和存储重新排序。
     * API注意事项：
     * 忽略与 C 和 C++ 的许多语义差异，该方法具有与 atomic_thread_fence(memory_order_seq_cst) 兼容的内存排序效果
     */
    @ForceInline
    public static void acquireFence() {
        UNSAFE.loadFence();
    }

    /**
     * 确保围栏之前的加载和存储不会与围栏之后的加载和存储重新排序。
     * API注意事项：
     * 忽略与 C 和 C++ 的许多语义差异，该方法具有与 atomic_thread_fence(memory_order_seq_cst) 兼容的内存排序效果
     */
    @ForceInline
    public static void releaseFence() {
        UNSAFE.storeFence();
    }

    /**
     * 确保围栏之前的负载不会与围栏之后的负载重新排序。
     */
    @ForceInline
    public static void loadLoadFence() {
        UNSAFE.loadLoadFence();
    }

    /**
     * 确保围栏前的存储不会与围栏后的存储重新排序。
     */
    @ForceInline
    public static void storeStoreFence() {
        UNSAFE.storeStoreFence();
    }
}
