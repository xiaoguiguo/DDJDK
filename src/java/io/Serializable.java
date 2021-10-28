package java.io;

/**
 * Serializable是java.io包中定义的、用于实现Java类的序列化操作而提供的一个语义级别的接口。
 * Serializable序列化接口没有任何方法或者字段，只是用于标识可序列化的语义。
 * 实现了Serializable接口的类可以被ObjectOutputStream转换为字节流，同时也可以通过ObjectInputStream再将其解析为对象。
 * 例如，我们可以将序列化对象写入文件后，再次从文件中读取它并反序列化成对象，也就是说，
 * 可以使用表示对象及其数据的类型信息和字节在内存中重新创建对象。
 *
 * @see java.io.ObjectOutput
 * @see java.io.ObjectInputStream
 * @see java.io.ObjectInput
 * @see java.io.ObjectInputStream
 * @see java.io.Externalizable
 */
public interface Serializable {
}
