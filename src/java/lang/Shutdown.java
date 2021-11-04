package java.lang;

/**
 * @className: Shutdown
 * @author: doudou
 * @datetime: 2021/11/4
 * @description: Package-private utility class containing data structures and logic
 *      governing the virtual-machine shutdown sequence.
 */
class Shutdown {

    private static class Lock {};

    private static Object lock = new Lock();

    static void exit(int status) {
        synchronized (lock) {
            // TODO
        }
    }
}
