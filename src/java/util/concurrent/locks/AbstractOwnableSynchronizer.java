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

/**
 * 可能由线程独占拥有的同步器。 此类为创建可能需要所有权概念的锁和相关同步器提供了基础。
 * AbstractOwnableSynchronizer 类本身不管理或使用此信息。
 * 但是，子类和工具可以使用适当维护的值来帮助控制和监视访问并提供诊断。
 */
public abstract class AbstractOwnableSynchronizer
    implements java.io.Serializable {

    /** 即使所有字段都是瞬态的，也要使用序列号。 */
    private static final long serialVersionUID = 3737899427754241961L;

    /**
     * 空构造函数
     */
    protected AbstractOwnableSynchronizer() { }

    /**
     * 独占模式同步的当前所有者。
     */
    private transient Thread exclusiveOwnerThread;

    /**
     * 设置当前拥有独占访问权限的线程。 null 参数表示没有线程拥有访问权限。 此方法不会以其他方式强加任何同步或易失性字段访问。
     */
    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }

    /**
     * 返回最后由 setExclusiveOwnerThread 设置的线程，如果从未设置，则返回 null。
     * 此方法不会以其他方式强加任何同步或 volatile字段访问。
     */
    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}
