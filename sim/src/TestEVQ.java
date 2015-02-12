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
 * $Id: TestEVQ.java,v 1.1 2007/12/14 17:18:02 joakime Exp $
 *
 * -----------------------------------------------------------------
 *
 * TestEVQ
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : Thu Dec 13 22:03:25 2007
 * Updated : $Date: 2007/12/14 17:18:02 $
 *           $Revision: 1.1 $
 */

import com.botbox.scheduler.*;

public class TestEVQ {

    public static int expNo = 0;
    public static int[] EVENT_IN_Q = new int[] { 1, 10, 100, 1000, 10000 };
    public static int EVENT_TESTS = 1000000;
    public static long startTime;

    public static void start(String name) {
        System.out.println("-----------------------------------------");
        System.out.println("Type: " + name);
        startTime = System.currentTimeMillis();
    }

    public static void end() {
        long elapsed = (System.currentTimeMillis() - startTime);
        System.out.println("Time elapsed: " + elapsed);
        System.out.println("Events / ms: " + (EVENT_TESTS / elapsed));
        System.out.println("Events in Q: " + EVENT_IN_Q[expNo]);
    }

    public static void main(String[] args) {
        System.out.println("Test of different event queues");

        while (expNo < EVENT_IN_Q.length) {
            System.out.println("=========================================");
            start("Ladder Event Q");
            EventQueue eq = new EventQueue();
            long time = 0;
            for (int i = 0, n = EVENT_IN_Q[expNo]; i < n; i++) {
                eq.addEvent(new TestEvent((long) (Math.random() * 1000)));
            }
            for (int i = 0, n = EVENT_TESTS; i < n; i++) {
                eq.addEvent(new TestEvent(time + 10 + (long) (Math.random() * 1000)));
                TimeEvent te = eq.popFirst();
                if (te != null) {
                    time = te.getTime();
                }
            }
            end();

            start("ArrayQueue Event Q");
            ALEventQueue aeq = new ALEventQueue();
            time = 0;
            for (int i = 0, n = EVENT_IN_Q[expNo]; i < n; i++) {
                aeq.addEvent(new TestEvent((long) (Math.random() * 1000)));
            }
            for (int i = 0, n = EVENT_TESTS; i < n; i++) {
                aeq.addEvent(new TestEvent(time + 10 + (long) (Math.random() * 1000)));
                TimeEvent te = aeq.popFirst();
                if (te != null) {
                    time = te.getTime();
                }
            }
            end();

            start("LinkedList Event Q");
            LLEventQueue leq = new LLEventQueue();
            time = 0;
            for (int i = 0, n = EVENT_IN_Q[expNo]; i < n; i++) {
                leq.addEvent(new TestEvent((long) (Math.random() * 1000)));
            }
            for (int i = 0, n = EVENT_TESTS; i < n; i++) {
                leq.addEvent(new TestEvent(time + 10 + (long) (Math.random() * 1000)));
                TimeEvent te = leq.popFirst();
                if (te != null) {
                    time = te.getTime();
                }
            }
            end();

            expNo++;
        }

    }

    public static class TestEvent extends TimeEvent {
        public TestEvent(long time) {
            super(time);
        }

        public void execute(long time) {
        }
    }
} // TestEVQ
