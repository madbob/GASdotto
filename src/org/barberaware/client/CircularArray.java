/*
 * $Header: /home/cvs/jakarta-commons/collections/src/java/org/apache/commons/collections/CircularArray.java,v 1.5 2002/10/13 12:59:04 scolebourne Exp $
 * $Revision: 1.5 $
 * $Date: 2002/10/13 12:59:04 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.barberaware.client;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
/**
 * The CircularArray is a <strong>very</strong> efficient implementation of
 * Buffer that does not alter the size of the buffer at runtime.
 * <p>
 * The removal order of a <code>CircularArray</code> is based on the
 * insertion order; elements are removed in the same order in which they
 * were added.  The iteration order is the same as the removal order.
 * <p>
 * The {@link #add(Object)}, {@link #remove()} and {@link #get()} operations
 * all perform in constant time.  All other operations perform in linear
 * time or worse.
 * <p>
 * Note that this implementation is not synchronized.  The following can be
 * used to provide synchronized access to your <code>CircularArray</code>:
 * <pre>
 *   Buffer fifo = BufferUtils.synchronizedBuffer(new CircularArray());
 * </pre>
 * <p>
 * This buffer prevents null objects from being added.
 *
 * @author Avalon
 * @author <a href="mailto:bloritsch@apache.org">Berin Loritsch</a>
 * @author Paul Jack
 * @author Stephen Colebourne
 * @since 2.1
 * @version $Id: CircularArray.java,v 1.5 2002/10/13 12:59:04 scolebourne Exp $
 */
public class CircularArray extends AbstractCollection {
    private final Object[] m_elements;
    private int m_start = 0;
    private int m_end = 0;
    private boolean m_full = false;

    /**
     * Constructs a new <code>CircularArray</code> big enough to hold
     * 32 elements.
     */
    public CircularArray() {
        this(32);
    }

    /**
     * Constructs a new <code>CircularArray</code> big enough to hold
     * the specified number of elements.
     *
     * @param size  the maximum number of elements for this fifo
     * @throws IllegalArgumentException  if the size is less than 1
     */
    public CircularArray(int size) {
        if (size <= 0) {
            size = 32;
        }
        m_elements = new Object[size];
    }

    /**
     * Constructs a new <code>CircularArray</code> big enough to hold all
     * of the elements in the specified collection. That collection's
     * elements will also be added to the buffer.
     *
     * @param coll  the collection whose elements to add
     */
    public CircularArray(Collection coll) {
        this(coll.size());
        addAll(coll);
    }

    /**
     * Returns the number of elements stored in the buffer.
     *
     * @return this buffer's size
     */
    public int size() {
        int size = 0;

        if (m_end < m_start) {
            size = m_elements.length - m_start + m_end;
        } else if (m_end == m_start) {
            size = (m_full ? m_elements.length : 0);
        } else {
            size = m_end - m_start;
        }

        return size;
    }

    /**
     * Returns true if this buffer is empty; false otherwise.
     *
     * @return true if this buffer is empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Clears this buffer.
     */
    public void clear() {
        m_full = false;
        m_start = 0;
        m_end = 0;

        for ( int i = 0; i < m_elements.length; i++ )
            m_elements [ i ] = null;
    }

    /**
     * Adds the given element to this buffer.
     *
     * @param element  the element to add
     * @return true, always
     * @throws NullPointerException  if the given element is null
     * @throws BufferOverflowException  if this buffer is full
     */
    public boolean add(Object element) {
        if (null == element) {
            return false;
        }

        if (m_full) {
            return false;
        }

        m_elements[m_end++] = element;

        if (m_end >= m_elements.length) {
            m_end = 0;
        }

        if (m_end == m_start) {
            m_full = true;
        }

        return true;
    }

    /**
     * Returns the least recently inserted element in this buffer.
     *
     * @return the least recently inserted element
     * @throws BufferUnderflowException  if the buffer is empty
     */
    public Object get() {
        if (isEmpty()) {
            return null;
        }

        return m_elements[m_start];
    }

    /**
     * Removes the least recently inserted element from this buffer.
     *
     * @return the least recently inserted element
     * @throws BufferUnderflowException  if the buffer is empty
     */
    public Object remove() {
        if (isEmpty()) {
            return null;
        }

        Object element = m_elements[m_start];

        if (null != element) {
            m_elements[m_start++] = null;

            if (m_start >= m_elements.length) {
                m_start = 0;
            }

            m_full = false;
        }

        return element;
    }

    /**
     * Increments the internal index.
     *
     * @param index  the index to increment
     * @return the updated index
     */
    private int increment(int index) {
        index++;
        if (index >= m_elements.length) {
            index = 0;
        }
        return index;
    }

    /**
     * Decrements the internal index.
     *
     * @param index  the index to decrement
     * @return the updated index
     */
    private int decrement(int index) {
        index--;
        if (index < 0) {
            index = m_elements.length - 1;
        }
        return index;
    }

    /**
     * Returns an iterator over this buffer's elements.
     *
     * @return an iterator over this buffer's elements
     */
    public Iterator iterator() {
        return new Iterator() {

            private int index = m_start;
            private int lastReturnedIndex = -1;
            private boolean isFirst = m_full;

            public boolean hasNext() {
                return isFirst || (index != m_end);

            }

            public Object next() {
                if (!hasNext()) return null;
                isFirst = false;
                lastReturnedIndex = index;
                index = increment(index);
                return m_elements[lastReturnedIndex];
            }

            public void remove() {
                if (lastReturnedIndex == -1) return;

                // First element can be removed quickly
                if (lastReturnedIndex == m_start) {
                    CircularArray.this.remove();
                    lastReturnedIndex = -1;
                    return;
                }

                // Other elements require us to shift the subsequent elements
                int i = lastReturnedIndex + 1;
                while (i != m_end) {
                    if (i >= m_elements.length) {
                        m_elements[i - 1] = m_elements[0];
                        i = 0;
                    } else {
                        m_elements[i - 1] = m_elements[i];
                        i++;
                    }
                }

                lastReturnedIndex = -1;
                m_end = decrement(m_end);
                m_elements[m_end] = null;
                m_full = false;
                index = decrement(index);
            }

        };
    }
}
