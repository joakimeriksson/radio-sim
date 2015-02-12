/*
 * Copyright (c) 2007, SICS AB.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 * 3. The name of the author may not be used to endorse or promote
 *    products derived from this software without specific prior
 *    written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * $Id: ArrayUtils.java,v 1.1 2007/05/15 14:29:53 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * ArrayUtils
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : Tue May 15 16:18:45 2007
 * Updated : $Date: 2007/05/15 14:29:53 $
 *           $Revision: 1.1 $
 */
package se.sics.sim.util;

/**
 */
public class ArrayUtils {

    private ArrayUtils() {
        // prevent instances of this class
    }

    /**
     * Returns the index of the specified value using equals() as comparison.
     * This method handles if the array is <CODE>null</CODE>.
     *
     * @param array
     *            the array to search in
     * @param element
     *            the element to search for
     * @return the index of the specified element or <CODE>-1</CODE> if the
     *         element was not found in the array.
     */
    public static int indexOf(Object[] array, Object element) {
        if (array != null) {
            if (element == null) {
                for (int i = 0, n = array.length; i < n; i++) {
                    if (array[i] == null) {
                        return i;
                    }
                }
            } else {
                for (int i = 0, n = array.length; i < n; i++) {
                    if (element.equals(array[i])) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public static Object[] add(Object[] array, Object value) {
        Object[] tmp = (Object[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(),
                array.length + 1);
        System.arraycopy(array, 0, tmp, 0, array.length);
        tmp[array.length] = value;
        return tmp;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] add(Class<T> componentType, T[] array, T value) {
        T[] tmp;
        if (array == null) {
            tmp = (T[]) java.lang.reflect.Array.newInstance(componentType, 1);
        } else {
            tmp = (T[]) java.lang.reflect.Array.newInstance(componentType, array.length + 1);
            System.arraycopy(array, 0, tmp, 0, array.length);
        }
        tmp[tmp.length - 1] = value;
        return tmp;
    }

    /**
     * Removes the element at the specified index. If the array only contains
     * one element (the removed one), <CODE>null</CODE> is returned.
     *
     * @param array
     *            the array to remove the element from
     * @param index
     *            the index of the element to remove
     * @return the array with the element removed or <CODE>null</CODE> if the
     *         array no longer contains any elements.
     */
    public static Object[] remove(Object[] array, int index) {
        // Check that the index is a valid index
        if ((index < 0) || (index >= array.length))
            throw new ArrayIndexOutOfBoundsException(index);

        if (array.length == 1)
            return null;

        Object[] tmp = (Object[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(),
                array.length - 1);
        if (index > 0) {
            System.arraycopy(array, 0, tmp, 0, index);
        }
        if (index < tmp.length) {
            System.arraycopy(array, index + 1, tmp, index, tmp.length - index);
        }
        return tmp;
    }

    public static Object[] remove(Object[] array, Object element) {
        int index = indexOf(array, element);
        return (index >= 0) ? remove(array, index) : array;
    }

} // ArrayUtils
