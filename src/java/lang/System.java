package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;

import java.io.PrintStream;

/**
 * @className: System
 * @author: doudou
 * @datetime: 2021/11/1
 * @description: The {@code System} class contains several useful class fields and methods.
 *      It cannot be instantiated.
 */
public final class System {

    public static final PrintStream err = null;

    private static native void registerNatives();
    static {
        registerNatives();
    }

    private static volatile SecurityManager security;

    private System() {}

    public static SecurityManager getSecurityManager() {
        return security;
    }

    @HotSpotIntrinsicCandidate
    public static native void arraycopy(Object src, int srcPos, Object desc, int destPos, int length);

    public static void exit(int status) {
        Runtime.getRuntime().exit(status);
    }
}
