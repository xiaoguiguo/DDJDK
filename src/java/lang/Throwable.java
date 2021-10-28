package java.lang;

import java.io.Serializable;

/**
 * Throwable类是Java语言中所有错误和异常的超类。 只有作为此类（或其子类之一）的实例的对象throw Java虚拟机抛出，
 * 或者可以由Java throw语句抛出。 类似地，只有这个类或其子类之一可以是catch子句中的参数类型。 出于编译时检查异常的目的， Throwable和任何子类Throwable （也不是RuntimeException或Error的子类）都被视为已检查的例外。
 */
public class Throwable implements Serializable {
    private static final long serialVersionUID = -5483213135545696013L;

    /** The JVM saves some indication of the stack backtrace in this slot. */
    private transient Object backtrace;

    /** Specific details about the Throwable. */
    private String detailMessage;

    private static class SentineHolder {

    }

}
