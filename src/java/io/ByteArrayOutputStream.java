/*
 * Copyright (c) 1994, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class implements an output stream in which the data is
 * written into a byte array. The buffer automatically grows as data
 * is written to it.
 * The data can be retrieved using {@code toByteArray()} and
 * {@code toString()}.
 * <p>
 * Closing a {@code ByteArrayOutputStream} has no effect. The methods in
 * this class can be called after the stream has been closed without
 * generating an {@code IOException}.
 *
 * @author  Arthur van Hoff
 * @since   1.0
 */

public class ByteArrayOutputStream extends OutputStream {

    /**
     * The buffer where data is stored.
     */
    protected byte buf[];

    /**
     * The number of valid bytes in the buffer.
     */
    protected int count;

    /**
     * Creates a new {@code ByteArrayOutputStream}. The buffer capacity is
     * initially 32 bytes, though its size increases if necessary.
     */
    public ByteArrayOutputStream() {
        this(32);
    }

    /**
     * Creates a new {@code ByteArrayOutputStream}, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param  size   the initial size.
     * @throws IllegalArgumentException if size is negative.
     */
    public ByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: "
                                               + size);
        }
        buf = new byte[size];
    }

    /**
     * Increases the capacity if necessary to ensure that it can hold
     * at least the number of elements specified by the minimum
     * capacity argument.
     *
     * @param  minCapacity the desired minimum capacity
     * @throws OutOfMemoryError if {@code minCapacity < 0}.  This is
     * interpreted as a request for the unsatisfiably large capacity
     * {@code (long) Integer.MAX_VALUE + (minCapacity - Integer.MAX_VALUE)}.
     */
    private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buf.length > 0)
            grow(minCapacity);
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        buf = Arrays.copyOf(buf, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }

    /**
     * Writes the specified byte to this {@code ByteArrayOutputStream}.
     *
     * @param   b   the byte to be written.
     */
    public synchronized void write(int b) {
        ensureCapacity(count + 1);
        buf[count] = (byte) b;
        count += 1;
    }

    /**
     * Writes {@code len} bytes from the specified byte array
     * starting at offset {@code off} to this {@code ByteArrayOutputStream}.
     *
     * @param   b     the data.
     * @param   off   the start offset in the data.
     * @param   len   the number of bytes to write.
     * @throws  NullPointerException if {@code b} is {@code null}.
     * @throws  IndexOutOfBoundsException if {@code off} is negative,
     * {@code len} is negative, or {@code len} is greater than
     * {@code b.length - off}
     */
    public synchronized void write(byte b[], int off, int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        ensureCapacity(count + len);
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    /**
     * Writes the complete contents of the specified byte array
     * to this {@code ByteArrayOutputStream}.
     *
     * @apiNote
     * This method is equivalent to {@link #write(byte[],int,int)
     * write(b, 0, b.length)}.
     *
     * @param   b     the data.
     * @throws  NullPointerException if {@code b} is {@code null}.
     * @since   11
     */
    public void writeBytes(byte b[]) {
        write(b, 0, b.length);
    }

    /**
     * Writes the complete contents of this {@code ByteArrayOutputStream} to
     * the specified output stream argument, as if by calling the output
     * stream's write method using {@code out.write(buf, 0, count)}.
     *
     * @param   out   the output stream to which to write the data.
     * @throws  NullPointerException if {@code out} is {@code null}.
     * @throws  IOException if an I/O error occurs.
     */
    public synchronized void writeTo(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    /**
     * Resets the {@code count} field of this {@code ByteArrayOutputStream}
     * to zero, so that all currently accumulated output in the
     * output stream is discarded. The output stream can be used again,
     * reusing the already allocated buffer space.
     *
     * @see     ByteArrayInputStream#count
     */
    public synchronized void reset() {
        count = 0;
    }

    /**
     * Creates a newly allocated byte array. Its size is the current
     * size of this output stream and the valid contents of the buffer
     * have been copied into it.
     *
     * @return  the current contents of this output stream, as a byte array.
     * @see     ByteArrayOutputStream#size()
     */
    public synchronized byte[] toByteArray() {
        return Arrays.copyOf(buf, count);
    }

    /**
     * Returns the current size of the buffer.
     *
     * @return  the value of the {@code count} field, which is the number
     *          of valid bytes in this output stream.
     * @see     ByteArrayOutputStream#count
     */
    public synchronized int size() {
        return count;
    }

    /**
     * Converts the buffer's contents into a string decoding bytes using the
     * platform's default character set. The length of the new {@code String}
     * is a function of the character set, and hence may not be equal to the
     * size of the buffer.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with the default replacement string for the platform's
     * default character set. The {@linkplain java.nio.charset.CharsetDecoder}
     * class should be used when more control over the decoding process is
     * required.
     *
     * @return String decoded from the buffer's contents.
     * @since  1.1
     */
    public synchronized String toString() {
        return new String(buf, 0, count);
    }

    /**
     * Converts the buffer's contents into a string by decoding the bytes using
     * the named {@link Charset charset}.
     *
     * <p> This method is equivalent to {@code #toString(charset)} that takes a
     * {@link Charset charset}.
     *
     * <p> An invocation of this method of the form
     *
     * <pre> {@code
     *      ByteArrayOutputStream b = ...
     *      b.toString("UTF-8")
     *      }
     * </pre>
     *
     * behaves in exactly the same way as the expression
     *
     * <pre> {@code
     *      ByteArrayOutputStream b = ...
     *      b.toString(StandardCharsets.UTF_8)
     *      }
     * </pre>
     *
     *
     * @param  charsetName  the name of a supported
     *         {@link Charset charset}
     * @return String decoded from the buffer's contents.
     * @throws UnsupportedEncodingException
     *         If the named charset is not supported
     * @since  1.1
     */
    public synchronized String toString(String charsetName)
        throws UnsupportedEncodingException
    {
        return new String(buf, 0, count, charsetName);
    }

    /**
     * Converts the buffer's contents into a string by decoding the bytes using
     * the specified {@link Charset charset}. The length of the new
     * {@code String} is a function of the charset, and hence may not be equal
     * to the length of the byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with the charset's default replacement string. The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param      charset  the {@linkplain Charset charset}
     *             to be used to decode the {@code bytes}
     * @return     String decoded from the buffer's contents.
     * @since      10
     */
    public synchronized String toString(Charset charset) {
        return new String(buf, 0, count, charset);
    }

    /**
     * Creates a newly allocated string. Its size is the current size of
     * the output stream and the valid contents of the buffer have been
     * copied into it. Each character <i>c</i> in the resulting string is
     * constructed from the corresponding element <i>b</i> in the byte
     * array such that:
     * <blockquote><pre>{@code
     *     c == (char)(((hibyte & 0xff) << 8) | (b & 0xff))
     * }</pre></blockquote>
     *
     * @deprecated This method does not properly convert bytes into characters.
     * As of JDK&nbsp;1.1, the preferred way to do this is via the
     * {@link #toString(String charsetName)} or {@link #toString(Charset charset)}
     * method, which takes an encoding-name or charset argument,
     * or the {@code toString()} method, which uses the platform's default
     * character encoding.
     *
     * @param      hibyte    the high byte of each resulting Unicode character.
     * @return     the current contents of the output stream, as a string.
     * @see        ByteArrayOutputStream#size()
     * @see        ByteArrayOutputStream#toString(String)
     * @see        ByteArrayOutputStream#toString()
     */
    @Deprecated
    public synchronized String toString(int hibyte) {
        return new String(buf, hibyte, 0, count);
    }

    /**
     * Closing a {@code ByteArrayOutputStream} has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an {@code IOException}.
     */
    public void close() throws IOException {
    }

}
