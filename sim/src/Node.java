/**
 * Describe class Node here.
 *
 *
 * Created: Thu Apr 19 17:23:48 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class Node {

    static int packetCtr = 0;
    Node[] neighbours;

    boolean requestSend = true;
    boolean endNode = false;

    int id;
    int received;

    /**
     * Creates a new <code>Node</code> instance.
     *
     */
    public Node(int id) {
        this.id = id;
    }

    public void setNeigbours(Node[] n) {
        neighbours = n;
    }

    public double errorRate(Node node) {
        return 0.11;
    }

    // All protocols sends on the broadcast/flood message
    public void receive(Node node, int id) {
        if (id != received) {
            requestSend = true;
            received = id;
        }
    }

    public void newMsg() {
        requestSend = true;
        received++;
    }

    public boolean tick() {
        if (requestSend) {
            requestSend = false;
            send();
            return true;
        }
        return false;
    }

    public void send() {
        packetCtr++;
        for (int i = 0, n = neighbours.length; i < n; i++) {
            if (neighbours[i].errorRate(this) < Math.random()) {
                // System.out.println("Node " + packetId + " => " +
                // neighbours[i].id);
                neighbours[i].receive(this, received);
            } else {
                // System.out.println("Node " + packetId + " <###> " +
                // neighbours[i].id);
            }
        }
    }
}
