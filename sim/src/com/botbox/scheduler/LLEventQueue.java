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
 * $Id: LLEventQueue.java,v 1.1 2007/12/17 21:11:03 joakime Exp $
 *
 * -----------------------------------------------------------------
 *
 * LLEventQueue
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : Fri Dec 14 00:31:13 2007
 * Updated : $Date: 2007/12/17 21:11:03 $
 *           $Revision: 1.1 $
 */
package com.botbox.scheduler;

/**
 */
public class LLEventQueue {

    private TimeEvent first;

    public LLEventQueue() {
    }

    public void addEvent(TimeEvent event) {
        if (first == null) {
            first = event;
        } else {
            TimeEvent pos = first;
            TimeEvent lastPos = first;
            while (pos != null && pos.time < event.time) {
                lastPos = pos;
                pos = pos.nextEvent;
            }
            // Here pos will be the first TE after event
            // and lastPos the first before
            if (pos == first) {
                // Before all other
                event.nextEvent = pos;
                first = event;
            } else {
                event.nextEvent = pos;
                lastPos.nextEvent = event;
            }
        }
    }

    public TimeEvent popFirst() {
        TimeEvent tmp = first;
        if (tmp != null) {
            first = tmp.nextEvent;
            // Unlink.
            tmp.nextEvent = null;
        }
        return tmp;
    }
} // LLEventQueue
