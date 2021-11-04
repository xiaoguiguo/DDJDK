package java.io;

/**
 * @className: PrintStream
 * @author: doudou
 * @datetime: 2021/11/4
 * @description:
 *
 * PrintStream 继承了 FilterOutputStream，是"装饰类"的一种，所以属于字节流体系中为其他的输出流添加功能，
 * 使它们能够方便打印各种数据值的表示形式。此外，值得注意的是：
 *
 * 与其他流不同的是，PrintStream 流永远不会抛出异常，因为做了 try{}catch(){} 会将异常捕获，出现异常情况会在内部设置标识，通过 checkError() 获取此标识
 * PrintStream 流有自动刷新机制，例如当向 PrintStream 流中写入一个字节数组后自动调用 flush() 方法
 * PrintStream 流打印的字符通过平台默认的编码方式转换成字节，在写入的是字符而不是字节的情况下，
 * 应该使用 PrintWriter。PrintStream 流中基本所有的 print(Object obj) 重载方法和 println(Object obj) 重载方法都是通过将对应数据先转换成字符串，
 * 然后调用 write() 方法写到底层输出流中。常见用到 PrintStream 流：System.out 就被包装成 PrintStream 流，System.err 也是 PrintStream 流，
 * 注意，System.in 不是 PrintStream，而是没有包装过的 OutputStream。所以 System.in 不能直接使用。
 *
 * PrintStream 流不是直接将数据写到文件的流，需要传入底层输出流 out，而且要实现指定编码方式，需要中间流 OutputStreamWriter。
 * OutputStreamWriter 流实现了字符流以指定编码方式转换成字节流。此外为了提高写入文件的效率，使用到了字符缓冲流 BufferWriter。
 *
 * 写入 PrintStream 流的数据怎么写到文件中？需要先了解一下数据读取和写入的流程：
 *
 * 数据从流写到文件过程：输出流----->缓冲流----->转化流----->文件流------>文件
 * 数据从文件到流的过程：文件----->文件流----->转化流----->缓冲流----->输入流
 */
public class PrintStream extends FilterOutputStream implements Appendable, Closeable {



    public void println() {
        // TODO
    }

    public void println(boolean x) {
        synchronized (this) {
            // TODO
        }
    }


    public void println(String x) {
        synchronized (this) {
            // TODO
        }
    }
}
