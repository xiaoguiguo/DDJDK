package java.util.concurrent.locks;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;
import jdk.internal.vm.annotation.ReservedStackAccess;

/**
 * StampedLock 支持三种模式，分别是：写锁、悲观读锁和乐观读
 *     1)写锁、悲观读锁的语义和 ReadWriteLock 的写锁、读锁的语义非常类似，
 *     2)允许多个线程同时获取悲观读锁，但是只允许一个线程获取写锁，写锁和悲观读锁是互斥的
 *     3)不同的是：StampedLock 里的写锁和悲观读锁加锁成功之后，都会返回一个 stamp；然后解锁的时候，需要传入这个 stamp
 *
 * 一种基于能力的锁，具有三种用于控制读/写访问的模式。 StampedLock 的状态由版本和模式组成。
 * 锁定获取方法返回一个标记，表示和控制与锁定状态相关的访问；这些方法的“尝试”版本可能会返回特殊值零以表示获取访问失败。
 * 锁释放和转换方法需要标记作为参数，如果它们与锁的状态不匹配则失败。这三种模式是：
 *  1. 写锁。方法 writeLock 可能会阻塞等待独占访问，返回可以在方法 unlockWrite 中使用以释放锁的戳记。
 *     还提供了不定时和定时版本的 tryWriteLock。当锁处于写模式时，可能无法获得读锁，所有乐观读验证都将失败。
 *  2. 读锁。方法 readLock 可能会阻塞等待非独占访问，返回一个标记，可以在方法 unlockRead 中使用以释放锁。
 *     还提供了不定时和定时版本的 tryReadLock。
 *  3. 乐观读锁。仅当锁当前未处于写入模式时，方法 tryOptimisticRead 才返回非零标记。
 *     如果自从获得给定的标记后没有在写入模式下获得锁，则方法 validate 返回 true。
 *     这种模式可以被认为是读锁的一个非常弱的版本，它可以随时被写者打破。对短的只读代码段使用乐观模式通常会减少争用并提高吞吐量。
 *     然而，它的使用本质上是脆弱的。乐观读取部分应该只读取字段并将它们保存在局部变量中以供验证后以后使用。
 *     在乐观模式下读取的字段可能非常不一致，因此仅当您足够熟悉数据表示以检查一致性和/或重复调用方法 validate() 时才适用。
 *     例如，当首先读取对象或数组引用，然后访问其字段、元素或方法之一时，通常需要这些步骤。
 */
public class StampedLock implements java.io.Serializable {

    private static final long serialVersionUID = -6001602636862214147L;

    /** 处理器数量，用于自旋控制 */
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    /** 入队获取前的最大重试次数； 至少 1 */
    private static final int SPINS = (NCPU > 1) ? 1 << 6 : 1;

    /** 队列头结点自旋获取锁最大失败次数后再次进入队列 */
    private static final int HEAD_SPINS = (NCPU > 1) ? 1 << 10 : 1;

    /** 重新阻塞前的最大重试次数 */
    private static final int MAX_HEAD_SPINS = (NCPU > 1) ? 1 << 16 : 1;

    /** 等待溢出自旋锁时的生产周期 */
    private static final int OVERFLOW_YIELD_RATE = 7; // must be power 2 - 1

    /** 溢出之前用于阅读器计数的位数 */
    private static final int LG_READERS = 7;

    // Values for lock state and stamp operations
    private static final long RUNIT = 1L;
    private static final long WBIT  = 1L << LG_READERS;
    private static final long RBITS = WBIT - 1L;
    private static final long RFULL = RBITS - 1L;
    private static final long ABITS = RBITS | WBIT;
    private static final long SBITS = ~RBITS; // note overlap with ABITS

    /**
     * 通过检查（m = stamp & ABITS）可以区分3种标记模式：
     * 写模式：m == WBIT 乐观读模式：m == 0L（即使持有读锁）
     * 读模式：m > 0L && m <= RFULL （标记是状态的副本，但标记中的读取保持计数除了用于确定模式外未使用）。
     * 这与状态的编码略有不同：(state & ABITS) == 0L 表示锁当前已解锁。
     * (state & ABITS) == RBITS 是一个特殊的瞬态值，指示自旋锁定以操纵读取器位溢出。
     */

    /** Initial value for lock state; avoids failure value zero. */
    private static final long ORIGIN = WBIT << 1;

    // Special value from cancelled acquire methods so caller can throw IE
    private static final long INTERRUPTED = 1L;

    // Values for node status; order matters
    private static final int WAITING   = -1;
    private static final int CANCELLED =  1;

    // Modes for nodes (int not boolean to allow arithmetic)
    private static final int RMODE = 0;
    private static final int WMODE = 1;

    /** Wait nodes */
    static final class WNode {
        volatile WNode prev;
        volatile WNode next;
        volatile WNode cowait;    // list of linked readers
        volatile Thread thread;   // non-null while possibly parked
        volatile int status;      // 0, WAITING, or CANCELLED
        final int mode;           // RMODE or WMODE
        WNode(int m, WNode p) { mode = m; prev = p; }
    }

