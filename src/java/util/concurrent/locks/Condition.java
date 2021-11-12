package java.util.concurrent.locks;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 任意一个Java对象，都拥有一组监视器方法（定义在java.lang.Object上），
 * 主要包括wait()、wait(long timeout)、notify()以及notifyAll()方法，这些方法与synchronized同步关键字配合，可以实现等待/通知模式。
 * Condition接口也提供了类似Object的监视器方法，与Lock配合可以实现等待/通知模式，但是这两者在使用方式以及功能特性上还是有差别的。
 *
 * Condition定义了等待/通知两种类型的方法，当前线程调用这些方法时，需要提前获取到Condition对象关联的锁。
 * Condition对象是由Lock对象（调用Lock对象的newCondition()方法）创建出来的，换句话说，Condition是依赖Lock对象的。
 */
public interface Condition {

    /**
     * 当线程进入等待状态直到被通知(signal)或中断，当前线程将进入运行状态且从await()方法返回的情况，
     * 包括：其他线程调用该Condition的signal()或signalAll()方法，而当前线程被选中唤醒；
     *      1. 其他线程(调用interrupt()方法)中断当前线程；
     *      2. 如果当前等待线程从await()方法返回，那么表明该线程已经获取了Condition对象所对应的锁
     */
    void await() throws InterruptedException;

    /**
     * 当前线程进入等待状态直到被通知，从方法名称上可以看出该方法对中断不敏感
     */
    void awaitUninterruptibly();

    /**
     * 当前线程进入等待状态直到被通知、中断或者超时。返回值表示剩余的时间，如果在nanosTimeout纳秒之前被唤醒，
     * 那么返回值就是(nanosTimeout - 实际耗时)。如果返回值是0或者负数，那么可以认定已经超时了
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * 使当前线程等待直到发出信号或中断，或指定的等待时间过去。
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 当前线程进入等待状态直到被通知、中断或者到某个时间。如果没有到指定时间就被通知，方法返回true，否则，表示到了指定时间，返回false
     */
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * 唤醒一个等待在Condition上的线程，该线程从等待方法返回前必须获得与Condition相关联的锁
     */
    void signal();

    /**
     * 唤醒所有等待在Condition上的线程，能够从等待方法返回的线程必须获得与Condition相关联的锁
     */
    void signalAll();
}
