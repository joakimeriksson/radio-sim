/**
 * @(#)EventQueue Created date: Sun Apr 22 11:37:35 2007
 * $Revision: 1.2 $, $Date: 2007/05/22 12:13:08 $
 *
 * Copyright (c) 2000-2007 BotBox AB.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * BotBox AB. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with
 * BotBox AB.
 */
package com.botbox.scheduler;

/**
 * An EventQueue -
 *
 * Based on ladder event queue by WAI TENG TANG, RICK SIOW MONG GOH and IAN
 * LI-JIN THNG National University of Singapore Published in ACM Transactions on
 * Modeling and Computer Simlation July 2005
 * --------------------------------------------------------------------- The
 * Ladded Queue is a three tier "lazy" sorted event queue
 *
 * | TOP | - consists of future events (far from now) | RUNG[] - buckets of
 * events in serveral "RUNGs" rungs closer to top have larger bucket widths and
 * rungs closer to bottom have smaller bucket widths | Bottom - sorted list of
 * events to be scheduled very soon
 *
 *
 * @author Joakim Eriksson (joakim.eriksson@botbox.com)
 * @author Niclas Finne (niclas.finne@botbox.com)
 * @version $Revision: 1.2 $, $Date: 2007/05/22 12:13:08 $
 */
public class EventQueue {

    public static boolean DEBUG = false;

    public static final int SPAWN_THRESHOLD = 20;
    // This BUCKET_NUMBER can be dynamic in a later implementation
    // Should be according to the ladder algorithm
    public static final int BUCKET_NUMBER = 20;

    // Max timestamp in top
    private long maxTS = 0;
    // min timestamp in top
    private long minTS = 0;
    // number of events in top!
    private int numTop = 0;

    private long topStart = 0;

    private TimeEvent topFirst = null;
    private TimeEvent topLast = null;

    // -------------------------------------------
    private Rung[] rungs = new Rung[20];
    private int rungCount;
    private Rung currentRung;
    private int numRung = 0;
    // -------------------------------------------
    private int numBottom = 0;
    private TimeEvent firstBottom = null;

    private long lastPopTime = 0;

    public EventQueue() {
    }

    public void addEvent(TimeEvent event) {
        if (event.nextEvent != null || event.prevEvent != null) {
            throw new IllegalStateException("Event already scheduled. "
                    + (event.nextEvent != null ? " Next event is " + event.nextEvent : "Previous event is "
                            + event.prevEvent));
        }
        if (event.time < lastPopTime) {
            throw new IllegalArgumentException("Can not insert a time value backwards in time - " + event.time + " vs "
                    + lastPopTime);
        }

        // Check if add to top!
        long time = event.time;
        if (time >= topStart) {
            if (topFirst == null) {
                topFirst = event;
                topLast = event;
                maxTS = minTS = time;
            } else {
                topLast.nextEvent = event;
                topLast = event;
            }
            // Update statistics for TOP
            if (time > maxTS)
                maxTS = time;
            if (time < minTS)
                minTS = time;
            numTop++;
        } else {
            int rung = 0;
            // Skip rungs with wrong 'start' time...
            while (rung < numRung && time < rungs[rung].bucketStartTime) {
                rung++;
            }
            if (rung < numRung) {
                if (DEBUG)
                    System.out.println("Inserting into rung: " + rung + " startTime: " + rungs[rung].bucketStartTime);
                Rung cRung = rungs[rung];
                // Insert into a bucket in a rung!
                int bucketIndex = (int) ((time - cRung.startTime) / cRung.bucketWidth);
                if (cRung.numBucket[bucketIndex] == 0) {
                    // First element!
                    cRung.bucketFirst[bucketIndex] = cRung.bucketLast[bucketIndex] = event;
                } else {
                    // Append to the list...
                    cRung.bucketLast[bucketIndex].nextEvent = event;
                    cRung.bucketLast[bucketIndex] = event;
                }
                cRung.numBucket[bucketIndex]++;
                cRung.numTotal++;
            } else {
                // Insert into bottom!
                // Here we should "pickup" bottom to a rung if bottom grows too
                // much...
                insertBottom(event);
            }
        }
    }

    public TimeEvent popFirst() {
        if (numBottom > 0) {
            TimeEvent retVal = firstBottom;
            firstBottom = firstBottom.nextEvent;
            numBottom--;
            lastPopTime = retVal.time;
            retVal.nextEvent = null;
            retVal.prevEvent = null;
            return retVal;
        } else if (numRung > 0) {
            moveBucket();
            if (numBottom > 0)
                return popFirst();
        } else if (numTop > 0) {
            // Set first runge!
            moveTop();
            moveBucket();
            if (numBottom > 0)
                return popFirst();
        }

        return null;
    }

    private void moveBucket() {
        if (DEBUG) {
            System.out.println("Move Bucket, rung: " + numRung + " evt left: " + currentRung.numTotal);
        }
        int bucket = findBucket();

        TimeEvent evt = currentRung.bucketFirst[bucket];
        currentRung.bucketFirst[bucket] = currentRung.bucketLast[bucket] = null;
        currentRung.numTotal -= currentRung.numBucket[bucket];
        currentRung.numBucket[bucket] = 0;
        // Also move the "cursor" for the current rung one step forward!
        currentRung.bucketStartTime += currentRung.bucketWidth;
        TimeEvent next = null;
        while (evt != null) {
            // Remember the next pointer
            next = evt.nextEvent;
            // since insert modifies next pointeer!
            insertBottom(evt);
            evt = next;
        }

        while (currentRung != null && currentRung.numTotal == 0) {
            numRung--;
            if (DEBUG)
                System.out.println("Removing rung! rung = " + numRung);
            if (numRung == 0) {
                currentRung = null;
            } else {
                currentRung = rungs[numRung - 1];
            }
        }
    }