    /** Head of CLH queue */
    private transient volatile WNode whead;
    /** Tail (last) of CLH queue */
    private transient volatile WNode wtail;

    // views
    transient ReadLockView readLockView;
    transient WriteLockView writeLockView;
    transient ReadWriteLockView readWriteLockView;

    /** Lock sequence/state */
    private transient volatile long state;
    /** extra reader count when state read count saturated */
    private transient int readerOverflow;

    /**
     * Creates a new lock, initially in unlocked state.
     */
    public StampedLock() {
        state = ORIGIN;
    }

    private boolean casState(long expectedValue, long newValue) {
        return STATE.compareAndSet(this, expectedValue, newValue);
    }

    private long tryWriteLock(long s) {
        // assert (s & ABITS) == 0L;
        long next;
        if (casState(s, next = s | WBIT)) {
            VarHandle.storeStoreFence();
            return next;
        }
        return 0L;
    }

    /**
     * Exclusively acquires the lock, blocking if necessary
     * until available.
     *
     * @return a write stamp that can be used to unlock or convert mode
     */
    @ReservedStackAccess
    public long writeLock() {
        long next;
        return ((next = tryWriteLock()) != 0L) ? next : acquireWrite(false, 0L);
    }

    /**
     * Exclusively acquires the lock if it is immediately available.
     *
     * @return a write stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     */
    @ReservedStackAccess
    public long tryWriteLock() {
        long s;
        return (((s = state) & ABITS) == 0L) ? tryWriteLock(s) : 0L;
    }

    /**
     * Exclusively acquires the lock if it is available within the
     * given time and the current thread has not been interrupted.
     * Behavior under timeout and interruption matches that specified
     * for method {@link Lock#tryLock(long,TimeUnit)}.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return a write stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long tryWriteLock(long time, TimeUnit unit)
        throws InterruptedException {
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            long next, deadline;
            if ((next = tryWriteLock()) != 0L)
                return next;
            if (nanos <= 0L)
                return 0L;
            if ((deadline = System.nanoTime() + nanos) == 0L)
                deadline = 1L;
            if ((next = acquireWrite(true, deadline)) != INTERRUPTED)
                return next;
        }
        throw new InterruptedException();
    }

    /**
     * Exclusively acquires the lock, blocking if necessary
     * until available or the current thread is interrupted.
     * Behavior under interruption matches that specified
     * for method {@link Lock#lockInterruptibly()}.
     *
     * @return a write stamp that can be used to unlock or convert mode
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    @ReservedStackAccess
    public long writeLockInterruptibly() throws InterruptedException {
        long next;
        if (!Thread.interrupted() &&
            (next = acquireWrite(true, 0L)) != INTERRUPTED)
            return next;
        throw new InterruptedException();
    }

    /**
     * Non-exclusively acquires the lock, blocking if necessary
     * until available.
     *
     * @return a read stamp that can be used to unlock or convert mode
     */
    @ReservedStackAccess
    public long readLock() {
        long s, next;
        // bypass acquireRead on common uncontended case
        return (whead == wtail
                && ((s = state) & ABITS) < RFULL
                && casState(s, next = s + RUNIT))
            ? next
            : acquireRead(false, 0L);
    }

    /**
     * Non-exclusively acquires the lock if it is immediately available.
     *
     * @return a read stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     */
    @ReservedStackAccess
    public long tryReadLock() {
        long s, m, next;
        while ((m = (s = state) & ABITS) != WBIT) {
            if (m < RFULL) {
                if (casState(s, next = s + RUNIT))
                    return next;
            }
            else if ((next = tryIncReaderOverflow(s)) != 0L)
                return next;
        }
        return 0L;
    }

    /**
     * Non-exclusively acquires the lock if it is available within the
     * given time and the current thread has not been interrupted.
     * Behavior under timeout and interruption matches that specified
     * for method {@link Lock#tryLock(long,TimeUnit)}.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return a read stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    @ReservedStackAccess
    public long tryReadLock(long time, TimeUnit unit)
        throws InterruptedException {
        long s, m, next, deadline;
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            if ((m = (s = state) & ABITS) != WBIT) {
                if (m < RFULL) {
                    if (casState(s, next = s + RUNIT))
                        return next;
                }
                else if ((next = tryIncReaderOverflow(s)) != 0L)
                    return next;
            }
            if (nanos <= 0L)
                return 0L;
            if ((deadline = System.nanoTime() + nanos) == 0L)
                deadline = 1L;
            if ((next = acquireRead(true, deadline)) != INTERRUPTED)
                return next;
        }
        throw new InterruptedException();
    }

    /**
     * Non-exclusively acquires the lock, blocking if necessary
     * until available or the current thread is interrupted.
     * Behavior under interruption matches that specified
     * for method {@link Lock#lockInterruptibly()}.
     *
     * @return a read stamp that can be used to unlock or convert mode
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    @ReservedStackAccess
    public long readLockInterruptibly() throws InterruptedException {
        long s, next;
        if (!Thread.interrupted()
            // bypass acquireRead on common uncontended case
            && ((whead == wtail
                 && ((s = state) & ABITS) < RFULL
                 && casState(s, next = s + RUNIT))
                ||
                (next = acquireRead(true, 0L)) != INTERRUPTED))
            return next;
        throw new InterruptedException();
    }

    /**
     * Returns a stamp that can later be validated, or zero
     * if exclusively locked.
     *
     * @return a valid optimistic read stamp, or zero if exclusively locked
     */
    public long tryOptimisticRead() {
        long s;
        return (((s = state) & WBIT) == 0L) ? (s & SBITS) : 0L;
    }

