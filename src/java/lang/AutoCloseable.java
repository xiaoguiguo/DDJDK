package java.lang;

/**
 * 一个可能持有资源（例如文件或套接字句柄）直到关闭的对象。
 * AutoCloseable 对象的 close() 方法在退出 try-with-resources 块时自动调用，
 * 该块已在资源规范标头中声明了该对象。 这种构造确保及时释放，避免资源耗尽异常和否则可能发生的错误。
 */
public interface AutoCloseable {

    /**
     * 主动调用 关闭资源
     */
    void close() throws Exception;
}
