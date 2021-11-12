package java.util.concurrent.locks;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import jdk.internal.vm.annotation.ReservedStackAccess;

/**
 * ReentrantReadWriteLock 类, 顾名思义, 是一种读写锁, 它是 ReadWriteLock 接口的直接实现,
 * 该类在内部实现了具体独占锁特点的写锁, 以及具有共享锁特点的读锁, 和 ReentrantLock 一样,
 * ReentrantReadWriteLock 类也是通过定义内部类实现AQS框架的API来实现独占/共享的功能.
 *
 * ReentrantLock 属于排他锁, 这些锁在同一时刻只允许一个线程进行访问, 但是在大多数场景下, 大部分时间都是提供读服务,
 * 而写服务占有的时间较少. 而且, 读服务不存在数据竞争问题, 如果一个线程在读时禁止其他线程读势必会导致性能降低. 所以就提供了读写锁.
 *
 * 读写锁维护着一对锁, 一个读锁和一个写锁. 通过分离读锁和写锁, 使得并发性比一般的排他锁有了较大的提升:
 *
 * 在同一时间, 可以允许多个读线程同时访问.
 * 但是, 在写线程访问时, 所有读线程和写线程都会被阻塞.
 * 读写锁的主要特性:
 *
 * 公平性：支持公平性和非公平性.
 * 重入性：支持重入. 读写锁最多支持 65535 个递归写入锁和 65535 个递归读取锁.
 * 锁降级：遵循获取写锁, 再获取读锁, 最后释放写锁的次序, 如此写锁能够降级成为读锁.
 */
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable {
    private static final long serialVersionUID = -6992448646407690164L;
    /** 提供读锁的内部类 */
    private final ReadLock readerLock;
    /** 提供写锁的内部类 */
    private final WriteLock writerLock;
    /** 执行所有同步机制 */
    final Sync sync;

    /**
     * 创建一个具有默认（非公平）排序属性的新 ReentrantReadWriteLock。
     */
    public ReentrantReadWriteLock() {
        this(false);
    }

    /**
     * 使用给定的公平策略创建一个新的 ReentrantReadWriteLock。
     */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    public WriteLock writeLock() { return writerLock; }
    public ReadLock  readLock()  { return readerLock; }

    /**
     * ReentrantReadWriteLock 的同步实现。 分为公平和非公平版本。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 6317671515068378041L;

        /**
         * 读取与写入计数提取常量和函数。 锁状态在逻辑上分为两个无符号shorts：
         * 较低的代表独占（写入者）锁持有计数，较高的代表共享（读取者）持有计数。
         */

        static final int SHARED_SHIFT   = 16;
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

        /** 返回以计数表示的共享保留数。 */
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
        /** 返回以计数表示的独占保留数。 */
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }

        /**
         * 每个线程读取保持计数的计数器。 作为 ThreadLocal 维护； 缓存在 cachedHoldCounter 中。
         */
        static final class HoldCounter {
            int count;          // initially 0
            // Use id, not reference, to avoid garbage retention
            final long tid = LockSupport.getThreadId(Thread.currentThread());
        }

        /**
         * ThreadLocal 子类。 为了反序列化机制，最容易明确定义。
         */
        static final class ThreadLocalHoldCounter
            extends ThreadLocal<HoldCounter> {
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        /**
         * 当前线程持有的可重入读锁的数量。 仅在构造函数和 readObject 中初始化。 每当线程的读取保持计数下降到 0 时删除。
         */
        private transient ThreadLocalHoldCounter readHolds;

        /**
         * 成功获取 readLock 的最后一个线程的保持计数。 在下一个要释放的线程是最后一个要获取的线程的常见情况下，
         * 这可以节省 ThreadLocal 查找。 这是非易失性的，因为它仅用作启发式方法，并且非常适合线程缓存。
         * 可以比它缓存读取保持计数的线程寿命更长，但通过不保留对线程的引用来避免垃圾保留。
         * 通过良性数据竞争访问； 依赖于内存模型的最终字段和凭空保证。
         */
        private transient HoldCounter cachedHoldCounter;

        /**
         * firstReader 是第一个获得读锁的线程。 firstReaderHoldCount 是 firstReader 的保留计数。
         * 更准确地说，firstReader 是最后一次将共享计数从 0 更改为 1 的唯一线程，此后一直没有释放读锁；
         * 如果没有这样的线程，则为 null。
         * 除非线程在不放弃其读锁的情况下终止，否则不会导致垃圾保留，因为 tryReleaseShared 将其设置为 null。
         * 通过良性数据竞争访问； 依赖于内存模型对引用的无中生有的保证。
         * 这允许跟踪无竞争读锁的读保持非常便宜。
         */
        private transient Thread firstReader;
        private transient int firstReaderHoldCount;

        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // ensures visibility of readHolds
        }

        /**
         * 获取和释放对公平和非公平锁使用相同的代码，但在队列非空时它们是否/如何允许插入不同。
         */

        /**
         * 如果当前线程在尝试获取 读锁时以及有资格这样做时，应该由于 overtake 其他等待线程的策略而阻塞，则返回 true。
         */
        abstract boolean readerShouldBlock();

        /**
         * 如果当前线程在尝试获取 写锁时以及有资格这样做时，应该由于 overtake 其他等待线程的策略而阻塞，则返回 true。
         */
        abstract boolean writerShouldBlock();

        /**
         * 注意 tryRelease 和 tryAcquire 可以被条件调用。
         * 因此，它们的参数可能包含在条件等待期间全部释放并在 tryAcquire 中重新建立的读取和写入保持。
         */
        @ReservedStackAccess
        protected final boolean tryRelease(int releases) {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            int nextc = getState() - releases;
            boolean free = exclusiveCount(nextc) == 0;
            if (free) {
                setExclusiveOwnerThread(null);
            }
            setState(nextc);
            return free;
        }

        @ReservedStackAccess
        protected final boolean tryAcquire(int acquires) {
            /**
             * 演练：
             *    1. 如果读取计数非零或写入计数非零且所有者是不同的线程，则失败。
             *    2. 如果计数会饱和，则失败。 （只有在 count 已经非零时才会发生这种情况。）
             *    3. 否则，如果该线程是可重入获取或队列策略允许，则该线程有资格获得锁定。 如果是，请更新状态并设置所有者。
             */
            Thread current = Thread.currentThread();
            int c = getState();
            int w = exclusiveCount(c);
            if (c != 0) {
                // (Note: if c != 0 and w == 0 then shared count != 0)
                if (w == 0 || current != getExclusiveOwnerThread()) {
                    return false;
                }
                if (w + exclusiveCount(acquires) > MAX_COUNT) {
                    throw new Error("Maximum lock count exceeded");
                }
                // Reentrant acquire
                setState(c + acquires);
                return true;
            }
            if (writerShouldBlock() || !compareAndSetState(c, c + acquires)) {
                return false;
            }
            setExclusiveOwnerThread(current);
            return true;
        }

        @ReservedStackAccess
        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();
            if (firstReader == current) {
                // assert firstReaderHoldCount > 0;
                if (firstReaderHoldCount == 1)
                    firstReader = null;
                else
                    firstReaderHoldCount--;
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null ||
                    rh.tid != LockSupport.getThreadId(current))
                    rh = readHolds.get();
                int count = rh.count;
                if (count <= 1) {
                    readHolds.remove();
                    if (count <= 0)
                        throw unmatchedUnlockException();
                }
                --rh.count;
            }
            for (;;) {
                int c = getState();
                int nextc = c - SHARED_UNIT;
                if (compareAndSetState(c, nextc)) {
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    return nextc == 0;
                }
            }
        }

        private static IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException(
                "attempt to unlock read lock, not locked by current thread");
        }

        @ReservedStackAccess
        protected final int tryAcquireShared(int unused) {
            /**
             * 演练：
             *    1. 如果写锁被另一个线程持有，则失败。
             *    2. 否则，该线程有资格获得锁写入状态，因此询问它是否应该因为队列策略而阻塞。
             *       如果没有，请尝试通过 CASing 状态和更新计数来授予。
             *       请注意，步骤不检查可重入获取，它被推迟到完整版本以避免在更典型的非可重入情况下检查保持计数。
             *    3. 如果第 2 步由于线程显然不符合条件或 CAS 失败或计数饱和而失败，则链接到具有完整重试循环的版本。
             */
            Thread current = Thread.currentThread();
            int c = getState();
            if (exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current) {
                return -1;
            }
            int r = sharedCount(c);
            if (!readerShouldBlock() &&
                r < MAX_COUNT &&
                compareAndSetState(c, c + SHARED_UNIT)) {
                if (r == 0) {
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    firstReaderHoldCount++;
                } else {
                    HoldCounter rh = cachedHoldCounter;
                    if (rh == null || rh.tid != LockSupport.getThreadId(current)) {
                        cachedHoldCounter = rh = readHolds.get();
                    } else if (rh.count == 0) {
                        readHolds.set(rh);
                    }
                    rh.count++;
                }
                return 1;
            }
            return fullTryAcquireShared(current);
        }

        /**
         * 完整版本的获取读取，处理 CAS 未命中和未在 tryAcquireShared 中处理的可重入读取。
         */
        final int fullTryAcquireShared(Thread current) {
            /**
             * 此代码与 tryAcquireShared 中的代码部分冗余，但总体上更简单，
             * 因为不会使 tryAcquireShared 与重试和延迟读取保持计数之间的交互复杂化。
             */
            HoldCounter rh = null;
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0) {
                    if (getExclusiveOwnerThread() != current) {
                        return -1;
                    }
                    // else we hold the exclusive lock; blocking here
                    // would cause deadlock.
                } else if (readerShouldBlock()) {
                    // Make sure we're not acquiring read lock reentrantly
                    if (firstReader == current) {
                        // assert firstReaderHoldCount > 0;
                    } else {
                        if (rh == null) {
                            rh = cachedHoldCounter;
                            if (rh == null ||
                                rh.tid != LockSupport.getThreadId(current)) {
                                rh = readHolds.get();
                                if (rh.count == 0) {
                                    readHolds.remove();
                                }
                            }
                        }
                        if (rh.count == 0) {
                            return -1;
                        }
                    }
                }
                if (sharedCount(c) == MAX_COUNT) {
                    throw new Error("Maximum lock count exceeded");
                }
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (sharedCount(c) == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        if (rh == null) {
                            rh = cachedHoldCounter;
                        }
                        if (rh == null || rh.tid != LockSupport.getThreadId(current)) {
                            rh = readHolds.get();
                        } else if (rh.count == 0) {
                            readHolds.set(rh);
                        }
                        rh.count++;
                        cachedHoldCounter = rh; // cache for release
                    }
                    return 1;
                }
            }
        }

        /**
         * 执行 tryLock 以进行写入，从而在两种模式下启用插入。 这与 tryAcquire 的效果相同，只是缺少对 writerShouldBlock 的调用。
         */
        @ReservedStackAccess
        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c);
                if (w == 0 || current != getExclusiveOwnerThread()) {
                    return false;
                }
                if (w == MAX_COUNT) {
                    throw new Error("Maximum lock count exceeded");
                }
            }
            if (!compareAndSetState(c, c + 1)) {
                return false;
            }
            setExclusiveOwnerThread(current);
            return true;
        }

        /**
         * 执行 tryLock 以进行读取，从而在两种模式下启用插入。
         * 除了缺少对 readerShouldBlock 的调用之外，这与 tryAcquireShared 的效果相同。
         */
        @ReservedStackAccess
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current) {
                    return false;
                }
                int r = sharedCount(c);
                if (r == MAX_COUNT) {
                    throw new Error("Maximum lock count exceeded");
                }
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        if (rh == null || rh.tid != LockSupport.getThreadId(current)) {
                            cachedHoldCounter = rh = readHolds.get();
                        }
                        else if (rh.count == 0) {
                            readHolds.set(rh);
                        }
                        rh.count++;
                    }
                    return true;
                }
            }
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // Methods relayed to outer class

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        final Thread getOwner() {
            // Must read state before owner to ensure memory consistency
            return ((exclusiveCount(getState()) == 0) ?
                    null :
                    getExclusiveOwnerThread());
        }

        final int getReadLockCount() {
            return sharedCount(getState());
        }

        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }

        final int getWriteHoldCount() {
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }

        final int getReadHoldCount() {
            if (getReadLockCount() == 0)
                return 0;

            Thread current = Thread.currentThread();
            if (firstReader == current)
                return firstReaderHoldCount;

            HoldCounter rh = cachedHoldCounter;
            if (rh != null && rh.tid == LockSupport.getThreadId(current))
                return rh.count;

            int count = readHolds.get().count;
            if (count == 0) readHolds.remove();
            return count;
        }

        /**
         * 从流中重构实例（即反序列化它）。
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            readHolds = new ThreadLocalHoldCounter();
            setState(0); // reset to unlocked state
        }

        final int getCount() { return getState(); }
    }

    /**
     * 同步的非公平版本
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -8159625535654395037L;
        final boolean writerShouldBlock() {
            return false; // writers can always barge
        }
        final boolean readerShouldBlock() {
            /**
             * 作为一种避免无限写入器饥饿的启发式方法，如果暂时出现在队列头（如果存在）的线程是等待写入器，则阻塞。
             * 这只是一种概率效应，因为如果在其他尚未从队列中排出的已启用读取器后面有等待写入器，则新读取器不会阻塞。
             */
            return apparentlyFirstQueuedIsExclusive();
        }
    }

    /**
     * 同步的公平版本
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -2274990926593161451L;
        final boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }
        final boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }

    /**
     * 方法 readLock 返回的锁。
     */
    public static class ReadLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        private final Sync sync;

        /**
         * 供子类使用的构造函数。
         */
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 获取读锁。
         * 如果写锁未被另一个线程持有，则获取读锁并立即返回。
         * 如果写锁被另一个线程持有，那么当前线程将因线程调度目的而被禁用，并处于休眠状态，直到获得读锁为止。
         */
        public void lock() {
            sync.acquireShared(1);
        }

        /**
         * 除非当前线程被中断，否则获取读锁。
         * 如果写锁未被另一个线程持有，则获取读锁并立即返回。
         * 如果写锁由另一个线程持有，则当前线程将出于线程调度目的而被禁用并处于休眠状态，直到发生以下两种情况之一：
         *  1. 读锁由当前线程获取； 或者
         *  2. 一些其他线程中断当前线程。
         * 如果当前线程：
         *  1. 在进入此方法时设置其中断状态； 或者
         *  2. 在获取读锁时被中断，
         * 然后抛出 InterruptedException 并清除当前线程的中断状态。
         * 在此实现中，由于此方法是显式中断点，因此优先响应中断而不是正常或可重入获取锁。
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        /**
         * 仅当调用时另一个线程未持有写锁时才获取读锁。
         * 如果写锁未被另一个线程持有，则获取读锁并立即返回值为 true。
         * 即使此锁已设置为使用公平排序策略，调用 tryLock() 将立即获取可用的读锁，无论其他线程当前是否正在等待读锁。
         * 这种“闯入”行为在某些情况下很有用，即使它破坏了公平。
         * 如果你想尊重这个锁的公平性设置，那么使用 tryLock(0, TimeUnit.SECONDS) 这几乎是等效的（它也检测中断）。
         * 如果写锁被另一个线程持有，则此方法将立即返回 false 值。
         */
        public boolean tryLock() {
            return sync.tryReadLock();
        }

        /**
         * 如果写锁在给定的等待时间内没有被另一个线程持有并且当前线程没有被中断，则获取读锁。
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        /**
         * 尝试释放此锁。
         * 如果读取器的数量现在为零，则该锁可用于写锁尝试。 如果当前线程不持有此锁，则抛出 IllegalMonitorStateException
         */
        public void unlock() {
            sync.releaseShared(1);
        }

        /**
         * 抛出 UnsupportedOperationException 因为 ReadLocks 不支持条件。
         */
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        /**
         * 返回标识此锁及其锁状态的字符串。 括号中的状态包括字符串“Read locks =”，后跟持有的读锁数。
         */
        public String toString() {
            int r = sync.getReadLockCount();
            return super.toString() +
                "[Read locks = " + r + "]";
        }
    }

    /**
     * 方法 writeLock 返回的锁。
     */
    public static class WriteLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -4992448646407690164L;
        private final Sync sync;

        /**
         * 供子类使用的构造函数。
         */
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 获取写锁。
         * 如果读锁和写锁都没有被另一个线程持有，则获取写锁并立即返回，将写锁持有计数设置为 1。
         * 如果当前线程已经持有写锁，则持有计数加一并且该方法立即返回。
         * 如果该锁由另一个线程持有，则当前线程将出于线程调度目的而被禁用并处于休眠状态，直到获得写锁为止，此时写锁持有计数设置为 1。
         */
        public void lock() {
            sync.acquire(1);
        }

        /**
         * 除非当前线程被中断，否则获取写锁。
         * 如果读锁和写锁都没有被另一个线程持有，则获取写锁并立即返回，将写锁持有计数设置为 1。
         * 如果当前线程已经持有这个锁，那么持有计数就会增加一并且该方法立即返回。
         * 如果锁被另一个线程持有，那么当前线程将被禁用以进行线程调度并处于休眠状态，直到发生以下两种情况之一：
         *  1. 写锁由当前线程获取；或者
         *  2. 一些其他线程中断当前线程。
         * 如果当前线程获取了写锁，则锁保持计数设置为 1。
         * 如果当前线程：
         *  1. 在进入此方法时设置其中断状态；或者
         *  2. 在获取写锁时被中断，
         * 然后抛出 InterruptedException 并清除当前线程的中断状态。
         * 在此实现中，由于此方法是显式中断点，因此优先响应中断而不是正常或可重入获取锁。
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        /**
         * 仅当在调用时未被另一个线程持有时才获取写锁。
         * 如果读锁和写锁都没有被另一个线程持有，则获取写锁，并立即返回值为 true，将写锁持有计数设置为 1。
         * 即使此锁已设置为使用公平排序策略，调用 tryLock() 也会立即获取该锁（如果可用），无论其他线程当前是否正在等待写锁。
         * 这种“闯入”行为在某些情况下很有用，即使它破坏了公平。
         * 如果你想尊重这个锁的公平性设置，那么使用 tryLock(0, TimeUnit.SECONDS) 这几乎是等效的（它也检测中断）。
         * 如果当前线程已持有此锁，则持有计数将增加 1，并且该方法返回 true。
         * 如果锁被另一个线程持有，那么这个方法将立即返回 false 值。
         */
        public boolean tryLock() {
            return sync.tryWriteLock();
        }

        /**
         * 如果在给定的等待时间内没有被另一个线程持有并且当前线程没有被中断，则获取写锁。
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }

        /**
         * 尝试释放此锁。
         * 如果当前线程是此锁的持有者，则持有计数递减。 如果保持计数现在为零，则锁定被释放。
         * 如果当前线程不是此锁的持有者，则抛出 IllegalMonitorStateException。
         */
        public void unlock() {
            sync.release(1);
        }

        /**
         * 返回与此 Lock 实例一起使用的 Condition 实例。
         *
         * 当与内置监视器锁一起使用时，返回的 Condition 实例支持与对象监视器方法（wait、notify 和 notifyAll）相同的用法。
         *  1. 如果在调用任何 Condition 方法时未持有此写锁，则会引发 IllegalMonitorStateException。
         *     （读锁独立于写锁，因此不会被检查或影响。然而，当当前线程也获得了读锁时，调用条件等待方法本质上总是一个错误，
         *     因为其他线程可以解除阻塞它不会被能够获取写锁。）
         *  2. 当条件等待方法被调用时，写锁被释放，在它们返回之前，写锁被重新获取，锁保持计数恢复到调用方法时的状态。
         *  3. 如果线程在等待时被中断，则等待将终止，将抛出 InterruptedException，并清除线程的中断状态。
         *  4. 等待线程按 FIFO 顺序发出信号。
         *  5. 从等待方法返回的线程重新获取锁的顺序与最初获取锁的线程相同，在默认情况下未指定，但对于公平锁，那些等待时间最长的线程优先。
         */
        public Condition newCondition() {
            return sync.newCondition();
        }

        /**
         * 返回标识此锁及其锁状态的字符串。 括号中的状态包括字符串“Unlocked”或字符串“Locked by”，后跟拥有线程的名称。
         */
        public String toString() {
            Thread o = sync.getOwner();
            return super.toString() + ((o == null) ?
                                       "[Unlocked]" :
                                       "[Locked by thread " + o.getName() + "]");
        }

        /**
         * 查询当前线程是否持有此写锁。 与 isWriteLockedByCurrentThread 效果相同。
         */
        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }

        /**
         * 查询当前线程持有该写锁的次数。 一个线程为每个与解锁操作不匹配的锁操作持有一个锁。 与 getWriteHoldCount 效果相同。
         */
        public int getHoldCount() {
            return sync.getWriteHoldCount();
        }
    }

    // Instrumentation and status

    /**
     * 如果此锁的公平性设置为 true，则返回 true。
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 返回当前拥有写锁的线程，如果不拥有，则返回 null。
     * 当此方法由不是所有者的线程调用时，返回值反映了当前锁定状态的尽力而为的近似值。
     * 例如，即使有线程试图获取锁但尚未这样做，所有者也可能暂时为空。 此方法旨在促进子类的构建，以提供更广泛的锁监控设施。
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 查询为此锁持有的读锁数。 该方法设计用于监视系统状态，而不是用于同步控制。
     */
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    /**
     * 查询写锁是否被任何线程持有。 该方法设计用于监视系统状态，而不是用于同步控制。
     */
    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }

    /**
     * 查询当前线程是否持有写锁。
     */
    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 查询当前线程对该锁的可重入写持有次数。 一个写线程为每个与解锁操作不匹配的锁操作持有一个锁。
     */
    public int getWriteHoldCount() {
        return sync.getWriteHoldCount();
    }

    /**
     * 查询当前线程对该锁持有的可重入读次数。 读取器线程为每个与解锁操作不匹配的锁操作持有一个锁。
     */
    public int getReadHoldCount() {
        return sync.getReadHoldCount();
    }

    /**
     * 返回一个包含可能正在等待获取写锁的线程的集合。 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
     * 返回集合的元素没有特定的顺序。 此方法旨在促进子类的构建，以提供更广泛的锁监控设施。
     */
    protected Collection<Thread> getQueuedWriterThreads() {
        return sync.getExclusiveQueuedThreads();
    }

    /**
     * 返回一个包含可能正在等待获取读锁的线程的集合。 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
     * 返回集合的元素没有特定的顺序。 此方法旨在促进子类的构建，以提供更广泛的锁监控设施。
     */
    protected Collection<Thread> getQueuedReaderThreads() {
        return sync.getSharedQueuedThreads();
    }

    /**
     * 查询是否有线程正在等待获取读锁或写锁。 请注意，因为取消可能随时发生，返回 true 并不能保证任何其他线程将永远获得锁。
     * 该方法主要用于监控系统状态。
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 查询给定线程是否正在等待获取读锁或写锁。 请注意，因为取消可能随时发生，返回 true 并不能保证该线程将永远获得锁。
     * 该方法主要用于监控系统状态。
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * 返回等待获取读锁或写锁的线程数的估计值。 该值只是一个估计值，因为当此方法遍历内部数据结构时，线程数可能会动态变化。
     * 该方法设计用于监视系统状态，而不是用于同步控制。
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 返回一个包含可能正在等待获取读锁或写锁的线程的集合。 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
     * 返回集合的元素没有特定的顺序。 此方法旨在促进子类的构建，以提供更广泛的监视设施。
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 查询是否有任何线程正在等待与写锁关联的给定条件。 请注意，因为超时和中断可能随时发生，返回 true 并不能保证未来的信号会唤醒任何线程。 该方法主要用于监控系统状态。
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null) {
            throw new NullPointerException();
        }
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 返回等待与写锁关联的给定条件的线程数的估计值。 请注意，由于超时和中断可能随时发生，因此估计值仅用作实际服务员人数的上限。
     * 此方法设计用于监视系统状态，而不是用于同步控制。
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null) {
            throw new NullPointerException();
        }
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 返回一个集合，其中包含那些可能正在等待与写锁关联的给定条件的线程。
     * 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
     * 返回集合的元素没有特定的顺序。 此方法旨在促进子类的构建，以提供更广泛的状态监控设施。
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null) {
            throw new NullPointerException();
        }
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 返回标识此锁及其锁状态的字符串。
     * 括号中的状态包括字符串“Write locks ="后跟可重入持有的写锁数量，以及字符串“Read locks ="后跟持有的读锁数量。
     */
    public String toString() {
        int c = sync.getCount();
        int w = Sync.exclusiveCount(c);
        int r = Sync.sharedCount(c);

        return super.toString() +
            "[Write locks = " + w + ", Read locks = " + r + "]";
    }

}