    /**
     * Returns true if the lock has not been exclusively acquired
     * since issuance of the given stamp. Always returns false if the
     * stamp is zero. Always returns true if the stamp represents a
     * currently held lock. Invoking this method with a value not
     * obtained from {@link #tryOptimisticRead} or a locking method
     * for this lock has no defined effect or result.
     *
     * @param stamp a stamp
     * @return {@code true} if the lock has not been exclusively acquired
     * since issuance of the given stamp; else false
     */
    public boolean validate(long stamp) {
        VarHandle.acquireFence();
        return (stamp & SBITS) == (state & SBITS);
    }

    /**
     * Returns an unlocked state, incrementing the version and
     * avoiding special failure value 0L.
     *
     * @param s a write-locked state (or stamp)
     */
    private static long unlockWriteState(long s) {
        return ((s += WBIT) == 0L) ? ORIGIN : s;
    }

    private long unlockWriteInternal(long s) {
        long next; WNode h;
        STATE.setVolatile(this, next = unlockWriteState(s));
        if ((h = whead) != null && h.status != 0)
            release(h);
        return next;
    }

    /**
     * If the lock state matches the given stamp, releases the
     * exclusive lock.
     *
     * @param stamp a stamp returned by a write-lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    @ReservedStackAccess
    public void unlockWrite(long stamp) {
        if (state != stamp || (stamp & WBIT) == 0L)
            throw new IllegalMonitorStateException();
        unlockWriteInternal(stamp);
    }

    /**
     * If the lock state matches the given stamp, releases the
     * non-exclusive lock.
     *
     * @param stamp a stamp returned by a read-lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    @ReservedStackAccess
    public void unlockRead(long stamp) {
        long s, m; WNode h;
        while (((s = state) & SBITS) == (stamp & SBITS)
               && (stamp & RBITS) > 0L
               && ((m = s & RBITS) > 0L)) {
            if (m < RFULL) {
                if (casState(s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                return;
        }
        throw new IllegalMonitorStateException();
    }

    /**
     * If the lock state matches the given stamp, releases the
     * corresponding mode of the lock.
     *
     * @param stamp a stamp returned by a lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    @ReservedStackAccess
    public void unlock(long stamp) {
        if ((stamp & WBIT) != 0L)
            unlockWrite(stamp);
        else
            unlockRead(stamp);
    }

    /**
     * If the lock state matches the given stamp, atomically performs one of
     * the following actions. If the stamp represents holding a write
     * lock, returns it.  Or, if a read lock, if the write lock is
     * available, releases the read lock and returns a write stamp.
     * Or, if an optimistic read, returns a write stamp only if
     * immediately available. This method returns zero in all other
     * cases.
     *
     * @param stamp a stamp
     * @return a valid write stamp, or zero on failure
     */
    public long tryConvertToWriteLock(long stamp) {
        long a = stamp & ABITS, m, s, next;
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            if ((m = s & ABITS) == 0L) {
                if (a != 0L)
                    break;
                if ((next = tryWriteLock(s)) != 0L)
                    return next;
            }
            else if (m == WBIT) {
                if (a != m)
                    break;
                return stamp;
            }
            else if (m == RUNIT && a != 0L) {
                if (casState(s, next = s - RUNIT + WBIT)) {
                    VarHandle.storeStoreFence();
                    return next;
                }
            }
            else
                break;
        }
        return 0L;
    }

    /**
     * If the lock state matches the given stamp, atomically performs one of
     * the following actions. If the stamp represents holding a write
     * lock, releases it and obtains a read lock.  Or, if a read lock,
     * returns it. Or, if an optimistic read, acquires a read lock and
     * returns a read stamp only if immediately available. This method
     * returns zero in all other cases.
     *
     * @param stamp a stamp
     * @return a valid read stamp, or zero on failure
     */
    public long tryConvertToReadLock(long stamp) {
        long a, s, next; WNode h;
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            if ((a = stamp & ABITS) >= WBIT) {
                // write stamp
                if (s != stamp)
                    break;
                STATE.setVolatile(this, next = unlockWriteState(s) + RUNIT);
                if ((h = whead) != null && h.status != 0)
                    release(h);
                return next;
            }
            else if (a == 0L) {
                // optimistic read stamp
                if ((s & ABITS) < RFULL) {
                    if (casState(s, next = s + RUNIT))
                        return next;
                }
                else if ((next = tryIncReaderOverflow(s)) != 0L)
                    return next;
            }
            else {
                // already a read stamp
                if ((s & ABITS) == 0L)
                    break;
                return stamp;
            }
        }
        return 0L;
    }

    /**
     * If the lock state matches the given stamp then, atomically, if the stamp
     * represents holding a lock, releases it and returns an
     * observation stamp.  Or, if an optimistic read, returns it if
     * validated. This method returns zero in all other cases, and so
     * may be useful as a form of "tryUnlock".
     *
     * @param stamp a stamp
     * @return a valid optimistic read stamp, or zero on failure
     */
    public long tryConvertToOptimisticRead(long stamp) {
        long a, m, s, next; WNode h;
        VarHandle.acquireFence();
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            if ((a = stamp & ABITS) >= WBIT) {
                // write stamp
                if (s != stamp)
                    break;
                return unlockWriteInternal(s);
            }
            else if (a == 0L)
                // already an optimistic read stamp
                return stamp;
            else if ((m = s & ABITS) == 0L) // invalid read stamp
                break;
            else if (m < RFULL) {
                if (casState(s, next = s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return next & SBITS;
                }
            }
            else if ((next = tryDecReaderOverflow(s)) != 0L)
                return next & SBITS;
        }
        return 0L;
    }

    /**
     * Releases the write lock if it is held, without requiring a
     * stamp value. This method may be useful for recovery after
     * errors.
     *
     * @return {@code true} if the lock was held, else false
     */
    @ReservedStackAccess
    public boolean tryUnlockWrite() {
        long s;
        if (((s = state) & WBIT) != 0L) {
            unlockWriteInternal(s);
            return true;
        }
        return false;
    }

    /**
     * Releases one hold of the read lock if it is held, without
     * requiring a stamp value. This method may be useful for recovery
     * after errors.
     *
     * @return {@code true} if the read lock was held, else false
     */
    @ReservedStackAccess
    public boolean tryUnlockRead() {
        long s, m; WNode h;
        while ((m = (s = state) & ABITS) != 0L && m < WBIT) {
            if (m < RFULL) {
                if (casState(s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return true;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                return true;
        }
        return false;
    }

    // status monitoring methods

    /**
     * Returns combined state-held and overflow read count for given
     * state s.
     */
    private int getReadLockCount(long s) {
        long readers;
        if ((readers = s & RBITS) >= RFULL)
            readers = RFULL + readerOverflow;
        return (int) readers;
    }

    /**
     * Returns {@code true} if the lock is currently held exclusively.
     *
     * @return {@code true} if the lock is currently held exclusively
     */
    public boolean isWriteLocked() {
        return (state & WBIT) != 0L;
    }

    /**
     * Returns {@code true} if the lock is currently held non-exclusively.
     *
     * @return {@code true} if the lock is currently held non-exclusively
     */
    public boolean isReadLocked() {
        return (state & RBITS) != 0L;
    }

    /**
     * Tells whether a stamp represents holding a lock exclusively.
     * This method may be useful in conjunction with
     * {@link #tryConvertToWriteLock}, for example: <pre> {@code
     * long stamp = sl.tryOptimisticRead();
     * try {
     *   ...
     *   stamp = sl.tryConvertToWriteLock(stamp);
     *   ...
     * } finally {
     *   if (StampedLock.isWriteLockStamp(stamp))
     *     sl.unlockWrite(stamp);
     * }}</pre>
     *
     * @param stamp a stamp returned by a previous StampedLock operation
     * @return {@code true} if the stamp was returned by a successful
     *   write-lock operation
     * @since 10
     */
    public static boolean isWriteLockStamp(long stamp) {
        return (stamp & ABITS) == WBIT;
    }

    /**
     * Tells whether a stamp represents holding a lock non-exclusively.
     * This method may be useful in conjunction with
     * {@link #tryConvertToReadLock}, for example: <pre> {@code
     * long stamp = sl.tryOptimisticRead();
     * try {
     *   ...
     *   stamp = sl.tryConvertToReadLock(stamp);
     *   ...
     * } finally {
     *   if (StampedLock.isReadLockStamp(stamp))
     *     sl.unlockRead(stamp);
     * }}</pre>
     *
     * @param stamp a stamp returned by a previous StampedLock operation
     * @return {@code true} if the stamp was returned by a successful
     *   read-lock operation
     * @since 10
     */
    public static boolean isReadLockStamp(long stamp) {
        return (stamp & RBITS) != 0L;
    }

    /**
     * Tells whether a stamp represents holding a lock.
     * This method may be useful in conjunction with
     * {@link #tryConvertToReadLock} and {@link #tryConvertToWriteLock},
     * for example: <pre> {@code
     * long stamp = sl.tryOptimisticRead();
     * try {
     *   ...
     *   stamp = sl.tryConvertToReadLock(stamp);
     *   ...
     *   stamp = sl.tryConvertToWriteLock(stamp);
     *   ...
     * } finally {
     *   if (StampedLock.isLockStamp(stamp))
     *     sl.unlock(stamp);
     * }}</pre>
     *
     * @param stamp a stamp returned by a previous StampedLock operation
     * @return {@code true} if the stamp was returned by a successful
     *   read-lock or write-lock operation
     * @since 10
     */
    public static boolean isLockStamp(long stamp) {
        return (stamp & ABITS) != 0L;
    }

    /**
     * Tells whether a stamp represents a successful optimistic read.
     *
     * @param stamp a stamp returned by a previous StampedLock operation
     * @return {@code true} if the stamp was returned by a successful
     *   optimistic read operation, that is, a non-zero return from
     *   {@link #tryOptimisticRead()} or
     *   {@link #tryConvertToOptimisticRead(long)}
     * @since 10
     */
    public static boolean isOptimisticReadStamp(long stamp) {
        return (stamp & ABITS) == 0L && stamp != 0L;
    }

    /**
     * Queries the number of read locks held for this lock. This
     * method is designed for use in monitoring system state, not for
     * synchronization control.
     * @return the number of read locks held
     */
    public int getReadLockCount() {
        return getReadLockCount(state);
    }

    /**
     * Returns a string identifying this lock, as well as its lock
     * state.  The state, in brackets, includes the String {@code
     * "Unlocked"} or the String {@code "Write-locked"} or the String
     * {@code "Read-locks:"} followed by the current number of
     * read-locks held.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        long s = state;
        return super.toString() +
            ((s & ABITS) == 0L ? "[Unlocked]" :
             (s & WBIT) != 0L ? "[Write-locked]" :
             "[Read-locks:" + getReadLockCount(s) + "]");
    }

    // views

    /**
     * Returns a plain {@link Lock} view of this StampedLock in which
     * the {@link Lock#lock} method is mapped to {@link #readLock},
     * and similarly for other methods. The returned Lock does not
     * support a {@link Condition}; method {@link Lock#newCondition()}
     * throws {@code UnsupportedOperationException}.
     *
     * @return the lock
     */
    public Lock asReadLock() {
        ReadLockView v;
        if ((v = readLockView) != null) return v;
        return readLockView = new ReadLockView();
    }

    /**
     * Returns a plain {@link Lock} view of this StampedLock in which
     * the {@link Lock#lock} method is mapped to {@link #writeLock},
     * and similarly for other methods. The returned Lock does not
     * support a {@link Condition}; method {@link Lock#newCondition()}
     * throws {@code UnsupportedOperationException}.
     *
     * @return the lock
     */
    public Lock asWriteLock() {
        WriteLockView v;
        if ((v = writeLockView) != null) return v;
        return writeLockView = new WriteLockView();
    }

    /**
     * Returns a {@link ReadWriteLock} view of this StampedLock in
     * which the {@link ReadWriteLock#readLock()} method is mapped to
     * {@link #asReadLock()}, and {@link ReadWriteLock#writeLock()} to
     * {@link #asWriteLock()}.
     *
     * @return the lock
     */
    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView v;
        if ((v = readWriteLockView) != null) return v;
        return readWriteLockView = new ReadWriteLockView();
    }

    // view classes

    final class ReadLockView implements Lock {
        public void lock() { readLock(); }
        public void lockInterruptibly() throws InterruptedException {
            readLockInterruptibly();
        }
        public boolean tryLock() { return tryReadLock() != 0L; }
        public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException {
            return tryReadLock(time, unit) != 0L;
        }
        public void unlock() { unstampedUnlockRead(); }
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class WriteLockView implements Lock {
        public void lock() { writeLock(); }
        public void lockInterruptibly() throws InterruptedException {
            writeLockInterruptibly();
        }
        public boolean tryLock() { return tryWriteLock() != 0L; }
        public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException {
            return tryWriteLock(time, unit) != 0L;
        }
        public void unlock() { unstampedUnlockWrite(); }
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class ReadWriteLockView implements ReadWriteLock {
        public Lock readLock() { return asReadLock(); }
        public Lock writeLock() { return asWriteLock(); }
    }

    // Unlock methods without stamp argument checks for view classes.
    // Needed because view-class lock methods throw away stamps.

    final void unstampedUnlockWrite() {
        long s;
        if (((s = state) & WBIT) == 0L)
            throw new IllegalMonitorStateException();
        unlockWriteInternal(s);
    }

    final void unstampedUnlockRead() {
        long s, m; WNode h;
        while ((m = (s = state) & RBITS) > 0L) {
            if (m < RFULL) {
                if (casState(s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                return;
        }
        throw new IllegalMonitorStateException();
    }

    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        STATE.setVolatile(this, ORIGIN); // reset to unlocked state
    }

    // internals

    /**
     * Tries to increment readerOverflow by first setting state
     * access bits value to RBITS, indicating hold of spinlock,
     * then updating, then releasing.
     *
     * @param s a reader overflow stamp: (s & ABITS) >= RFULL
     * @return new stamp on success, else zero
     */
    private long tryIncReaderOverflow(long s) {
        // assert (s & ABITS) >= RFULL;
        if ((s & ABITS) == RFULL) {
            if (casState(s, s | RBITS)) {
                ++readerOverflow;
                STATE.setVolatile(this, s);
                return s;
            }
        }
        else if ((LockSupport.nextSecondarySeed() & OVERFLOW_YIELD_RATE) == 0)
            Thread.yield();
        else
            Thread.onSpinWait();
        return 0L;
    }

    /**
     * Tries to decrement readerOverflow.
     *
     * @param s a reader overflow stamp: (s & ABITS) >= RFULL
     * @return new stamp on success, else zero
     */
    private long tryDecReaderOverflow(long s) {
        // assert (s & ABITS) >= RFULL;
        if ((s & ABITS) == RFULL) {
            if (casState(s, s | RBITS)) {
                int r; long next;
                if ((r = readerOverflow) > 0) {
                    readerOverflow = r - 1;
                    next = s;
                }
                else
                    next = s - RUNIT;
                STATE.setVolatile(this, next);
                return next;
            }
        }
        else if ((LockSupport.nextSecondarySeed() & OVERFLOW_YIELD_RATE) == 0)
            Thread.yield();
        else
            Thread.onSpinWait();
        return 0L;
    }

    /**
     * Wakes up the successor of h (normally whead). This is normally
     * just h.next, but may require traversal from wtail if next
     * pointers are lagging. This may fail to wake up an acquiring
     * thread when one or more have been cancelled, but the cancel
     * methods themselves provide extra safeguards to ensure liveness.
     */
    private void release(WNode h) {
        if (h != null) {
            WNode q; Thread w;
            WSTATUS.compareAndSet(h, WAITING, 0);
            if ((q = h.next) == null || q.status == CANCELLED) {
                for (WNode t = wtail; t != null && t != h; t = t.prev)
                    if (t.status <= 0)
                        q = t;
            }
            if (q != null && (w = q.thread) != null)
                LockSupport.unpark(w);
        }
    }

    /**
     * See above for explanation.
     *
     * @param interruptible true if should check interrupts and if so
     * return INTERRUPTED
     * @param deadline if nonzero, the System.nanoTime value to timeout
     * at (and return zero)
     * @return next state, or INTERRUPTED
     */
    private long acquireWrite(boolean interruptible, long deadline) {
        WNode node = null, p;
        for (int spins = -1;;) { // spin while enqueuing
            long m, s, ns;
            if ((m = (s = state) & ABITS) == 0L) {
                if ((ns = tryWriteLock(s)) != 0L)
                    return ns;
            }
            else if (spins < 0)
                spins = (m == WBIT && wtail == whead) ? SPINS : 0;
            else if (spins > 0) {
                --spins;
                Thread.onSpinWait();
            }
            else if ((p = wtail) == null) { // initialize queue
                WNode hd = new WNode(WMODE, null);
                if (WHEAD.weakCompareAndSet(this, null, hd))
                    wtail = hd;
            }
            else if (node == null)
                node = new WNode(WMODE, p);
            else if (node.prev != p)
                node.prev = p;
            else if (WTAIL.weakCompareAndSet(this, p, node)) {
                p.next = node;
                break;
            }
        }

        boolean wasInterrupted = false;
        for (int spins = -1;;) {
            WNode h, np, pp; int ps;
            if ((h = whead) == p) {
                if (spins < 0)
                    spins = HEAD_SPINS;
                else if (spins < MAX_HEAD_SPINS)
                    spins <<= 1;
                for (int k = spins; k > 0; --k) { // spin at head
                    long s, ns;
                    if (((s = state) & ABITS) == 0L) {
                        if ((ns = tryWriteLock(s)) != 0L) {
                            whead = node;
                            node.prev = null;
                            if (wasInterrupted)
                                Thread.currentThread().interrupt();
                            return ns;
                        }
                    }
                    else
                        Thread.onSpinWait();
                }
            }
            else if (h != null) { // help release stale waiters
                WNode c; Thread w;
                while ((c = h.cowait) != null) {
                    if (WCOWAIT.weakCompareAndSet(h, c, c.cowait) &&
                        (w = c.thread) != null)
                        LockSupport.unpark(w);
                }
            }
            if (whead == h) {
                if ((np = node.prev) != p) {
                    if (np != null)
                        (p = np).next = node;   // stale
                }
                else if ((ps = p.status) == 0)
                    WSTATUS.compareAndSet(p, 0, WAITING);
                else if (ps == CANCELLED) {
                    if ((pp = p.prev) != null) {
                        node.prev = pp;
                        pp.next = node;
                    }
                }
                else {
                    long time; // 0 argument to park means no timeout
                    if (deadline == 0L)
                        time = 0L;
                    else if ((time = deadline - System.nanoTime()) <= 0L)
                        return cancelWaiter(node, node, false);
                    Thread wt = Thread.currentThread();
                    node.thread = wt;
                    if (p.status < 0 && (p != h || (state & ABITS) != 0L) &&
                        whead == h && node.prev == p) {
                        if (time == 0L)
                            LockSupport.park(this);
                        else
                            LockSupport.parkNanos(this, time);
                    }
                    node.thread = null;
                    if (Thread.interrupted()) {
                        if (interruptible)
                            return cancelWaiter(node, node, true);
                        wasInterrupted = true;
                    }
                }
            }
        }
    }

    /**
     * See above for explanation.
     *
     * @param interruptible true if should check interrupts and if so
     * return INTERRUPTED
     * @param deadline if nonzero, the System.nanoTime value to timeout
     * at (and return zero)
     * @return next state, or INTERRUPTED
     */
    private long acquireRead(boolean interruptible, long deadline) {
        boolean wasInterrupted = false;
        WNode node = null, p;
        for (int spins = -1;;) {
            WNode h;
            if ((h = whead) == (p = wtail)) {
                for (long m, s, ns;;) {
                    if ((m = (s = state) & ABITS) < RFULL ?
                        casState(s, ns = s + RUNIT) :
                        (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L)) {
                        if (wasInterrupted)
                            Thread.currentThread().interrupt();
                        return ns;
                    }
                    else if (m >= WBIT) {
                        if (spins > 0) {
                            --spins;
                            Thread.onSpinWait();
                        }
                        else {
                            if (spins == 0) {
                                WNode nh = whead, np = wtail;
                                if ((nh == h && np == p) || (h = nh) != (p = np))
                                    break;
                            }
                            spins = SPINS;
                        }
                    }
                }
            }
            if (p == null) { // initialize queue
                WNode hd = new WNode(WMODE, null);
                if (WHEAD.weakCompareAndSet(this, null, hd))
                    wtail = hd;
            }
            else if (node == null)
                node = new WNode(RMODE, p);
            else if (h == p || p.mode != RMODE) {
                if (node.prev != p)
                    node.prev = p;
                else if (WTAIL.weakCompareAndSet(this, p, node)) {
                    p.next = node;
                    break;
                }
            }
            else if (!WCOWAIT.compareAndSet(p, node.cowait = p.cowait, node))
                node.cowait = null;
            else {
                for (;;) {
                    WNode pp, c; Thread w;
                    if ((h = whead) != null && (c = h.cowait) != null &&
                        WCOWAIT.compareAndSet(h, c, c.cowait) &&
                        (w = c.thread) != null) // help release
                        LockSupport.unpark(w);
                    if (Thread.interrupted()) {
                        if (interruptible)
                            return cancelWaiter(node, p, true);
                        wasInterrupted = true;
                    }
                    if (h == (pp = p.prev) || h == p || pp == null) {
                        long m, s, ns;
                        do {
                            if ((m = (s = state) & ABITS) < RFULL ?
                                casState(s, ns = s + RUNIT) :
                                (m < WBIT &&
                                 (ns = tryIncReaderOverflow(s)) != 0L)) {
                                if (wasInterrupted)
                                    Thread.currentThread().interrupt();
                                return ns;
                            }
                        } while (m < WBIT);
                    }
                    if (whead == h && p.prev == pp) {
                        long time;
                        if (pp == null || h == p || p.status > 0) {
                            node = null; // throw away
                            break;
                        }
                        if (deadline == 0L)
                            time = 0L;
                        else if ((time = deadline - System.nanoTime()) <= 0L) {
                            if (wasInterrupted)
                                Thread.currentThread().interrupt();
                            return cancelWaiter(node, p, false);
                        }
                        Thread wt = Thread.currentThread();
                        node.thread = wt;
                        if ((h != pp || (state & ABITS) == WBIT) &&
                            whead == h && p.prev == pp) {
                            if (time == 0L)
                                LockSupport.park(this);
                            else
                                LockSupport.parkNanos(this, time);
                        }
                        node.thread = null;
                    }
                }
            }
        }

        for (int spins = -1;;) {
            WNode h, np, pp; int ps;
            if ((h = whead) == p) {
                if (spins < 0)
                    spins = HEAD_SPINS;
                else if (spins < MAX_HEAD_SPINS)
                    spins <<= 1;
                for (int k = spins;;) { // spin at head
                    long m, s, ns;
                    if ((m = (s = state) & ABITS) < RFULL ?
                        casState(s, ns = s + RUNIT) :
                        (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L)) {
                        WNode c; Thread w;
                        whead = node;
                        node.prev = null;
                        while ((c = node.cowait) != null) {
                            if (WCOWAIT.compareAndSet(node, c, c.cowait) &&
                                (w = c.thread) != null)
                                LockSupport.unpark(w);
                        }
                        if (wasInterrupted)
                            Thread.currentThread().interrupt();
                        return ns;
                    }
                    else if (m >= WBIT && --k <= 0)
                        break;
                    else
                        Thread.onSpinWait();
                }
            }
            else if (h != null) {
                WNode c; Thread w;
                while ((c = h.cowait) != null) {
                    if (WCOWAIT.compareAndSet(h, c, c.cowait) &&
                        (w = c.thread) != null)
                        LockSupport.unpark(w);
                }
            }
            if (whead == h) {
                if ((np = node.prev) != p) {
                    if (np != null)
                        (p = np).next = node;   // stale
                }
                else if ((ps = p.status) == 0)
                    WSTATUS.compareAndSet(p, 0, WAITING);
                else if (ps == CANCELLED) {
                    if ((pp = p.prev) != null) {
                        node.prev = pp;
                        pp.next = node;
                    }
                }
                else {
                    long time;
                    if (deadline == 0L)
                        time = 0L;
                    else if ((time = deadline - System.nanoTime()) <= 0L)
                        return cancelWaiter(node, node, false);
                    Thread wt = Thread.currentThread();
                    node.thread = wt;
                    if (p.status < 0 &&
                        (p != h || (state & ABITS) == WBIT) &&
                        whead == h && node.prev == p) {
                            if (time == 0L)
                                LockSupport.park(this);
                            else
                                LockSupport.parkNanos(this, time);
                    }
                    node.thread = null;
                    if (Thread.interrupted()) {
                        if (interruptible)
                            return cancelWaiter(node, node, true);
                        wasInterrupted = true;
                    }
                }
            }
        }
    }

    /**
     * If node non-null, forces cancel status and unsplices it from
     * queue if possible and wakes up any cowaiters (of the node, or
     * group, as applicable), and in any case helps release current
     * first waiter if lock is free. (Calling with null arguments
     * serves as a conditional form of release, which is not currently
     * needed but may be needed under possible future cancellation
     * policies). This is a variant of cancellation methods in
     * AbstractQueuedSynchronizer (see its detailed explanation in AQS
     * internal documentation).
     *
     * @param node if non-null, the waiter
     * @param group either node or the group node is cowaiting with
     * @param interrupted if already interrupted
     * @return INTERRUPTED if interrupted or Thread.interrupted, else zero
     */
    private long cancelWaiter(WNode node, WNode group, boolean interrupted) {
        if (node != null && group != null) {
            Thread w;
            node.status = CANCELLED;
            // unsplice cancelled nodes from group
            for (WNode p = group, q; (q = p.cowait) != null;) {
                if (q.status == CANCELLED) {
                    WCOWAIT.compareAndSet(p, q, q.cowait);
                    p = group; // restart
                }
                else
                    p = q;
            }
            if (group == node) {
                for (WNode r = group.cowait; r != null; r = r.cowait) {
                    if ((w = r.thread) != null)
                        LockSupport.unpark(w); // wake up uncancelled co-waiters
                }
                for (WNode pred = node.prev; pred != null; ) { // unsplice
                    WNode succ, pp;        // find valid successor
                    while ((succ = node.next) == null ||
                           succ.status == CANCELLED) {
                        WNode q = null;    // find successor the slow way
                        for (WNode t = wtail; t != null && t != node; t = t.prev)
                            if (t.status != CANCELLED)
                                q = t;     // don't link if succ cancelled
                        if (succ == q ||   // ensure accurate successor
                            WNEXT.compareAndSet(node, succ, succ = q)) {
                            if (succ == null && node == wtail)
                                WTAIL.compareAndSet(this, node, pred);
                            break;
                        }
                    }
                    if (pred.next == node) // unsplice pred link
                        WNEXT.compareAndSet(pred, node, succ);
                    if (succ != null && (w = succ.thread) != null) {
                        // wake up succ to observe new pred
                        succ.thread = null;
                        LockSupport.unpark(w);
                    }
                    if (pred.status != CANCELLED || (pp = pred.prev) == null)
                        break;
                    node.prev = pp;        // repeat if new pred wrong/cancelled
                    WNEXT.compareAndSet(pp, pred, succ);
                    pred = pp;
                }
            }
        }
        WNode h; // Possibly release first waiter
        while ((h = whead) != null) {
            long s; WNode q; // similar to release() but check eligibility
            if ((q = h.next) == null || q.status == CANCELLED) {
                for (WNode t = wtail; t != null && t != h; t = t.prev)
                    if (t.status <= 0)
                        q = t;
            }
            if (h == whead) {
                if (q != null && h.status == 0 &&
                    ((s = state) & ABITS) != WBIT && // waiter is eligible
                    (s == 0L || q.mode == RMODE))
                    release(h);
                break;
            }
        }
        return (interrupted || Thread.interrupted()) ? INTERRUPTED : 0L;
    }

    // VarHandle mechanics
    private static final VarHandle STATE;
    private static final VarHandle WHEAD;
    private static final VarHandle WTAIL;
    private static final VarHandle WNEXT;
    private static final VarHandle WSTATUS;
    private static final VarHandle WCOWAIT;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            STATE = l.findVarHandle(StampedLock.class, "state", long.class);
            WHEAD = l.findVarHandle(StampedLock.class, "whead", WNode.class);
            WTAIL = l.findVarHandle(StampedLock.class, "wtail", WNode.class);
            WSTATUS = l.findVarHandle(WNode.class, "status", int.class);
            WNEXT = l.findVarHandle(WNode.class, "next", WNode.class);
            WCOWAIT = l.findVarHandle(WNode.class, "cowait", WNode.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
