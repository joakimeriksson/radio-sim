package se.sics.emul8.radiomedium;

import javax.xml.bind.DatatypeConverter;
import com.eclipsesource.json.JsonObject;

public class RadioPacket {

    private Node node;
    private long time;
    private double txpower;
    private int channel;
    private String packetData;

    public RadioPacket(Node node, long time, String packetData) {
        this.node = node;
        this.time = time;
        this.txpower = node.getTransmitPower();
        this.channel = node.getWirelessChannel();
        this.packetData = packetData;
    }

    public Node getSource() {
        return this.node;
    }

    public long getTime() {
        return this.time;
    }

    public double getTransmitPower() {
        return this.txpower;
    }

    public void setTransmitPower(double txpower) {
        this.txpower = txpower;
    }

    public int getWirelessChannel() {
        return this.channel;
    }

    public void setWirelessChannel(int channel) {
        this.channel = channel;
    }

    public String getPacketDataAsHex() {
        return packetData;
    }
    
    public byte[] getPacketDataAsBytes() {
        return DatatypeConverter.parseHexBinary(packetData);
    }

    public JsonObject toJsonDestination(Node destination, double rssi) {
        JsonObject json = new JsonObject();
        json.add("command", "receive");
        json.add("destination-node-id", destination.getId());
        json.add("time", this.time);
        json.add("receive-strength", rssi);
        json.add("wireless-channel", this.channel);
        json.add("packet-data", this.packetData);
        return json;
    }
}
