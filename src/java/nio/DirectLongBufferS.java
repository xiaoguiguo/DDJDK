/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileDescriptor;
import java.lang.ref.Reference;
import jdk.internal.misc.VM;
import jdk.internal.ref.Cleaner;
import sun.nio.ch.DirectBuffer;


class DirectLongBufferS

    extends LongBuffer



    implements DirectBuffer
{



    // Cached array base offset
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(long[].class);

    // Cached unaligned-access capability
    protected static final boolean UNALIGNED = Bits.unaligned();

    // Base address, used in all indexing calculations
    // NOTE: moved up to Buffer.java for speed in JNI GetDirectBufferAddress
    //    protected long address;

    // An object attached to this buffer. If this buffer is a view of another
    // buffer then we use this field to keep a reference to that buffer to
    // ensure that its memory isn't freed before we are done with it.
    private final Object att;

    public Object attachment() {
        return att;
    }




































    public Cleaner cleaner() { return null; }


















































































    // For duplicates and slices
    //
    DirectLongBufferS(DirectBuffer db,         // package-private
                               int mark, int pos, int lim, int cap,
                               int off)
    {

        super(mark, pos, lim, cap);
        address = db.address() + off;



        att = db;




    }

    @Override
    Object base() {
        return null;
    }

    public LongBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        int rem = (pos <= lim ? lim - pos : 0);
        int off = (pos << 3);
        assert (off >= 0);
        return new DirectLongBufferS(this, -1, 0, rem, rem, off);
    }










    public LongBuffer duplicate() {
        return new DirectLongBufferS(this,
                                              this.markValue(),
                                              this.position(),
                                              this.limit(),
                                              this.capacity(),
                                              0);
    }

    public LongBuffer asReadOnlyBuffer() {

        return new DirectLongBufferRS(this,
                                           this.markValue(),
                                           this.position(),
                                           this.limit(),
                                           this.capacity(),
                                           0);



    }



    public long address() {
        return address;
    }

    private long ix(int i) {
        return address + ((long)i << 3);
    }

    public long get() {
        try {
            return (Bits.swap(UNSAFE.getLong(ix(nextGetIndex()))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }

    public long get(int i) {
        try {
            return (Bits.swap(UNSAFE.getLong(ix(checkIndex(i)))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }











    public LongBuffer get(long[] dst, int offset, int length) {

        if (((long)length << 3) > Bits.JNI_COPY_TO_ARRAY_THRESHOLD) {
            checkBounds(offset, length, dst.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if (length > rem)
                throw new BufferUnderflowException();

            long dstOffset = ARRAY_BASE_OFFSET + ((long)offset << 3);
            try {

                if (order() != ByteOrder.nativeOrder())
                    UNSAFE.copySwapMemory(null,
                                          ix(pos),
                                          dst,
                                          dstOffset,
                                          (long)length << 3,
                                          (long)1 << 3);
                else

                    UNSAFE.copyMemory(null,
                                      ix(pos),
                                      dst,
                                      dstOffset,
                                      (long)length << 3);
            } finally {
                Reference.reachabilityFence(this);
            }
            position(pos + length);
        } else {
            super.get(dst, offset, length);
        }
        return this;



    }



    public LongBuffer put(long x) {

        try {
            UNSAFE.putLong(ix(nextPutIndex()), Bits.swap((x)));
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;



    }

    public LongBuffer put(int i, long x) {

        try {
            UNSAFE.putLong(ix(checkIndex(i)), Bits.swap((x)));
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;



    }

    public LongBuffer put(LongBuffer src) {

            super.put(src);
        return this;



    }

    public LongBuffer put(long[] src, int offset, int length) {

        if (((long)length << 3) > Bits.JNI_COPY_FROM_ARRAY_THRESHOLD) {
            checkBounds(offset, length, src.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if (length > rem)
                throw new BufferOverflowException();

            long srcOffset = ARRAY_BASE_OFFSET + ((long)offset << 3);
            try {

                if (order() != ByteOrder.nativeOrder())
                    UNSAFE.copySwapMemory(src,
                                          srcOffset,
                                          null,
                                          ix(pos),
                                          (long)length << 3,
                                          (long)1 << 3);
                else

                    UNSAFE.copyMemory(src,
                                      srcOffset,
                                      null,
                                      ix(pos),
                                      (long)length << 3);
            } finally {
                Reference.reachabilityFence(this);
            }
            position(pos + length);
        } else {
            super.put(src, offset, length);
        }
        return this;



    }

    public LongBuffer compact() {

        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        try {
            UNSAFE.copyMemory(ix(pos), ix(0), (long)rem << 3);
        } finally {
            Reference.reachabilityFence(this);
        }
        position(rem);
        limit(capacity());
        discardMark();
        return this;



    }

    public boolean isDirect() {
        return true;
    }

    public boolean isReadOnly() {
        return false;
    }















































    public ByteOrder order() {

        return ((ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN)
                ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);





    }


















}

