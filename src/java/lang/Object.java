package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;

/**
 * Object 是 Java 类库中的一个特殊类，也是所有类的父类。也就是说，Java 允许把任何类型的对象赋给 Object 类型的变量。
 * 当一个类被定义后，如果没有指定继承的父类，那么默认父类就是 Object 类
 */
public class Object {

    /**
     * 本地方法的实现是由其他语言编写并保存在动态连接库中，因而在java类中不需要方法实现。registerNatives本质上就是一个本地方法，
     * 但这又是一个有别于一般本地方法的本地方法，从方法名我们可以猜测该方法应该是用来注册本地方法的。
     * 事实上，上述代码的功能就是先定义了registerNatives()方法，然后当该类被加载的时候，调用该方法完成对该类中本地方法的注册。
     *
     * 一个Java程序要想调用一个本地方法，需要执行两个步骤：
     *
     * （1）通过System.loadLibrary()将包含本地方法实现的动态文件加载进内存；
     * （2）当Java程序需要调用本地方法时，虚拟机在加载的动态文件中定位并链接该本地方法，从而得以执行本地方法。
     *
     * registerNatives()方法的作用就是取代第二步，让程序主动将本地方法链接到调用方，当Java程序需要调用本地方法时就可以直接调用，
     * 而不需要虚拟机再去定位并链接。
     */
    private static native void registerNatives();
    static {
        registerNatives();
    }

    /**
     * 构造方法
     */
    @HotSpotIntrinsicCandidate
    public Object() {}

    /**
     * 返回一个对象运行时的实例类
     */
    @HotSpotIntrinsicCandidate
    public final native Class<?> getClass();

    /**
     * 返回对象的哈希码值。 支持此方法是为了有利于散列表，例如 {@link java.util.HashMap} 提供的散列表。
     *
     * 1.在一个Java应用运行期间,只要一个对象的{@code equals}方法所用到的信息没有被修改,
     *  那么对这同一个对象调用多次{@code hashCode}方法,都必须返回同一个整数.
     *  在同一个应用程序的多次执行中,每次执行所返回的整数可以不一样.
     * 2.如果两个对象根据{@code equals(Object)}方法进行比较结果是相等的,那么调用这两个对象的
     *  {@code hashCode}方法必须返回同样的结果.
     * 3.如果两个对象根据{@link java.lang.Object#equals(java.lang.Object)}进行比较是不相等的,
     *  那么并<em>不</em>要求调用这两个对象的{@code hashCode}方法必须返回不同的整数结果.
     *  但是程序员应该了解到,给不相等的对象产生不同的整数结果,可能提高散列表(hash tables)的性能.
     *
     * 简要的说,在一个应用的一次运行期间,equals为true的对象hashCode必须相同,而equals为false的对象hashCode未必不同.
     * 但是应当让不同的对象返回不同的hashCode.
     */
    @HotSpotIntrinsicCandidate
    public native int hashCode();

    /**
     * 比较两对象是否相等
     */
    public boolean equals(Object obj) {
        return (this == obj);
    }

    /**
     * 创建与该对象的类相同的新对象
     */
    @HotSpotIntrinsicCandidate
    protected native Object clone() throws CloneNotSupportedException;

    /**
     * 返回该对象的字符串表示
     */
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    /**
     * 激活等待在该对象的监视器上的一个线程
     */
    @HotSpotIntrinsicCandidate
    public final native void notify();

    /**
     * 激活等待在该对象的监视器上的全部线程
     */
    @HotSpotIntrinsicCandidate
    public final native void notifyAll();

    /**
     * 在其他线程调用此对象的 notify() 方法或 notifyAll() 方法前，导致当前线程等待
     */
    public final void wait() throws InterruptedException {
        wait(0L);
    }

    /**
     * 使当前线程等待直到它被唤醒，通常是通过 Notified 或 Interrupted ，或者直到经过一定的实时时间。
     * 在所有方面，此方法的行为就好像 {@code wait(timeoutMillis, 0)} 已被调用
     */
    public final native void wait(long timeoutMillis) throws InterruptedException;

    /**
     * Object wait(long timeout, int nanos) 方法让当前线程处于等待(阻塞)状态，直到其他线程调用此对象的 notify() 方法
     *  或 notifyAll() 方法，或者超过参数 timeout 与 nanos 设置的超时时间。
     *
     * 该方法与 wait(long timeout) 方法类似，多了一个 nanos 参数，这个参数表示额外时间（以纳秒为单位，范围是 0-999999）。
     * 所以超时的时间还需要加上 nanos 纳秒。
     *
     * 如果 timeout 与 nanos 参数都为 0，则不会超时，会一直进行等待，类似于 wait() 方法。
     *
     * 当前线程必须是此对象的监视器所有者，否则还是会发生 IllegalMonitorStateException 异常。
     * 如果当前线程在等待之前或在等待时被任何线程中断，则会抛出 InterruptedException 异常。
     * 如果传递的参数不合法或 nanos 不在 0-999999 范围内，则会抛出 IllegalArgumentException 异常。
     */
    public final void wait(long timeoutMillis, int nanos) throws InterruptedException {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeoutMillis value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }

        if (nanos > 0) {
            timeoutMillis++;
        }

        wait(timeoutMillis);
    }

    /**
     * 当垃圾回收器确定不存在对该对象的更多引用时，对象垃圾回收器调用该方法
     */
    @Deprecated(since="9")
    protected void finalize() throws Throwable { }
}
