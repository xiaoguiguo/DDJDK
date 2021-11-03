package java.lang;

import java.io.Serializable;

/**
 * 抽象类 Number 是平台类的超类，表示可转换为基本类型 byte、double、float、int、long 和 short 的数值。
 * 从特定 Number 实现的数值到给定基本类型的转换的特定语义由所讨论的 Number 实现定义。
 * 对于平台类，转换通常类似于 Java™ 语言规范中定义的用于在原始类型之间进行转换的缩小原语转换或扩大原语转换。
 * 因此，转换可能会丢失有关数值整体大小的信息，可能会丢失精度，甚至可能返回与输入符号不同的结果
 */
public abstract class Number implements Serializable {
    private static final long serialVersionUID = 3166984097235214156L;

    public abstract int intValue();

    public abstract long longValue();

    public abstract float floatValue();

    public abstract double doubleValue();

    public byte byteValue() {
        return (byte) intValue();
    }

    public short shortValue() {
        return (short) intValue();
    }

}
