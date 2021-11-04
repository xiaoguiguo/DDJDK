package java.security;

/**
 * @className: Guard
 * @author: doudou
 * @datetime: 2021/11/2
 * @description: This interface represents a guard, which is an object that is used to protect access to another object.
 */
public interface Guard {

    /**
     * 确定是否允许访问受保护的对象 object 。 如果允许访问，则静默返回。 否则，抛出 SecurityException。
     */
    void checkGuard(Object object) throws SecurityException;
}
