package se.sics.sim.net;

import se.sics.sim.core.*;
import com.botbox.scheduler.*;

/**
 * Describe class MessageEvent here.
 *
 *
 * Created: Tue Apr 24 08:27:00 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class MessageEvent extends TimeEvent implements Message {

    private Link dst;
    private MessageNode src;
    private int id;
    private int type;

    /**
     * Creates a new <code>MessageEvent</code> instance.
     *
     */
    public MessageEvent(MessageNode src, Link dst, long time, int id) {
        this(src, dst, time, 0, id);
    }

    public MessageEvent(MessageNode src, Link dst, long time, int type, int id) {
        super(time);
        this.src = src;
        this.dst = dst;
        this.id = id;
        this.type = type;
    }

    public Node getSource() {
        return src;
    }

    public int getID() {
        return id;
    }

    public int getType() {
        return type;
    }

    public void execute(long time) {
        src.bytesSent += MessageNode.MESSAGE_SIZE[type];
        dst.destination.messageReceived(time, this);
    }

    @Override
    public int getContentType() {
        // TODO Auto-generated method stub
        return 0;
    }

}
