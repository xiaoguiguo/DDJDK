/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;

/**
 * Byte 类将基本类型为 byte 的值包装在一个对象中。
 * 一个 Byte 类的对象只包含一个类型为 byte 的字段。此外，该类还为 byte 和 String 的相互转换提供了方法，
 * 并提供了一些处理 byte 时非常有用的常量和方法。
 */
public final class Byte extends Number implements Comparable<Byte> {

    /**
     * byte的最小值，-2^7
     */
    public static final byte   MIN_VALUE = -128;

    /**
     * byte的最大值，2^7-1
     */
    public static final byte   MAX_VALUE = 127;

    /**
     * byte基本类型的包装类型
     */
    @SuppressWarnings("unchecked")
    public static final Class<Byte>     TYPE = (Class<Byte>) Class.getPrimitiveClass("byte");

    /**
     * 指定byte的字符串表示
     */
    public static String toString(byte b) {
        return Integer.toString((int)b, 10);
    }

    private static class ByteCache {
        private ByteCache(){}

        static final Byte[] cache = new Byte[-(-128) + 127 + 1];

        static {
            for(int i = 0; i < cache.length; i++) {
                cache[i] = new Byte((byte)(i - 128));
            }
        }
    }

    /**
     * 由给定参数(值)表示的字节对象
     */
    @HotSpotIntrinsicCandidate
    public static Byte valueOf(byte b) {
        final int offset = 128;
        return ByteCache.cache[(int)b + offset];
    }

    /**
     * 将 String 型参数解析成等价的 byte 形式
     */
    public static byte parseByte(String s, int radix)
        throws NumberFormatException {
        int i = Integer.parseInt(s, radix);
        if (i < MIN_VALUE || i > MAX_VALUE) {
            throw new NumberFormatException(
                "Value out of range. Value:\"" + s + "\" Radix:" + radix);
        }
        return (byte)i;
    }

    /**
     * 将 String 型参数解析成等价的 byte 形式
     */
    public static byte parseByte(String s) throws NumberFormatException {
        return parseByte(s, 10);
    }

    /**
     * 返回一个保持指定 String 所给出的值的 Byte 对象
     */
    public static Byte valueOf(String s, int radix)
        throws NumberFormatException {
        return valueOf(parseByte(s, radix));
    }

    /**
     * 返回一个保持指定 String 所给出的值的 Byte 对象
     */
    public static Byte valueOf(String s) throws NumberFormatException {
        return valueOf(s, 10);
    }

    /**
     * 将 String 解码为 Byte。它可以接受十进制、十六进制和八进制数。将 String 解码为 Byte。它可以接受十进制、十六进制和八进制数。
     */
    public static Byte decode(String nm) throws NumberFormatException {
        int i = Integer.decode(nm);
        if (i < MIN_VALUE || i > MAX_VALUE) {
            throw new NumberFormatException(
                    "Value " + i + " out of range from input " + nm);
        }
        return valueOf((byte)i);
    }

    /**
     * Byte的值
     */
    private final byte value;

    /**
     * 构造方法
     */
    @Deprecated(since="9")
    public Byte(byte value) {
        this.value = value;
    }

    /**
     * 构造方法
     */
    @Deprecated(since="9")
    public Byte(String s) throws NumberFormatException {
        this.value = parseByte(s, 10);
    }

    /**
     * 返回byte的值
     */
    @HotSpotIntrinsicCandidate
    public byte byteValue() {
        return value;
    }

    /**
     * 用于将这个Byte对象的值作为 short 返回
     */
    public short shortValue() {
        return (short)value;
    }

    /**
     * 用于将这个Byte对象的值作为 int 返回
     */
    public int intValue() {
        return (int)value;
    }

    /**
     * 用于将这个Byte对象的值作为 long 返回
     */
    public long longValue() {
        return (long)value;
    }

    /**
     * 用于将这个Byte对象的值作为 float 返回
     */
    public float floatValue() {
        return (float)value;
    }

    /**
     * 用于将这个Byte对象的值作为 double 返回
     */
    public double doubleValue() {
        return (double)value;
    }

    /**
     * Byte 的字符串表示
     */
    public String toString() {
        return Integer.toString((int)value);
    }

    /**
     * 返回当前Byte的hashcode
     */
    @Override
    public int hashCode() {
        return Byte.hashCode(value);
    }

    /**
     * 返回指定byte的hashcode
     */
    public static int hashCode(byte value) {
        return (int)value;
    }

    /**
     * 比较是否相等
     */
    public boolean equals(Object obj) {
        if (obj instanceof Byte) {
            return value == ((Byte)obj).byteValue();
        }
        return false;
    }

    /**
     * 比较大小
     */
    public int compareTo(Byte anotherByte) {
        return compare(this.value, anotherByte.value);
    }

    /**
     * 比较大小
     */
    public static int compare(byte x, byte y) {
        return x - y;
    }

    /**
     * 通过将值视为无符号来对两个字节对象进行数字比较。
     * @since 9
     */
    public static int compareUnsigned(byte x, byte y) {
        return Byte.toUnsignedInt(x) - Byte.toUnsignedInt(y);
    }

    /**
     * 通过无符号转换将指定的参数转换为 int
     * @since 1.8
     */
    public static int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    /**
     * 通过无符号转换将指定的参数转换为 long。
     * @since 1.8
     */
    public static long toUnsignedLong(byte x) {
        return ((long) x) & 0xffL;
    }


    /**
     * 用于以二进制补码形式表示 {@code byte} 值的位数。
     */
    public static final int SIZE = 8;

    /**
     * 用于以二进制补码形式表示 {@code byte} 值的字节数。
     */
    public static final int BYTES = SIZE / Byte.SIZE;

    /** use serialVersionUID from JDK 1.1. for interoperability */
    private static final long serialVersionUID = -7183698231559129828L;
}
