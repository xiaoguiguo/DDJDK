/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

// -- This file was mechanically generated: Do not edit! -- //

package java.nio;

/**

 * A read/write HeapByteBuffer.






 */

class HeapByteBuffer
    extends ByteBuffer
{
    // Cached array base offset
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);

    // Cached array index scale
    private static final long ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale(byte[].class);

    // For speed these fields are actually declared in X-Buffer;
    // these declarations are here as documentation
    /*

    protected final byte[] hb;
    protected final int offset;

    */

    HeapByteBuffer(int cap, int lim) {            // package-private

        super(-1, 0, lim, cap, new byte[cap], 0);
        /*
        hb = new byte[cap];
        offset = 0;
        */
        this.address = ARRAY_BASE_OFFSET;




    }

    HeapByteBuffer(byte[] buf, int off, int len) { // package-private

        super(-1, off, off + len, buf.length, buf, 0);
        /*
        hb = buf;
        offset = 0;
        */
        this.address = ARRAY_BASE_OFFSET;




    }

    protected HeapByteBuffer(byte[] buf,
                                   int mark, int pos, int lim, int cap,
                                   int off)
    {

        super(mark, pos, lim, cap, buf, off);
        /*
        hb = buf;
        offset = off;
        */
        this.address = ARRAY_BASE_OFFSET + off * ARRAY_INDEX_SCALE;




    }

    public ByteBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        int rem = (pos <= lim ? lim - pos : 0);
        return new HeapByteBuffer(hb,
                                        -1,
                                        0,
                                        rem,
                                        rem,
                                        pos + offset);
    }


    ByteBuffer slice(int pos, int lim) {
        assert (pos >= 0);
        assert (pos <= lim);
        int rem = lim - pos;
        return new HeapByteBuffer(hb,
                                        -1,
                                        0,
                                        rem,
                                        rem,
                                        pos + offset);
    }


    public ByteBuffer duplicate() {
        return new HeapByteBuffer(hb,
                                        this.markValue(),
                                        this.position(),
                                        this.limit(),
                                        this.capacity(),
                                        offset);
    }

    public ByteBuffer asReadOnlyBuffer() {

        return new HeapByteBufferR(hb,
                                     this.markValue(),
                                     this.position(),
                                     this.limit(),
                                     this.capacity(),
                                     offset);



    }



    protected int ix(int i) {
        return i + offset;
    }


    private long byteOffset(long i) {
        return address + i;
    }


    public byte get() {
        return hb[ix(nextGetIndex())];
    }

    public byte get(int i) {
        return hb[ix(checkIndex(i))];
    }







    public ByteBuffer get(byte[] dst, int offset, int length) {
        checkBounds(offset, length, dst.length);
        int pos = position();
        if (length > limit() - pos)
            throw new BufferUnderflowException();
        System.arraycopy(hb, ix(pos), dst, offset, length);
        position(pos + length);
        return this;
    }

    public boolean isDirect() {
        return false;
    }



    public boolean isReadOnly() {
        return false;
    }

    public ByteBuffer put(byte x) {

        hb[ix(nextPutIndex())] = x;
        return this;



    }

    public ByteBuffer put(int i, byte x) {

        hb[ix(checkIndex(i))] = x;
        return this;



    }

    public ByteBuffer put(byte[] src, int offset, int length) {

        checkBounds(offset, length, src.length);
        int pos = position();
        if (length > limit() - pos)
            throw new BufferOverflowException();
        System.arraycopy(src, offset, hb, ix(pos), length);
        position(pos + length);
        return this;



    }

    public ByteBuffer put(ByteBuffer src) {

            super.put(src);
        return this;



    }

    public ByteBuffer compact() {

        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        System.arraycopy(hb, ix(pos), hb, ix(0), rem);
        position(rem);
        limit(capacity());
        discardMark();
        return this;



    }





    byte _get(int i) {                          // package-private
        return hb[i];
    }

    void _put(int i, byte b) {                  // package-private

        hb[i] = b;



    }

    // char



    public char getChar() {
        return UNSAFE.getCharUnaligned(hb, byteOffset(nextGetIndex(2)), bigEndian);
    }

    public char getChar(int i) {
        return UNSAFE.getCharUnaligned(hb, byteOffset(checkIndex(i, 2)), bigEndian);
    }



    public ByteBuffer putChar(char x) {

        UNSAFE.putCharUnaligned(hb, byteOffset(nextPutIndex(2)), x, bigEndian);
        return this;



    }

    public ByteBuffer putChar(int i, char x) {

        UNSAFE.putCharUnaligned(hb, byteOffset(checkIndex(i, 2)), x, bigEndian);
        return this;



    }

    public CharBuffer asCharBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 1;
        long addr = address + pos;
        return (bigEndian
                ? (CharBuffer)(new ByteBufferAsCharBufferB(this,
                                                               -1,
                                                               0,
                                                               size,
                                                               size,
                                                               addr))
                : (CharBuffer)(new ByteBufferAsCharBufferL(this,
                                                               -1,
                                                               0,
                                                               size,
                                                               size,
                                                               addr)));
    }


    // short



    public short getShort() {
        return UNSAFE.getShortUnaligned(hb, byteOffset(nextGetIndex(2)), bigEndian);
    }

    public short getShort(int i) {
        return UNSAFE.getShortUnaligned(hb, byteOffset(checkIndex(i, 2)), bigEndian);
    }



    public ByteBuffer putShort(short x) {

        UNSAFE.putShortUnaligned(hb, byteOffset(nextPutIndex(2)), x, bigEndian);
        return this;



    }

    public ByteBuffer putShort(int i, short x) {

        UNSAFE.putShortUnaligned(hb, byteOffset(checkIndex(i, 2)), x, bigEndian);
        return this;



    }

    public ShortBuffer asShortBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 1;
        long addr = address + pos;
        return (bigEndian
                ? (ShortBuffer)(new ByteBufferAsShortBufferB(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 addr))
                : (ShortBuffer)(new ByteBufferAsShortBufferL(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 addr)));
    }


    // int



    public int getInt() {
        return UNSAFE.getIntUnaligned(hb, byteOffset(nextGetIndex(4)), bigEndian);
    }

    public int getInt(int i) {
        return UNSAFE.getIntUnaligned(hb, byteOffset(checkIndex(i, 4)), bigEndian);
    }



    public ByteBuffer putInt(int x) {

        UNSAFE.putIntUnaligned(hb, byteOffset(nextPutIndex(4)), x, bigEndian);
        return this;



    }

    public ByteBuffer putInt(int i, int x) {

        UNSAFE.putIntUnaligned(hb, byteOffset(checkIndex(i, 4)), x, bigEndian);
        return this;



    }

    public IntBuffer asIntBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 2;
        long addr = address + pos;
        return (bigEndian
                ? (IntBuffer)(new ByteBufferAsIntBufferB(this,
                                                             -1,
                                                             0,
                                                             size,
                                                             size,
                                                             addr))
                : (IntBuffer)(new ByteBufferAsIntBufferL(this,
                                                             -1,
                                                             0,
                                                             size,
                                                             size,
                                                             addr)));
    }


    // long



    public long getLong() {
        return UNSAFE.getLongUnaligned(hb, byteOffset(nextGetIndex(8)), bigEndian);
    }

    public long getLong(int i) {
        return UNSAFE.getLongUnaligned(hb, byteOffset(checkIndex(i, 8)), bigEndian);
    }



    public ByteBuffer putLong(long x) {

        UNSAFE.putLongUnaligned(hb, byteOffset(nextPutIndex(8)), x, bigEndian);
        return this;



    }

    public ByteBuffer putLong(int i, long x) {

        UNSAFE.putLongUnaligned(hb, byteOffset(checkIndex(i, 8)), x, bigEndian);
        return this;



    }

    public LongBuffer asLongBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 3;
        long addr = address + pos;
        return (bigEndian
                ? (LongBuffer)(new ByteBufferAsLongBufferB(this,
                                                               -1,
                                                               0,
                                                               size,
                                                               size,
                                                               addr))
                : (LongBuffer)(new ByteBufferAsLongBufferL(this,
                                                               -1,
                                                               0,
                                                               size,
                                                               size,
                                                               addr)));
    }


    // float



    public float getFloat() {
        int x = UNSAFE.getIntUnaligned(hb, byteOffset(nextGetIndex(4)), bigEndian);
        return Float.intBitsToFloat(x);
    }

    public float getFloat(int i) {
        int x = UNSAFE.getIntUnaligned(hb, byteOffset(checkIndex(i, 4)), bigEndian);
        return Float.intBitsToFloat(x);
    }



    public ByteBuffer putFloat(float x) {

        int y = Float.floatToRawIntBits(x);
        UNSAFE.putIntUnaligned(hb, byteOffset(nextPutIndex(4)), y, bigEndian);
        return this;



    }

    public ByteBuffer putFloat(int i, float x) {

        int y = Float.floatToRawIntBits(x);
        UNSAFE.putIntUnaligned(hb, byteOffset(checkIndex(i, 4)), y, bigEndian);
        return this;



    }

    public FloatBuffer asFloatBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 2;
        long addr = address + pos;
        return (bigEndian
                ? (FloatBuffer)(new ByteBufferAsFloatBufferB(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 addr))
                : (FloatBuffer)(new ByteBufferAsFloatBufferL(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 addr)));
    }


    // double



    public double getDouble() {
        long x = UNSAFE.getLongUnaligned(hb, byteOffset(nextGetIndex(8)), bigEndian);
        return Double.longBitsToDouble(x);
    }

    public double getDouble(int i) {
        long x = UNSAFE.getLongUnaligned(hb, byteOffset(checkIndex(i, 8)), bigEndian);
        return Double.longBitsToDouble(x);
    }



    public ByteBuffer putDouble(double x) {

        long y = Double.doubleToRawLongBits(x);
        UNSAFE.putLongUnaligned(hb, byteOffset(nextPutIndex(8)), y, bigEndian);
        return this;



    }

    public ByteBuffer putDouble(int i, double x) {

        long y = Double.doubleToRawLongBits(x);
        UNSAFE.putLongUnaligned(hb, byteOffset(checkIndex(i, 8)), y, bigEndian);
        return this;



    }

    public DoubleBuffer asDoubleBuffer() {
        int pos = position();
        int size = (limit() - pos) >> 3;
        long addr = address + pos;
        return (bigEndian
                ? (DoubleBuffer)(new ByteBufferAsDoubleBufferB(this,
                                                                   -1,
                                                                   0,
                                                                   size,
                                                                   size,
                                                                   addr))
                : (DoubleBuffer)(new ByteBufferAsDoubleBufferL(this,
                                                                   -1,
                                                                   0,
                                                                   size,
                                                                   size,
                                                                   addr)));
    }















































}

