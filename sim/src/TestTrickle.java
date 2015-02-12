import java.util.ArrayList;
import com.botbox.scheduler.EventQueue;
import com.botbox.scheduler.TimeEvent;

/* Trickle timer tester
 * - two different implementation just to see if the simplified trickle
 *  suffers from any problems compared to the normal
 *
 * tests todo:
 * 1. test if it is possible to get good performance and loadbalancing with
 *    the new and the old mixed. [works perfect]
 * 2. test the case that a node is out of phase in regular trickle
 *
 *
 * TrickleTimer - new trickle timer
 * TrickleTimer2 - old version (a la RPL)
 */

public class TestTrickle {
    private static int NODE_COUNT = 10;
    private static int START = 12;
    private static int DOUBLE = 0;

    private static final double LOSS_LEVEL = 0.00;
    private static final int PACKET_LIMIT = 1;
    private static final int NO_SEND_LIMIT = 10;
    public static final boolean COMPENSATE_SEND = false; // true;//false;
    public static final boolean COMPENSATE_NOSEND = false;

    private static boolean EXTRA_NODE = false;
    private static int RANDOMNESS = 0;

    EventQueue eq = new EventQueue();

    int totPackets = 0;

    private long MAX_TIME = 10000000;

    public interface Stat {
        public int getTriggered();

        public int sentPackets();

        public void printStat();
    }

    static long lastPacketTime;

    public class TrickleTimer extends TimeEvent implements Stat {
        int start = 0;
        int dbl = 0;
        int currval = 0;
        int noSend = 0;

        /* packets when last triggered */
        int lastPackets;
        int mySentPackets;
        int triggered;

        public TrickleTimer(long currentTime, int start, int dbl) {
            super(currentTime);
            this.start = start;
            this.dbl = dbl;
            currval = start;
        }

        long lastRest;
        long add = 0;

        public void execute(long currentTime) {
            if (currval < start + dbl)
                currval++;
            long interval = 1L << currval;
            /* t = [0 - I] */
            long randTime = (long) (interval / 1 * Math.random());
            if (COMPENSATE_SEND) {
                randTime += add;
                add = add / 2;
            }
            // /* how will this affect things such as propagation speed ? */
            // if (randTime + lastRest < interval/2) {
            // randTime += interval/2;
            // }

            int loss = Math.random() > LOSS_LEVEL ? 1 : 0;
            if (lastPackets + PACKET_LIMIT - loss >= totPackets) {
                totPackets++;
                mySentPackets++;
                /* a full interval after sending a packet ? */
                lastPacketTime = time;
                add = interval / 2;
                noSend = 0;
            } else {
                noSend++;
                /* gotten all packets already */
                if (COMPENSATE_NOSEND) {
                    /*
                     * This will cause a resynch so that this node gets to
                     * send...
                     */
                    if (noSend > NO_SEND_LIMIT) {
                        lastRest = lastRest - lastRest / 64;
                    }
                }
            }

            this.time = currentTime + randTime + lastRest;
            lastRest = interval - randTime;
            // System.out.println("Last rest:" + lastRest);
            // System.out.println("Adding timer at: " + this.time + " now: " +
            // currentTime);
            eq.addEvent(this);

            /* reset redundancy counter */
            lastPackets = totPackets;

            /* but if the last packets are less than an interval away... */
            /* this will allow us to remember this packet for the next interval */
            if (noSend > 0 && (this.time - lastPacketTime) <= interval) {
                // System.out.println("***Remembering a packet back in time...");
                lastPackets--;
            }

            triggered++;
        }

        public int sentPackets() {
            return mySentPackets;
        }

        public void printStat() {
            System.out.println("Triggered times: " + triggered);
            System.out.println("Sent packets   : " + mySentPackets);
        }

        @Override
        public int getTriggered() {
            return triggered;
        }
    }

    public class TrickleTimer2 extends TimeEvent implements Stat {

        int start = 0;
        int dbl = 0;
        int currval = 0;

        /* packets when last triggered */
        int lastPackets;
        int mySentPackets;
        int triggered;