    private void insertBottom(TimeEvent event) {
        if (DEBUG)
            System.out.println("insertBottom: " + event.time);
        long time = event.time;
        if (numBottom == 0) {
            firstBottom = event;
            // Unlink eventual next elements...
            event.nextEvent = null;
        } else {
            TimeEvent evt = firstBottom;
            TimeEvent last = null;
            // Go on until evt.time is too large => insert into
            // after last
            while (evt != null && evt.time < time) {
                last = evt;
                evt = evt.nextEvent;
            }
            // Insert here!!!
            // No last => insert before since time is less than first!
            if (last == null) {
                if (DEBUG)
                    System.out.println("Insert before start!");
                last = firstBottom;
                firstBottom = event;
                event.nextEvent = last;
            } else {
                if (DEBUG) {
                    if (evt != null) {
                        System.out.println("Insert between " + last.time + " and " + evt.time);
                    } else {
                        System.out.println("Inserting last, after: " + last.time);
                    }
                }
                last.nextEvent = event;
                event.nextEvent = evt;
            }
        }
        numBottom++;
    }

    private int findBucket() {
        if (DEBUG)
            System.out.println("findBucket, rung: " + numRung);
        int bucketIndex = 0;
        currentRung.bucketStartTime = currentRung.startTime;
        while (currentRung.numBucket[bucketIndex] == 0) {
            bucketIndex++;
            currentRung.bucketStartTime += currentRung.bucketWidth;
        }
        // Did not find any non-empty here!!!???
        if (bucketIndex == BUCKET_NUMBER)
            return -1;
        if (currentRung.bucketWidth > 1 && currentRung.numBucket[bucketIndex] > SPAWN_THRESHOLD) {
            // create a new rung
            if (DEBUG)
                System.out.println("findBucket, too many events in bucket, creating new rung: "
                        + currentRung.numBucket[bucketIndex]);
            createRung(bucketIndex);
            return findBucket();
        }
        if (DEBUG)
            System.out.println("   bucket found: " + bucketIndex + " startTime: " + currentRung.bucketStartTime);

        return bucketIndex;
    }

    private void createRung(int bucketIndex) {
        TimeEvent newRungStart = currentRung.bucketFirst[bucketIndex];
        currentRung.bucketFirst[bucketIndex] = currentRung.bucketLast[bucketIndex] = null;
        currentRung.numTotal -= currentRung.numBucket[bucketIndex];
        currentRung.numBucket[bucketIndex] = 0;
        // Increase rung before the "call"?!
        Rung oldCurrent = currentRung;
        createRung(newRungStart, currentRung.bucketStartTime, 1 + (currentRung.bucketWidth / BUCKET_NUMBER));
        // After a new rung have been spawned, this rungs current bucket
        // start time needs to be updated!
        oldCurrent.bucketStartTime += oldCurrent.bucketWidth;
    }

    private void createRung(TimeEvent first, long rs, int bw) {
        numRung++;
        if (DEBUG)
            System.out.println("Creating RUNG: " + numRung + " start: " + rs + " bucket width: " + bw);
        TimeEvent last;
        // If there are not enough rungs in the array list add one more!
        if (rungCount < numRung) {
            if (DEBUG)
                System.out.println("Creating new rung!");
            if (rungCount == rungs.length) {
                // Same capacity increase as in ArrayList
                int newCapacity = (rungCount * 3) / 2 + 1;
                Rung[] tmp = new Rung[newCapacity];
                System.arraycopy(rungs, 0, tmp, 0, rungCount);
                rungs = tmp;
            }
            rungs[rungCount++] = new Rung(BUCKET_NUMBER);
        }
        // Pick up the "numRung - 1" as the current rung!
        currentRung = rungs[numRung - 1];
        currentRung.startTime = rs;
        currentRung.bucketWidth = bw;
        while (first != null) {
            int bucketIndex = (int) ((first.time - rs) / bw);
            if (DEBUG)
                System.out.println("Adding: " + first.time + " to " + bucketIndex);
            if (currentRung.bucketFirst[bucketIndex] == null) {
                currentRung.bucketFirst[bucketIndex] = currentRung.bucketLast[bucketIndex] = first;
            } else {
                // Add to last elements next pointer, and set last!
                currentRung.bucketLast[bucketIndex].nextEvent = first;
                currentRung.bucketLast[bucketIndex] = first;
            }
            last = first;
            // Step one step forward!
            first = first.nextEvent;
            // Unlink... the newly added element!
            last.nextEvent = null;
            currentRung.numBucket[bucketIndex]++;
            currentRung.numTotal++;
        }
    }

    // Moves top to the first runge!
    private void moveTop() {
        int bw = 1 + (int) ((maxTS - minTS) / BUCKET_NUMBER);
        if (DEBUG)
            System.out.println("Moving top: " + minTS + ", " + maxTS + " => " + bw);
        topStart = maxTS;
        numTop = 0;
        createRung(topFirst, minTS, bw);
        topFirst = null;
    }

} // EventQueue
