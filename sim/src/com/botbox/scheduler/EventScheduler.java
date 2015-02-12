/**
 * @(#)EventScheduler Created date: Sun Apr 22 11:37:05 2007
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
public class EventScheduler {

    private EventQueue eventQueue;

    public EventScheduler() {
        eventQueue = new EventQueue();
    }

    public void addEvent(TimeEvent event) {
    }

    public void removeEvent(TimeEvent event) {
    }

} // EventScheduler
