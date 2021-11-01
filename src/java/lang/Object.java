package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;

public class Object {

    /**
     * 通过此本地方法 将类中的其他本地方法与其他语言或其他方式实现的方法做关联
     * 在调用的时候，可以直接使用，防止二次查找
     */
    private static native void registerNatives();
    static {
        registerNatives();
    }

    @HotSpotIntrinsicCandidate
    public Object() {}

    @HotSpotIntrinsicCandidate
    public final native Class<?> getClass();

    /** returns a hash code value for the object */
    @HotSpotIntrinsicCandidate
    public native int hashCode();

    /** Indicates whether some other object is "equal to" this one. */
    public boolean equals(Object obj) {
        return (this == obj);
    }

    public String toString() {
//        return getClass() TODO

        return "";
    }
}
