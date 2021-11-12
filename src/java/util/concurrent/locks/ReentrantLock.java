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

package java.util.concurrent.locks;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import jdk.internal.vm.annotation.ReservedStackAccess;

/**
 * 可重入互斥锁与使用同步方法和语句访问的隐式监视器锁具有相同的基本行为和语义，但具有扩展功能。
 *
 * 为了保证任何时刻只有一个线程能进入临界区，通常需要给临界区上锁，只有获得锁的线程才能进入临界区。
 * 为了达到上锁的目的，我们通常使用synchronized关键字。
 * 在Java SE 5.0之后，java引入了一个ReentrantLock类，也可以实现给代码块上锁和释放锁的效果。
 */
public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    /** 提供所有实现机制的同步器 */
    private final Sync sync;

    /**
     * 此锁的同步控制基础。 下面细分为公平和非公平版本。 使用 AQS 状态来表示锁的持有次数。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * 执行不公平的 tryLock。 tryAcquire 在子类中实现，但两者都需要对 trylock 方法进行非公平尝试。
         */
        @ReservedStackAccess
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) {// overflow
                    throw new Error("Maximum lock count exceeded");
                }
                setState(nextc);
                return true;
            }
            return false;
        }

        @ReservedStackAccess
        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // Methods relayed from outer class

        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * 从流中重构实例（即反序列化它）。
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }

    /**
     * 非公平锁的同步对象
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;
        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * 公平锁的同步对象
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;
        /**
         * tryAcquire 的公平版本。 除非递归调用或没有 waiter 或是第一个，否则不要授予访问权限。
         */
        @ReservedStackAccess
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
    }

    /**
     * 创建 ReentrantLock 的实例。 这相当于使用 ReentrantLock(false)。
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * 使用给定的公平策略创建 ReentrantLock 的实例。
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * 获得锁。
     * 如果其他线程没有持有锁，则获取该锁并立即返回，将锁持有计数设置为 1。
     * 如果当前线程已经持有锁，那么持有计数加一并且该方法立即返回。
     * 如果该锁被另一个线程持有，那么当前线程将因线程调度目的而被禁用并处于休眠状态，直到获得该锁为止，此时锁持有计数设置为 1。
     */
    public void lock() {
        sync.acquire(1);
    }

    /**
     * 除非当前线程被中断，否则获取锁。
     * 如果其他线程没有持有锁，则获取该锁并立即返回，将锁持有计数设置为 1。
     * 如果当前线程已经持有这个锁，那么持有计数就会增加一并且该方法立即返回。
     * 如果锁被另一个线程持有，那么当前线程将被禁用以进行线程调度并处于休眠状态，直到发生以下两种情况之一：
     *  1. 锁被当前线程获取；或者
     *  2. 一些其他线程中断当前线程。
     * 如果当前线程获取了锁，则锁保持计数设置为 1。
     * 如果当前线程：
     *  1. 在进入此方法时设置其中断状态；或者
     *  2. 在获取锁时被中断，
     * 然后抛出 InterruptedException 并清除当前线程的中断状态。
     * 在此实现中，由于此方法是显式中断点，因此优先响应中断而不是正常或可重入获取锁。
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * 仅当调用时其他线程未持有该锁时才获取该锁。
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * 如果在给定的等待时间内没有被另一个线程持有并且当前线程没有被中断，则获取锁。
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
     * 当与内置监视器锁一起使用时，返回的 Condition 实例支持与对象监视器方法（wait、notify 和 notifyAll）相同的用法。
     *  1. 如果在调用任何 Condition 等待或信号方法时未持有此锁，则抛出 IllegalMonitorStateException。
     *  2. 当条件等待方法被调用时，锁被释放，在它们返回之前，锁被重新获取，锁保持计数恢复到调用方法时的状态。
     *  3. 如果线程在等待时被中断，则等待将终止，将抛出 InterruptedException，并清除线程的中断状态。
     *  4. 等待线程按 FIFO 顺序发出信号。
     *  5. 从等待方法返回的线程重新获取锁的顺序与最初获取锁的线程相同，在默认情况下未指定，但对于公平锁，那些等待时间最长的线程优先。
     */
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * 查询当前线程持有该锁的次数。
     * 一个线程为每个与解锁操作不匹配的锁操作持有一个锁。
     * 保持计数信息通常仅用于测试和调试目的。
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     * 查询当前线程是否持有此锁。
     * 类似于内置监视器锁的 Thread.holdsLock(Object) 方法，此方法通常用于调试和测试。
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 查询此锁是否被任何线程持有。 此方法设计用于监视系统状态，而不是用于同步控制。
     */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /**
     * 如果此锁的公平性设置为 true，则返回 true。
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 返回当前拥有此锁的线程，如果不拥有，则返回 null。 当此方法由不是所有者的线程调用时，返回值反映了当前锁定状态的尽力而为的近似值。
     * 例如，即使有线程试图获取锁但尚未这样做，所有者也可能暂时为空。 此方法旨在促进子类的构建，以提供更广泛的锁监控设施。
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 查询是否有线程正在等待获取此锁。 请注意，因为取消可能随时发生，返回 true 并不能保证任何其他线程将永远获得此锁。
     * 该方法主要用于监控系统状态。
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 查询给定线程是否正在等待获取此锁。 请注意，因为取消可能随时发生，返回 true 并不能保证该线程将永远获得此锁。
     * 该方法主要用于监控系统状态。
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * 返回等待获取此锁的线程数的估计值。 该值只是一个估计值，因为当此方法遍历内部数据结构时，线程数可能会动态变化。
     * 该方法设计用于监视系统状态，而不是用于同步控制。
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 返回一个包含可能正在等待获取此锁的线程的集合。 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
     * 返回集合的元素没有特定的顺序。 此方法旨在促进子类的构建，以提供更广泛的监视设施。
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 查询是否有任何线程正在等待与此锁关联的给定条件。 请注意，因为超时和中断可能随时发生，返回 true 并不能保证未来的信号会唤醒任何线程。
     * 该方法主要用于监控系统状态。
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
     * 返回等待与此锁关联的给定条件的线程数的估计值。 请注意，由于超时和中断可能随时发生，因此估计值仅用作实际服务员人数的上限。
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
     * 返回一个包含可能正在等待与此锁关联的给定条件的线程的集合。
     * 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。 返回集合的元素没有特定的顺序。
     * 此方法旨在促进子类的构建，以提供更广泛的状态监控设施。
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
     * 返回标识此锁及其锁状态的字符串。 括号中的状态包括字符串“Unlocked”或字符串“Locked by”，后跟拥有线程的名称。
     */
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
    }
}
