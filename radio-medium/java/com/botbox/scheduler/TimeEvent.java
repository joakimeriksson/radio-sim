/**
 * @(#)TimeEvent Created date: Sun Apr 22 11:32:42 2007
 * $Revision: 1.2 $, $Date: 2007/12/14 17:18:02 $
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
 * @version $Revision: 1.2 $, $Date: 2007/12/14 17:18:02 $
 */
public abstract class TimeEvent {

    // For linking events...
    TimeEvent nextEvent;
    TimeEvent prevEvent;

    protected long time;

    public TimeEvent(long time) {
        this.time = time;
    }

    public final long getTime() {
        return time;
    }

    public void setTime(long t) {
        time = t;
    }

    public abstract void execute(long currentTime);

} // TimeEvent
