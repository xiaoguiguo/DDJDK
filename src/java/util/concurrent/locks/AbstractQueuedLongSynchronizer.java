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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer.Node;

/**
 * AbstractQueuedSynchronizer 的一个版本，其中同步状态保持为 long。
 * 此类具有与 AbstractQueuedSynchronizer 完全相同的结构、属性和方法，不同之处在于所有与状态相关的参数和结果都定义为 long 而不是 int。
 * 在创建需要 64 位状态的多级锁和屏障等同步器时，此类可能很有用。
 */
public abstract class AbstractQueuedLongSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414692L;

    /**
     * 为了保持源同步，此源文件的其余部分完全从 AbstractQueuedSynchronizer 克隆，替换类名并将与同步状态相关的整数更改为长整数。 请保持这种状态。
     */

    /**
     * 创建一个新的 AbstractQueuedLongSynchronizer 实例，初始同步状态为零。
     */
    protected AbstractQueuedLongSynchronizer() { }

    /**
     * 等待队列的头部，延迟初始化。 除初始化外，仅通过 setHead 方法进行修改。
     * 注意：如果 head 存在，则保证其 waitStatus 不会被 CANCELLED。
     */
    private transient volatile Node head;

    /**
     * 等待队列的尾部，延迟初始化。 仅通过方法 enq 修改以添加新的等待节点。
     */
    private transient volatile Node tail;

    /**
     * 同步状态。
     */
    private volatile long state;

    /**
     * 返回同步状态的当前值。 此操作具有易失性(volatile)读取的内存语义。
     */
    protected final long getState() {
        return state;
    }

    /**
     * 设置同步状态的值。 此操作具有易失性写入的内存语义。
     */
    protected final void setState(long newState) {
        // See JDK-8180620: Clarify VarHandle mixed-access subtleties
        STATE.setVolatile(this, newState);
    }

    /**
     * 如果当前状态值等于预期值，则原子地将同步状态设置为给定的更新值。 此操作具有易失性读写的内存语义。
     */
    protected final boolean compareAndSetState(long expect, long update) {
        return STATE.compareAndSet(this, expect, update);
    }

    // Queuing utilities

    /**
     * 旋转速度比使用定时停车更快的纳秒数。 粗略估计足以在非常短的超时时间内提高响应能力。
     */
    static final long SPIN_FOR_TIMEOUT_THRESHOLD = 1000L;

    /**
     * 将节点插入队列，必要时进行初始化。
     */
    private Node enq(Node node) {
        for (;;) {
            Node oldTail = tail;
            if (oldTail != null) {
                node.setPrevRelaxed(oldTail);
                if (compareAndSetTail(oldTail, node)) {
                    oldTail.next = node;
                    return oldTail;
                }
            } else {
                initializeSyncQueue();
            }
        }
    }

    /**
     * 为当前线程和给定模式创建和排队节点。
     */
    private Node addWaiter(Node mode) {
        Node node = new Node(mode);

        for (;;) {
            Node oldTail = tail;
            if (oldTail != null) {
                node.setPrevRelaxed(oldTail);
                if (compareAndSetTail(oldTail, node)) {
                    oldTail.next = node;
                    return node;
                }
            } else {
                initializeSyncQueue();
            }
        }
    }

    /**
     * 将队列头设置为节点，从而出队。 仅由获取方法调用。 为了 GC 和抑制不必要的信号和遍历，还清空了未使用的字段。
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * 唤醒节点的后继节点（如果存在）。
     */
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         */
        int ws = node.waitStatus;
        if (ws < 0) {
            node.compareAndSetWaitStatus(ws, 0);
        }

        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         */
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node p = tail; p != node && p != null; p = p.prev) {
                if (p.waitStatus <= 0) {
                    s = p;
                }
            }
        }
        if (s != null) {
            LockSupport.unpark(s.thread);
        }
    }

    /**
     * 共享模式的释放动作——表示后继者并确保传播。 （注意：对于独占模式，如果需要信号，释放就相当于调用头部的 unparkSuccessor。）
     */
    private void doReleaseShared() {
        /*
         * Ensure that a release propagates, even if there are other
         * in-progress acquires/releases.  This proceeds in the usual
         * way of trying to unparkSuccessor of head if it needs
         * signal. But if it does not, status is set to PROPAGATE to
         * ensure that upon release, propagation continues.
         * Additionally, we must loop in case a new node is added
         * while we are doing this. Also, unlike other uses of
         * unparkSuccessor, we need to know if CAS to reset status
         * fails, if so rechecking.
         */
        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
                    if (!h.compareAndSetWaitStatus(Node.SIGNAL, 0)) {
                        continue;            // loop to recheck cases
                    }
                    unparkSuccessor(h);
                }
                else if (ws == 0 && !h.compareAndSetWaitStatus(0, Node.PROPAGATE)) {
                    continue;                // loop on failed CAS
                }
            }
            if (h == head) {                   // loop if head changed
                break;
            }
        }
    }

    /**
     * 设置队列头，并检查后继者是否可能在共享模式下等待，如果传播 > 0 或设置了 PROPAGATE 状态，则传播。
     */
    private void setHeadAndPropagate(Node node, long propagate) {
        Node h = head; // Record old head for check below
        setHead(node);
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because
         *      PROPAGATE status may transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode,
         *     or we don't know, because it appears null
         *
         * The conservatism in both of these checks may cause
         * unnecessary wake-ups, but only when there are multiple
         * racing acquires/releases, so most need signals now or soon
         * anyway.
         */
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
            (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            if (s == null || s.isShared()) {
                doReleaseShared();
            }
        }
    }

    // Utilities for various versions of acquire

    /**
     * 取消正在进行的获取尝试。
     */
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        if (node == null) {
            return;
        }

        node.thread = null;

        // Skip cancelled predecessors
        Node pred = node.prev;
        while (pred.waitStatus > 0) {
            node.prev = pred = pred.prev;
        }

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary, although with
        // a possibility that a cancelled node may transiently remain
        // reachable.
        Node predNext = pred.next;

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        node.waitStatus = Node.CANCELLED;

        // If we are the tail, remove ourselves.
        if (node == tail && compareAndSetTail(node, pred)) {
            pred.compareAndSetNext(predNext, null);
        } else {
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
            int ws;
            if (pred != head &&
                ((ws = pred.waitStatus) == Node.SIGNAL ||
                 (ws <= 0 && pred.compareAndSetWaitStatus(ws, Node.SIGNAL))) &&
                pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    pred.compareAndSetNext(predNext, next);
            } else {
                unparkSuccessor(node);
            }

            node.next = node; // help GC
        }
    }

    /**
     * 检查和更新未能获取的节点的状态。 如果线程应该阻塞，则返回 true。 这是所有获取循环中的主要信号控制。 要求 pred == node.prev。
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL) {
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             */
            return true;
        }
        if (ws > 0) {
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             */
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             */
            pred.compareAndSetWaitStatus(ws, Node.SIGNAL);
        }
        return false;
    }

    /**
     * 中断当前线程的便捷方法。
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * 停车然后检查是否中断的便捷方法。
     */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    /**
     * 各种风格的获取，在独占/共享和控制模式中各不相同。 每个都大致相同，但又令人讨厌的不同。
     * 由于异常机制（包括确保我们在 tryAcquire 抛出异常时取消）和其他控制的相互作用，只能进行一点分解，至少不会在不过度损害性能的情况下。
     */

    /**
     * 以独占不间断模式获取已在队列中的线程。 由条件等待方法以及获取使用。
     */
    final boolean acquireQueued(final Node node, long arg) {
        boolean interrupted = false;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    return interrupted;
                }
                if (shouldParkAfterFailedAcquire(p, node)) {
                    interrupted |= parkAndCheckInterrupt();
                }
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            if (interrupted) {
                selfInterrupt();
            }
            throw t;
        }
    }

    /**
     * 在独占可中断模式下获取。
     */
    private void doAcquireInterruptibly(long arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.EXCLUSIVE);
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            throw t;
        }
    }

    /**
     * 以独占定时模式获取。
     */
    private boolean doAcquireNanos(long arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) {
                    cancelAcquire(node);
                    return false;
                }
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            throw t;
        }
    }

    /**
     * 在共享不间断模式下获取。
     */
    private void doAcquireShared(long arg) {
        final Node node = addWaiter(Node.SHARED);
        boolean interrupted = false;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    long r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node)) {
                    interrupted |= parkAndCheckInterrupt();
                }
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            throw t;
        } finally {
            if (interrupted) {
                selfInterrupt();
            }
        }
    }

    /**
     * 在共享可中断模式下获取。
     */
    private void doAcquireSharedInterruptibly(long arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    long r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            throw t;
        }
    }

    /**
     * 在共享定时模式下获取。
     */
    private boolean doAcquireSharedNanos(long arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L) {
            return false;
        }
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.SHARED);
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    long r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) {
                    cancelAcquire(node);
                    return false;
                }
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            throw t;
        }
    }

    // Main exported methods

    /**
     * 尝试以独占模式获取。 该方法应该查询对象的状态是否允许以独占模式获取它，如果允许则获取它。
     * 此方法始终由执行获取的线程调用。 如果此方法报告失败，acquire 方法可能会将线程排队（如果它尚未排队），直到收到来自某个其他线程的释放信号。
     * 这可用于实现方法 Lock.tryLock()。
     */
    protected boolean tryAcquire(long arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 尝试设置状态以反映独占模式下的发布。
     * 此方法始终由执行释放的线程调用。
     */
    protected boolean tryRelease(long arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 尝试以共享模式获取。 该方法应该查询对象的状态是否允许在共享模式下获取它，如果允许则获取它。
     * 此方法始终由执行获取的线程调用。 如果此方法报告失败，acquire 方法可能会将线程排队（如果它尚未排队），直到收到来自某个其他线程的释放信号。
     */
    protected long tryAcquireShared(long arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 尝试设置状态以反映共享模式下的发布。
     * 此方法始终由执行释放的线程调用。
     */
    protected boolean tryReleaseShared(long arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 如果与当前（调用）线程独占同步，则返回 true。 每次调用 AbstractQueuedLongSynchronizer.ConditionObject 方法时都会调用此方法。
     * 默认实现抛出 UnsupportedOperationException。 此方法仅在 AbstractQueuedLongSynchronizer.ConditionObject 方法内部调用，
     * 因此如果不使用条件则无需定义。
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * 以独占模式获取，忽略中断。 通过至少调用一次 tryAcquire 实现，成功返回。
     * 否则线程会排队，可能会反复阻塞和解除阻塞，调用 tryAcquire 直到成功。
     * 此方法可用于实现方法 Lock.lock。
     */
    public final void acquire(long arg) {
        if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) {
            selfInterrupt();
        }
    }

    /**
     * 以独占模式获取，如果中断则中止。 通过首先检查中断状态来实现，然后至少调用一次 tryAcquire，成功时返回。
     * 否则线程排队，可能重复阻塞和解除阻塞，调用 tryAcquire 直到成功或线程被中断。
     * 此方法可用于实现 Lock.lockInterruptably 方法。
     */
    public final void acquireInterruptibly(long arg)
            throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!tryAcquire(arg)) {
            doAcquireInterruptibly(arg);
        }
    }

    /**
     * 尝试以独占模式获取，如果中断则中止，如果给定的超时时间过去则失败。
     * 通过首先检查中断状态来实现，然后至少调用一次 tryAcquire，成功时返回。
     * 否则，线程将排队，可能会重复阻塞和解除阻塞，调用 tryAcquire 直到成功或线程被中断或超时过去。
     * 此方法可用于实现方法 Lock.tryLock(long, TimeUnit)。
     */
    public final boolean tryAcquireNanos(long arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return tryAcquire(arg) || doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * 以独占模式发布。 如果 tryRelease 返回 true，则通过解除阻塞一个或多个线程来实现。 此方法可用于实现方法 Lock.unlock。
     */
    public final boolean release(long arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0) {
                unparkSuccessor(h);
            }
            return true;
        }
        return false;
    }

    /**
     * 在共享模式下获取，如果中断则中止。 通过首先检查中断状态来实现，然后至少调用一次 tryAcquireShared，成功返回。
     * 否则线程会排队，可能会重复阻塞和解除阻塞，调用 tryAcquireShared 直到成功或线程被中断。
     */
    public final void acquireShared(long arg) {
        if (tryAcquireShared(arg) < 0) {
            doAcquireShared(arg);
        }
    }

    /**
     * 在共享模式下获取，如果中断则中止。 通过首先检查中断状态来实现，然后至少调用一次 tryAcquireShared，成功返回。
     * 否则线程会排队，可能会重复阻塞和解除阻塞，调用 tryAcquireShared 直到成功或线程被中断。
     */
    public final void acquireSharedInterruptibly(long arg)
            throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) < 0) {
            doAcquireSharedInterruptibly(arg);
        }
    }

    /**
     * 尝试在共享模式下获取，如果被中断则中止，如果给定的超时时间过去则失败。
     * 通过首先检查中断状态来实现，然后至少调用一次 tryAcquireShared，成功返回。
     * 否则，线程将排队，可能会重复阻塞和解除阻塞，调用 tryAcquireShared 直到成功或线程被中断或超时。
     */
    public final boolean tryAcquireSharedNanos(long arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return tryAcquireShared(arg) >= 0 ||
            doAcquireSharedNanos(arg, nanosTimeout);
    }

    /**
     * 以共享模式发布。 如果 tryReleaseShared 返回 true，则通过解除阻塞一个或多个线程来实现。
     */
    public final boolean releaseShared(long arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    // Queue inspection methods

    /**
     * 查询是否有线程在等待获取。 请注意，由于中断和超时导致的取消随时可能发生，真正的返回并不能保证任何其他线程将永远获得。
     */
    public final boolean hasQueuedThreads() {
        for (Node p = tail, h = head; p != h && p != null; p = p.prev) {
            if (p.waitStatus <= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查询是否有线程争用过这个同步器； 也就是说，如果一个获取方法曾经被阻塞。
     * 在此实现中，此操作以恒定时间返回。
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * 返回队列中的第一个（等待时间最长的）线程，如果当前没有线程排队，则返回 null。
     * 在此实现中，此操作通常以恒定时间返回，但如果其他线程同时修改队列，则可能会在争用时进行迭代。
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * 快速路径失败时调用的 getFirstQueuedThread 版本。
     */
    private Thread fullGetFirstQueuedThread() {
        /**
         * 第一个节点通常是 head.next。 尝试获取其线程字段，确保读取一致：如果线程字段被清空或 s.prev 不再是头，那么在我们的一些读取之间，
         * 一些其他线程并发执行 setHead。 在诉诸遍历之前，我们尝试了两次。
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null && s.prev == head && (st = s.thread) != null) ||
            ((h = head) != null && (s = h.next) != null && s.prev == head && (st = s.thread) != null)) {
            return st;
        }

        /**
         * Head 的 next 字段可能尚未设置，或者可能在 setHead 之后未设置。 所以我们必须检查tail是否实际上是第一个节点。
         * 如果没有，我们继续，安全地从尾部回到头部找到第一个，保证终止。
         */

        Thread firstThread = null;
        for (Node p = tail; p != null && p != head; p = p.prev) {
            Thread t = p.thread;
            if (t != null) {
                firstThread = t;
            }
        }
        return firstThread;
    }

    /**
     * 如果给定线程当前正在排队，则返回 true。
     * 此实现遍历队列以确定给定线程的存在。
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null) {
            throw new NullPointerException();
        }
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread == thread) {
                return true;
            }
        }
        return false;
    }

    /**
     * 如果明显的第一个排队线程（如果存在）正在以独占模式等待，则返回 true。
     * 如果此方法返回 true，并且当前线程正在尝试以共享模式获取（即从 tryAcquireShared 调用此方法），则可以保证当前线程不是第一个排队的线程。
     * 仅用作 ReentrantReadWriteLock 中的启发式方法。
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&
            (s = h.next)  != null &&
            !s.isShared()         &&
            s.thread != null;
    }

    /**
     * 查询是否有任何线程等待获取的时间比当前线程长。
     */
    public final boolean hasQueuedPredecessors() {
        Node h, s;
        if ((h = head) != null) {
            if ((s = h.next) == null || s.waitStatus > 0) {
                s = null; // traverse in case of concurrent cancellation
                for (Node p = tail; p != h && p != null; p = p.prev) {
                    if (p.waitStatus <= 0) {
                        s = p;
                    }
                }
            }
            if (s != null && s.thread != Thread.currentThread()) {
                return true;
            }
        }
        return false;
    }

    // Instrumentation and monitoring methods

    /**
     * 返回等待获取的线程数的估计值。 该值只是一个估计值，因为当此方法遍历内部数据结构时，线程数可能会动态变化。
     * 该方法设计用于监视系统状态，而不是用于同步控制。
     */
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null) {
                ++n;
            }
        }
        return n;
    }

    /**
     * 返回一个包含可能正在等待获取的线程的集合。 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
     * 返回集合的元素没有特定的顺序。 此方法旨在促进子类的构建，以提供更广泛的监视设施。
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null) {
                list.add(t);
            }
        }
        return list;
    }

    /**
     * 返回一个包含可能在独占模式下等待获取的线程的集合。 它与 getQueuedThreads 具有相同的属性，除了它只返回由于独占获取而等待的线程。
     */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    /**
     * 返回一个包含可能在共享模式下等待获取的线程的集合。 它与 getQueuedThreads 具有相同的属性，除了它只返回由于共享获取而等待的那些线程。
     */
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    /**
     * 返回标识此同步器及其状态的字符串。 括号中的状态包括字符串“State =”，后跟 getState 的当前值，以及“非空”或“空”，具体取决于队列是否为空。
     */
    public String toString() {
        return super.toString()
            + "[State = " + getState() + ", "
            + (hasQueuedThreads() ? "non" : "") + "empty queue]";
    }


    // Internal support methods for Conditions

    /**
     * 如果一个节点（始终是最初放置在条件队列中的节点）现在正在等待重新获取同步队列，则返回 true。
     */
    final boolean isOnSyncQueue(Node node) {
        if (node.waitStatus == Node.CONDITION || node.prev == null) {
            return false;
        }
        if (node.next != null) { // If has successor, it must be on queue
            return true;
        }
        /**
         * node.prev 可以为非空，但尚未在队列中，因为将其放入队列的 CAS 可能会失败。 所以我们必须从尾部遍历以确保它确实做到了。
         * 在调用这个方法时它总是靠近尾部，除非 CAS 失败（这不太可能），它会在那里，所以我们几乎不会遍历太多。
         */
        return findNodeFromTail(node);
    }

    /**
     * 如果节点通过从尾部向后搜索在同步队列上，则返回 true。 仅在 isOnSyncQueue 需要时调用。
     */
    private boolean findNodeFromTail(Node node) {
        // We check for node first, since it's likely to be at or near tail.
        // tail is known to be non-null, so we could re-order to "save"
        // one null check, but we leave it this way to help the VM.
        for (Node p = tail;;) {
            if (p == node) {
                return true;
            }
            if (p == null) {
                return false;
            }
            p = p.prev;
        }
    }

    /**
     * 将节点从条件队列转移到同步队列。 如果成功则返回真。
     */
    final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         */
        if (!node.compareAndSetWaitStatus(Node.CONDITION, 0)) {
            return false;
        }

        /**
         * 拼接到队列并尝试设置前驱的 waitStatus 以指示线程（可能）正在等待。
         * 如果取消或尝试设置 waitStatus 失败，则唤醒以重新同步（在这种情况下，waitStatus 可能是暂时且无害的错误）。
         */
        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !p.compareAndSetWaitStatus(ws, Node.SIGNAL)) {
            LockSupport.unpark(node.thread);
        }
        return true;
    }

    /**
     * 如有必要，在取消等待后将节点传输到同步队列。 如果线程在发出信号之前被取消，则返回 true。
     */
    final boolean transferAfterCancelledWait(Node node) {
        if (node.compareAndSetWaitStatus(Node.CONDITION, 0)) {
            enq(node);
            return true;
        }
        /**
         * 如果我们输给了一个 signal()，那么在它完成 enq() 之前我们不能继续。 在不完整的转移期间取消既罕见又短暂，因此只需旋转即可。
         */
        while (!isOnSyncQueue(node)) {
            Thread.yield();
        }
        return false;
    }

    /**
     * 使用当前状态值调用 release； 返回保存状态。 取消节点并在失败时抛出异常。
     */
    final long fullyRelease(Node node) {
        try {
            long savedState = getState();
            if (release(savedState)) {
                return savedState;
            }
            throw new IllegalMonitorStateException();
        } catch (Throwable t) {
            node.waitStatus = Node.CANCELLED;
            throw t;
        }
    }

    // Instrumentation methods for conditions

    /**
     * 查询给定的 ConditionObject 是否使用此同步器作为其锁。
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * 查询是否有任何线程正在等待与此同步器关联的给定条件。 请注意，因为超时和中断可能随时发生，真正的返回并不能保证未来的信号会唤醒任何线程。
     * 该方法主要用于监控系统状态。
     */
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition)) {
            throw new IllegalArgumentException("Not owner");
        }
        return condition.hasWaiters();
    }

    /**
     * 返回等待与此同步器关联的给定条件的线程数的估计值。 请注意，由于超时和中断可能随时发生，因此估计值仅用作实际服务员人数的上限。
     * 该方法设计用于监视系统状态，而不是用于同步控制。
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition)) {
            throw new IllegalArgumentException("Not owner");
        }
        return condition.getWaitQueueLength();
    }

    /**
     * 返回一个包含可能正在等待与此同步器关联的给定条件的线程的集合。
     * 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。 返回集合的元素没有特定的顺序。
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition)) {
            throw new IllegalArgumentException("Not owner");
        }
        return condition.getWaitingThreads();
    }

    /**
     * 作为 Lock 实现基础的 AbstractQueuedLongSynchronizer 的条件实现。
     * 此类的方法文档从锁定和条件用户的角度描述了机制，而不是行为规范。
     * 此类的导出版本通常需要随附描述依赖于关联 AbstractQueuedLongSynchronizer 的条件语义的文档。
     * 此类是可序列化的，但所有字段都是瞬态的，因此反序列化的条件没有等待者。
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /** First node of condition queue. */
        private transient Node firstWaiter;
        /** Last node of condition queue. */
        private transient Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         */
        public ConditionObject() { }

        // Internal methods

        /**
         * 添加一个新的服务员到等待队列。
         */
        private Node addConditionWaiter() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node t = lastWaiter;
            // If lastWaiter is cancelled, clean out.
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }

            Node node = new Node(Node.CONDITION);

            if (t == null) {
                firstWaiter = node;
            } else {
                t.nextWaiter = node;
            }
            lastWaiter = node;
            return node;
        }

        /**
         * 删除并传输节点，直到命中未取消的 1 或为空。 从信号中分离出来部分是为了鼓励编译器内联没有服务员的情况。
         */
        private void doSignal(Node first) {
            do {
                if ((firstWaiter = first.nextWaiter) == null) {
                    lastWaiter = null;
                }
                first.nextWaiter = null;
            } while (!transferForSignal(first) &&
                     (first = firstWaiter) != null);
        }

        /**
         * 删除并转移所有节点。
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }

        /**
         * 从条件队列中取消链接已取消的等待节点。 仅在持有锁时调用。
         * 当在条件等待期间发生取消时，以及在看到 lastWaiter 已被取消时插入新的服务员时，将调用此方法。
         * 需要这种方法来避免在没有信号的情况下垃圾保留。 因此，即使它可能需要完全遍历，它也仅在没有信号的情况下发生超时或取消时才起作用。
         * 它遍历所有节点而不是在特定目标处停止以取消所有指向垃圾节点的指针的链接，而无需在取消风暴期间进行多次重新遍历。
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;
                    if (trail == null) {
                        firstWaiter = next;
                    } else {
                        trail.nextWaiter = next;
                    }
                    if (next == null) {
                        lastWaiter = trail;
                    }
                } else {
                    trail = t;
                }
                t = next;
            }
        }

        // public methods

        /**
         * 将等待时间最长的线程（如果存在）从此条件的等待队列移动到拥有锁的等待队列。
         */
        public final void signal() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            Node first = firstWaiter;
            if (first != null) {
                doSignal(first);
            }
        }

        /**
         * 将所有线程从此条件的等待队列移动到拥有锁的等待队列。
         */
        public final void signalAll() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            Node first = firstWaiter;
            if (first != null) {
                doSignalAll(first);
            }
        }

        /**
         * 实现不间断条件等待。
         *   1. 保存由 getState 返回的锁状态。
         *   2. 以保存的状态作为参数调用 release，如果失败则抛出 IllegalMonitorStateException。
         *   3. 阻塞直到发出信号。
         *   4. 通过以保存的状态作为参数调用特定版本的获取来重新获取。
         */
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            long savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted()) {
                    interrupted = true;
                }
            }
            if (acquireQueued(node, savedState) || interrupted) {
                selfInterrupt();
            }
        }

        /**
         * 对于可中断的等待，我们需要跟踪是否抛出 InterruptedException，
         * 如果在条件阻塞时中断，与重新中断当前线程，如果在阻塞等待重新获取时中断。
         */

        /** 模式意味着在退出等待时重新中断 */
        private static final int REINTERRUPT =  1;
        /** 模式意味着在退出等待时抛出 InterruptedException */
        private static final int THROW_IE    = -1;

        /**
         * 检查中断，如果在发出信号之前被中断，则返回 THROW_IE，如果发出信号则返回 REINTERRUPT，如果未中断则返回 0。
         */
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                0;
        }

        /**
         * 根据模式，抛出 InterruptedException、重新中断当前线程或不执行任何操作。
         */
        private void reportInterruptAfterWait(int interruptMode)
            throws InterruptedException {
            if (interruptMode == THROW_IE) {
                throw new InterruptedException();
            } else if (interruptMode == REINTERRUPT) {
                selfInterrupt();
            }
        }

        /**
         * 实现可中断条件等待。
         *   1. 如果当前线程被中断，则抛出 InterruptedException。
         *   2. 保存由 getState 返回的锁状态。
         *   3. 以保存的状态作为参数调用 release，如果失败则抛出 IllegalMonitorStateException。
         *   4. 阻塞直到发出信号或被中断。
         *   5. 通过以保存的状态作为参数调用特定版本的获取来重新获取。
         *   6. 如果在步骤 4 中被阻塞时被中断，则抛出 InterruptedException。
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Node node = addConditionWaiter();
            long savedState = fullyRelease(node);
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) {
                    break;
                }
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE) {
                interruptMode = REINTERRUPT;
            }
            if (node.nextWaiter != null) { // clean up if cancelled
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
        }

        /**
         * 实现定时条件等待。
         *   1. 如果当前线程被中断，则抛出 InterruptedException。
         *   2. 保存由 getState 返回的锁状态。
         *   3. 以保存的状态作为参数调用 release，如果失败则抛出 IllegalMonitorStateException。
         *   4. 阻塞直到发出信号、中断或超时。
         *   5. 通过以保存的状态作为参数调用特定版本的获取来重新获取。
         *   6. 如果在步骤 4 中被阻塞时被中断，则抛出 InterruptedException。
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            // We don't check for nanosTimeout <= 0L here, to allow
            // awaitNanos(0) as a way to "yield the lock".
            final long deadline = System.nanoTime() + nanosTimeout;
            long initialNanos = nanosTimeout;
            Node node = addConditionWaiter();
            long savedState = fullyRelease(node);
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) {
                    break;
                }
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE) {
                interruptMode = REINTERRUPT;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
            long remaining = deadline - System.nanoTime(); // avoid overflow
            return (remaining <= initialNanos) ? remaining : Long.MIN_VALUE;
        }

        /**
         * 实现绝对定时条件等待。
         *   1. 如果当前线程被中断，则抛出 InterruptedException。
         *   2. 保存由 getState 返回的锁状态。
         *   3. 以保存的状态作为参数调用 release，如果失败则抛出 IllegalMonitorStateException。
         *   4. 阻塞直到发出信号、中断或超时。
         *   5. 通过以保存的状态作为参数调用特定版本的获取来重新获取。
         *   6. 如果在步骤 4 中被阻塞时被中断，则抛出 InterruptedException。
         *   7. 如果在步骤 4 中阻塞时超时，则返回 false，否则返回 true。
         */
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Node node = addConditionWaiter();
            long savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() >= abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) {
                    break;
                }
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE) {
                interruptMode = REINTERRUPT;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
            return !timedout;
        }

        /**
         * 实现定时条件等待。
         *   1. 如果当前线程被中断，则抛出 InterruptedException。
         *   2. 保存由 getState 返回的锁状态。
         *   3. 以保存的状态作为参数调用 release，如果失败则抛出 IllegalMonitorStateException。
         *   4. 阻塞直到发出信号、中断或超时。
         *   5. 通过以保存的状态作为参数调用特定版本的获取来重新获取。
         *   6. 如果在步骤 4 中被阻塞时被中断，则抛出 InterruptedException。
         *   7. 如果在步骤 4 中阻塞时超时，则返回 false，否则返回 true。
         */
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            // We don't check for nanosTimeout <= 0L here, to allow
            // await(0, unit) as a way to "yield the lock".
            final long deadline = System.nanoTime() + nanosTimeout;
            Node node = addConditionWaiter();
            long savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) {
                    break;
                }
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE) {
                interruptMode = REINTERRUPT;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
            return !timedout;
        }

        //  support for instrumentation

        /**
         * 如果此条件是由给定的同步对象创建的，则返回 true。
         */
        final boolean isOwnedBy(AbstractQueuedLongSynchronizer sync) {
            return sync == AbstractQueuedLongSynchronizer.this;
        }

        /**
         * 查询是否有线程在此条件下等待。 实现 hasWaiters(AbstractQueuedLongSynchronizer.ConditionObject)。
         */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 返回等待此条件的线程数的估计值。 实现 getWaitQueueLength(AbstractQueuedLongSynchronizer.ConditionObject)。
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    ++n;
                }
            }
            return n;
        }

        /**
         * 返回一个包含可能正在等待此 Condition 的线程的集合。
         * 实现 getWaitingThreads(AbstractQueuedLongSynchronizer.ConditionObject)。
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    // VarHandle mechanics
    private static final VarHandle STATE;
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            STATE = l.findVarHandle(AbstractQueuedLongSynchronizer.class, "state", long.class);
            HEAD = l.findVarHandle(AbstractQueuedLongSynchronizer.class, "head", Node.class);
            TAIL = l.findVarHandle(AbstractQueuedLongSynchronizer.class, "tail", Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }

        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;
    }

    /**
     * 在第一次争用时初始化头字段和尾字段。
     */
    private final void initializeSyncQueue() {
        Node h;
        if (HEAD.compareAndSet(this, null, (h = new Node())))
            tail = h;
    }

    /**
     * CASes 尾字段。
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return TAIL.compareAndSet(this, expect, update);
    }
}
