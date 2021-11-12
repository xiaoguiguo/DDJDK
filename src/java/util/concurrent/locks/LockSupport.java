package java.util.concurrent.locks;

import jdk.internal.misc.Unsafe;

/**
 * 在同步组件中，当需要阻塞或唤醒一个线程的时候，都会使用LockSupport工具类来完成相应 工作。
 *
 * LockSupport是JDK中比较底层的类，用来创建锁和其他同步工具类的基本线程阻塞原语。
 * java锁和同步器框架的核心 AQS: AbstractQueuedSynchronizer，
 * 就是通过调用 LockSupport .park()和 LockSupport .unpark()实现线程的阻塞和唤醒 的。
 *
 * LockSupport 很类似于二元信号量(只有1个许可证可供使用)，如果这个许可还没有被占用，当前线程获取许可并继 续 执行；
 * 如果许可已经被占用，当前线 程阻塞，等待获取许可。
 */
public class LockSupport {
    private LockSupport() {} // Cannot be instantiated.

    /**
     * 设置blocker
     */
    private static void setBlocker(Thread t, Object arg) {
        // 即使不稳定，hotspot 在这里也不需要写屏障。
        U.putObject(t, PARKBLOCKER, arg);
    }

    /**
     * 使给定线程的许可可用（如果它尚不可用）。 如果线程在 park 上被阻塞，那么它将解除阻塞。
     * 否则，保证它的下一次 挂起 调用不会阻塞。 如果给定的线程尚未启动，则无法保证此操作有任何效果。
     */
    public static void unpark(Thread thread) {
        if (thread != null) {
            U.unpark(thread);
        }
    }

    /**
     * 除非许可可用，否则出于线程调度目的禁用当前线程。
     * 如果许可可用，则它被消耗并且调用立即返回； 否则，当前线程将因线程调度目的而被禁用并处于休眠状态，直到发生以下三种情况之一：
     *  1. 其他一些线程以当前线程为目标调用 unpark； 或者
     *  2. 其他一些线程中断当前线程； 或者
     *  3. 虚假调用（即，无缘无故）返回。
     * 此方法不报告其中哪些导致方法返回。 调用者应该首先重新检查导致线程挂起的条件。 例如，调用者还可以确定线程在返回时的中断状态。
     */
    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        U.park(false, 0L);
        setBlocker(t, null);
    }

    /**
     * 为线程调度目的禁用当前线程，直至指定的等待时间，除非许可可用。
     * 如果许可可用，则它被消耗并且调用立即返回； 否则，当前线程将因线程调度目的而被禁用，并处于休眠状态，直到发生以下四种情况之一：
     *  1. 其他一些线程以当前线程为目标调用 unpark； 或者
     *  2. 其他一些线程中断当前线程； 或者
     *  3. 指定的等待时间过去； 或者
     *  4. 虚假调用（即，无缘无故）返回。
     * 此方法不报告其中哪些导致方法返回。 调用者应该首先重新检查导致线程挂起的条件。 例如，调用者还可以确定线程的中断状态或返回时经过的时间。
     */
    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            U.park(false, nanos);
            setBlocker(t, null);
        }
    }

    /**
     * 出于线程调度目的禁用当前线程，直到指定的截止日期，除非许可可用。
     * 如果许可可用，则它被消耗并且调用立即返回； 否则，当前线程将因线程调度目的而被禁用，并处于休眠状态，直到发生以下四种情况之一：
     *  1. 其他一些线程以当前线程为目标调用 unpark； 或者
     *  2. 其他一些线程中断当前线程； 或者
     *  3. 指定的截止日期已过； 或者
     *  4. 虚假调用（即，无缘无故）返回。
     * 此方法不报告其中哪些导致方法返回。 调用者应该首先重新检查导致线程挂起的条件。 例如，调用者还可以确定线程的中断状态或返回时的当前时间。
     */
    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        U.park(true, deadline);
        setBlocker(t, null);
    }

    /**
     * 返回提供给尚未解除阻塞的 park 方法的最近调用的阻塞程序对象，如果未阻塞，则返回 null。
     * 返回的值只是一个瞬间的快照——线程可能已经在不同的阻塞器对象上解除阻塞或阻塞。
     */
    public static Object getBlocker(Thread t) {
        if (t == null) {
            throw new NullPointerException();
        }
        return U.getObjectVolatile(t, PARKBLOCKER);
    }

    /**
     * 除非许可可用，否则出于线程调度目的禁用当前线程。
     * 如果许可可用，则它被消耗并且调用立即返回； 否则，当前线程将因线程调度目的而被禁用并处于休眠状态，直到发生以下三种情况之一：
     *  1. 其他一些线程以当前线程为目标调用 unpark； 或者
     *  2. 其他一些线程中断当前线程； 或者
     *  3. 虚假调用（即，无缘无故）返回。
     * 此方法不报告其中哪些导致方法返回。 调用者应该首先重新检查导致线程挂起的条件。 例如，调用者还可以确定线程在返回时的中断状态。
     */
    public static void park() {
        U.park(false, 0L);
    }

    /**
     * 为线程调度目的禁用当前线程，直至指定的等待时间，除非许可可用。
     * 如果许可可用，则它被消耗并且调用立即返回； 否则，当前线程将因线程调度目的而被禁用，并处于休眠状态，直到发生以下四种情况之一：
     *  1. 其他一些线程以当前线程为目标调用 unpark； 或者
     *  2. 其他一些线程中断当前线程； 或者
     *  3. 指定的等待时间过去； 或者
     *  4. 虚假调用（即，无缘无故）返回。
     * 此方法不报告其中哪些导致方法返回。 调用者应该首先重新检查导致线程挂起的条件。 例如，调用者还可以确定线程的中断状态或返回时经过的时间。
     */
    public static void parkNanos(long nanos) {
        if (nanos > 0) {
            U.park(false, nanos);
        }
    }

    /**
     * 出于线程调度目的禁用当前线程，直到指定的截止日期，除非许可可用。
     * 如果许可可用，则它被消耗并且调用立即返回； 否则，当前线程将因线程调度目的而被禁用，并处于休眠状态，直到发生以下四种情况之一：
     *  1. 其他一些线程以当前线程为目标调用 unpark； 或者
     *  2. 其他一些线程中断当前线程； 或者
     *  3. 指定的截止日期已过； 或者
     *  4. 虚假调用（即，无缘无故）返回。
     * 此方法不报告其中哪些导致方法返回。 调用者应该首先重新检查导致线程挂起的条件。
     * 例如，调用者还可以确定线程的中断状态或返回时的当前时间。
     */
    public static void parkUntil(long deadline) {
        U.park(true, deadline);
    }

    /**
     * 返回伪随机初始化或更新的辅助种子。 由于包访问限制，从 ThreadLocalRandom 复制。
     */
    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if ((r = U.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        }
        else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0) {
            r = 1; // avoid zero
        }
        U.putInt(t, SECONDARY, r);
        return r;
    }

    /**
     * 返回给定线程的线程 ID。 我们必须直接访问它，而不是通过 Thread.getId() 方法，因为已知 getId() 以不保留唯一映射的方式被覆盖。
     */
    static final long getThreadId(Thread thread) {
        return U.getLong(thread, TID);
    }

    // Hotspot implementation via intrinsics API
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long PARKBLOCKER = U.objectFieldOffset
            (Thread.class, "parkBlocker");
    private static final long SECONDARY = U.objectFieldOffset
            (Thread.class, "threadLocalRandomSecondarySeed");
    private static final long TID = U.objectFieldOffset
            (Thread.class, "tid");

}
