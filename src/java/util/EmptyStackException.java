/*
 * Copyright (c) 1994, 2008, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

/**
 * Thrown by methods in the {@code Stack} class to indicate
 * that the stack is empty.
 *
 * @author  Jonathan Payne
 * @see     Stack
 * @since   1.0
 */
public
class EmptyStackException extends RuntimeException {
    private static final long serialVersionUID = 5084686378493302095L;

    /**
     * Constructs a new {@code EmptyStackException} with {@code null}
     * as its error message string.
     */
    public EmptyStackException() {
    }
}
