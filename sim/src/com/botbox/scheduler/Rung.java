/**
 * @(#)Rung Created date: Mon Apr 23 00:27:49 2007
 * $Revision: 1.1 $, $Date: 2007/04/24 06:36:46 $
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
 *
 * @author Joakim Eriksson (joakim.eriksson@botbox.com)
 * @author Niclas Finne (niclas.finne@botbox.com)
 * @author Sverker Janson (sverker.janson@botbox.com)
 * @version $Revision: 1.1 $, $Date: 2007/04/24 06:36:46 $
 */
public class Rung {
    // Bucket width in Rung - this should probably be an object - rung!
    // where subsequent dequeeing will be performed
    int size;
    int bucketWidth;

    // current start time of current bucket
    long bucketStartTime;
    // starting timestamp of the first bucket in the rung
    long startTime;

    // number of events in a bucket in a rung...
    int[] numBucket;
    int numTotal;

    // first event of each bucket...
    TimeEvent[] bucketFirst;
    TimeEvent[] bucketLast;

    public Rung(int buckets) {
        size = buckets;
        numBucket = new int[size];
        bucketFirst = new TimeEvent[size];
        bucketLast = new TimeEvent[size];
    }

} // Rung
