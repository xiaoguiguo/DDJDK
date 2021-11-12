package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/**
 * 接口用作线程同步机制，类似于同步块。新的 Lock 机制更灵活，提供比同步块更多的选项。 锁和同步块之间的主要区别如下：
 *  1. 序列的保证 - 同步块不提供对等待线程进行访问的序列的任何保证，但Lock接口处理它。
 *  2. 无超时，如果未授予锁，则同步块没有超时选项。Lock接口提供了这样的选项。
 *  3. 单一方法同步块必须完全包含在单个方法中，而Lock接口的方法lock()和unlock()可以以不同的方式调用。
 */
public interface Lock {

    /**
     * 获得 锁
     */
    void lock();

    /**
     * 获取锁，除非当前线程中断
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * 只有在调用时才可以获得锁
     */
    boolean tryLock();

    /**
     * 如果在给定的等待时间内自由，并且当前线程未被中断，则获取该锁。
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 释放 锁
     */
    void unlock();

    /**
     * 返回绑定到此Lock实例的新Condition实例
     */
    Condition newCondition();
}
