package se.sics.emul8.radiomedium;
import se.sics.emul8.radiomedium.net.ClientConnection;

public class Node {

    private final String id;
    private final Position pos = new Position();
    private double txpower = 0.0;
    private int wirelessChannel = 26;
    private ClientConnection clientConnection;

    public Node(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public Position getPosition() {
        return pos;
    }

    public ClientConnection getClientConnection() {
        return this.clientConnection;
    }

    public void setClientConnection(ClientConnection cc) {
        this.clientConnection = cc;
    }

    public double getTransmitPower() {
        return this.txpower;
    }

    public void setTransmitPower(double txpower) {
        this.txpower = txpower;
    }

    public int getWirelessChannel() {
        return wirelessChannel;
    }

    public void setWirelessChannel(int wirelessChannel) {
        this.wirelessChannel = wirelessChannel;
    }

}