        TimeEvent interval = new TimeEvent(0) {
            public void execute(long currentTime) {
                /* at the interval we calculate the next trickle */
                if (currval < start + dbl)
                    currval++;
                long randTime = (long) (((1L << currval) * 0.5 + (0.5 * ((1L << currval) * Math.random()))));
                // long randTime = ((long)((1L << currval) * Math.random()));
                TrickleTimer2.this.time = currentTime + randTime;
                // System.out.println("Adding timer at: " + this.time + " now: "
                // + currentTime);
                eq.addEvent(TrickleTimer2.this);
                interval.setTime(currentTime + (1L << currval));
                eq.addEvent(interval);
                /* reset C */
                lastPackets = totPackets;
                triggered++;
            }
        };

        public TrickleTimer2(long currTime, int start, int dbl) {
            super(currTime);
            this.start = start;
            this.dbl = dbl;
            currval = start;
            interval.setTime(currTime + (1L << currval));
            eq.addEvent(interval);
        }

        /* send packet and resync */
        public void execute(long currentTime) {
            int loss = Math.random() > LOSS_LEVEL ? 1 : 0;
            if (lastPackets + PACKET_LIMIT - loss >= totPackets) {
                totPackets++;
                mySentPackets++;
            }
        }

        public int sentPackets() {
            return mySentPackets;
        }

        public void printStat() {
            System.out.println("Triggered times: " + triggered);
            System.out.println("Sent packets   : " + mySentPackets);
        }

        @Override
        public int getTriggered() {
            return triggered;
        }
    }

    public int test(int phase) {
        // System.out.println("=========================================");
        long time = 0;
        ArrayList<Stat> nodes = new ArrayList<Stat>();
        for (int i = 0, n = NODE_COUNT; i < n; i++) {
            TrickleTimer tt = new TrickleTimer((long) (Math.random() * RANDOMNESS), START, DOUBLE);
            // TrickleTimer2 tt = new TrickleTimer2((long) (Math.random()*
            // RANDOMNESS), START, DOUBLE);
            eq.addEvent(tt);
            nodes.add(tt);
        }
        if (EXTRA_NODE) {
            TrickleTimer tt = new TrickleTimer(phase, START, DOUBLE);
            // TrickleTimer2 tt = new TrickleTimer2(phase, START, DOUBLE);
            eq.addEvent(tt);
            nodes.add(tt);
        }
        TimeEvent te;
        while ((te = eq.popFirst()) != null && (time < MAX_TIME)) {
            time = te.getTime();
            te.execute(time);
        }

        // System.out.println("Sent packets in total: " + totPackets);
        // for (int i = 0; i < nodes.size(); i++) {
        // System.out.println("\nNode " + i+ ":");
        // nodes.get(i).printStat();
        // }
        // System.out.println("Phase: " + phase + " packets: " + totPackets);
        if (nodes.size() == 1)
            nodes.add(new TrickleTimer(0, 0, 0));
        System.out.println(" " + phase + ", " + nodes.get(nodes.size() - 1).sentPackets() + ", "
                + nodes.get(nodes.size() - 2).sentPackets() + ", " + totPackets + ", "
                + nodes.get(nodes.size() - 1).getTriggered() + ", " + nodes.get(nodes.size() - 2).getTriggered() + ", "
                + nodes.size());

        // nodes.get(nodes.size() - 1).printStat();
        return totPackets;
    }

    public static void main(String[] args) {
        int totPackets = 0;

        /* phase test ... */
        if (true) {
            EXTRA_NODE = true;
            for (int i = 0; i < (1L << START); i += 10) {
                TestTrickle tt = new TestTrickle();
                totPackets += tt.test(i);
            }
        }
        /* scalability test */
        if (false) {
            EXTRA_NODE = false;
            NODE_COUNT = 1;
            RANDOMNESS = 4090;
            for (int i = 0; i < 10; i += 1) {
                TestTrickle tt = new TestTrickle();
                totPackets += tt.test(0);
                NODE_COUNT = NODE_COUNT * 2;
            }

        }
        System.out.println("Total packets sent: " + totPackets);

    }
}
