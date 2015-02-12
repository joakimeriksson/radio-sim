package se.sics.sim.util;

/**
 * Describe class Visualizer here.
 *
 *
 * Created: Fri May 11 12:50:07 2007
 *
 * @author <a href="mailto:joakime@GRAYLING"></a>
 * @version 1.0
 */
public abstract class Visualizer {

    // Maybe we should have Node objects instead of ID's here!!!!

    public abstract void addNode(int node);

    public abstract void setNodeProperty(int node, String name, String data);

    public abstract void setLocation(int node, int x, int y);

    public abstract void addLink(int node1, int node2);

    public abstract void setLinkProperty(int sourceNode, int destinationNode, String name, String data);

    public abstract void messageSent(int node);

    public abstract void messageReceived(int node);

    public abstract void close();

}
