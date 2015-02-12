package se.sics.sim.net;

import se.sics.sim.core.*;
import com.botbox.scheduler.*;
import se.sics.sim.rl.*;

/**
 * Describe class BCTrigger here.
 *
 *
 * Created: Wed Apr 25 17:45:35 2007
 *
 * @author <a href="mailto:joakim@RODING"></a>
 * @version 1.0
 */
public class BCTrigger extends TimeEvent {

    private int count;
    private int msgType;
    private int id;
    private Simulator sim;
    private MessageNode node;
    private EpisodeManager episodeManager;

    public BCTrigger(Simulator sim, long time, MessageNode node, int msgType, int count) {
        super(time);
        this.count = count;
        this.msgType = msgType;
        this.sim = sim;
        this.node = node;
        this.id = 0;
        episodeManager = EpisodeManager.getDefault();
    }

    public void execute(long time) {
        // Call episode manager after last episode
        if (id > 0 && episodeManager != null) {
            episodeManager.nextEpisode();
        }

        if (MessageNode.DEBUG)
            System.out.println("------------------------------------");
        count--;
        node.sendBroadcast(time, msgType, ++id);

        this.time += 100;
        if (count > 0) {
            // Since it is recently executed add this event again!
            sim.addEvent(this);
        }
    }

}
