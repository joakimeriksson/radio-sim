/**
 * @(#)ALEventQueue Created date: Mon Apr 23 17:24:12 2007
 * $Revision: 1.2 $, $Date: 2007/04/24 10:48:40 $
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

import java.util.ArrayList;

/**
 *
 * @author Joakim Eriksson (joakim.eriksson@botbox.com)
 * @author Niclas Finne (niclas.finne@botbox.com)
 * @author Sverker Janson (sverker.janson@botbox.com)
 * @version $Revision: 1.2 $, $Date: 2007/04/24 10:48:40 $
 */
public class ALEventQueue {

    private ArrayList<TimeEvent> queue = new ArrayList<TimeEvent>();

    public ALEventQueue() {
    }

    public void addEvent(TimeEvent event) {
        int i = 0;
        for (int n = queue.size(); i < n && event.time > queue.get(i).time; i++)
            ;
        // Add this at the specified index!
        queue.add(i, event);
    }

    public TimeEvent popFirst() {
        if (queue.size() > 0)
            return queue.remove(0);
        return null;
    }

} // ALEventQueue
