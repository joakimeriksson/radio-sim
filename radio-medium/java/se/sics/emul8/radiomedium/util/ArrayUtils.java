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
 * -----------------------------------------------------------------
 *
 * ArrayUtils
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : Tue May 15 16:18:45 2007
 */
package se.sics.emul8.radiomedium.util;

/**
 */
public class ArrayUtils {

    private ArrayUtils() {
        // prevent instances of this class
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

    public static <T> T[] remove(T[] array, T value) {
        if (array != null) {
            for (int index = 0, n = array.length; index < n; index++) {
                if (value.equals(array[index])) {
                    if (n == 1) {
                        return null;
                    }
                    @SuppressWarnings("unchecked")
                    T[] tmp = (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), array.length - 1);
                    if (index > 0) {
                        System.arraycopy(array, 0, tmp, 0, index);
                    }
                    if (index < tmp.length) {
                        System.arraycopy(array, index + 1, tmp, index, tmp.length - index);
                    }
                    return tmp;
                }
            }
        }
        return array;
    }

} // ArrayUtils
