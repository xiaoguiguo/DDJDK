package java.lang;

public class Object {

    /**
     * https://blog.csdn.net/Saintyyu/article/details/90452826
     */
    private static native void registerNatives();
    static {
        registerNatives();
    }

//    @HotSpotIntrinsicCandidate TODO
    public Object() {}
}
