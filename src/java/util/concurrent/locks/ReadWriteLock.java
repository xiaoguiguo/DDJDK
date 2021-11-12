package java.util.concurrent.locks;

/**
 * 接口允许一次读取多个线程，但一次只能写入一个线程。
 *
 * 读锁 - 如果没有线程锁定ReadWriteLock进行写入，则多线程可以访问读锁。
 * 写锁 - 如果没有线程正在读或写，那么一个线程可以访问写锁。
 */
public interface ReadWriteLock {
    /**
     * 返回读锁
     */
    Lock readLock();

    /**
     * 返回写锁
     */
    Lock writeLock();
}
