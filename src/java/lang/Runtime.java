package java.lang;

/**
 * @className: Runtime
 * @author: doudou
 * @datetime: 2021/11/4
 * @description: 每个 Java 应用程序都有一个 {@code Runtime} 类的实例，它允许应用程序与应用程序运行的环境进行交互。
 *      当前 runtime 可以从 {@code getRuntime} 方法获得。
 *      应用程序无法创建自己的此类实例。
 */
public class Runtime {
    private static final Runtime currentRuntime = new Runtime();

    /**
     * 返回与当前 Java 应用程序关联的 runtime 对象。 {@code Runtime} 类的大多数方法都是实例方法，必须针对当前运行时对象调用。
     */
    public static Runtime getRuntime() {
        return currentRuntime;
    }

    public void exit(int status) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            /** 先使用SecurityManager检查是否有关闭JVM的权限 */
            security.checkExit(status);
        }
        Shutdown.exit(status);
    }
}
