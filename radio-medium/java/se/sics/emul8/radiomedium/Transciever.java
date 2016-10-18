package se.sics.emul8.radiomedium;

public class Transciever {

    public final static int LISTENING    = 0;
    public final static int TRANSMITTING = 1;
    public final static int RECEIVING    = 2;
    public final static int DISABLED     = 3;

    private final Node node;
    private double txpower = 0.0;
    private int wirelessChannel = 26;
    private boolean isEnabled = true;
    private RadioPacket receivingPacket;
    private RadioPacket sendingPacket;
    private double receivingRSSI;
    private double rxProbability = 1.0;
    private double txProbability = 1.0;

    public Transciever(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public double getTransmitPower() {
        return this.txpower;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled  = isEnabled;
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

    public double getRSSI() {
        if (isReceiving()) {
            return this.receivingRSSI;
        }
        RadioMedium medium = this.node.getRadioMedium();
        if (medium != null) {
            return medium.getBaseRSSI(this.node);
        }
        return -99.99;
    }

    public boolean isReceiving() {
        return this.receivingPacket != null;
    }

    public int getReceivingState() {
        if (!isEnabled) {
            return DISABLED;
        }
        if (this.receivingPacket != null) {
            return RECEIVING;
        }
        if (this.sendingPacket != null) {
            return TRANSMITTING;
        }
        return LISTENING;
    }

    public void setReceiving(RadioPacket packet, double rssi) {
        this.clearSending();
        this.receivingPacket = packet;
        this.receivingRSSI = rssi;
    }

    public void clearReceiving() {
        this.receivingPacket = null;
    }

    public double getRxProbability() {
        return this.rxProbability;
    }

    public void setRxProbability(double probability) {
        this.rxProbability = probability;
    }

    public double getTxProbability() {
        return this.txProbability;
    }

    public void setTxProbability(double probability) {
        this.txProbability = probability;
    }

    public void setSending(RadioPacket packet) {
        this.clearReceiving();
        this.sendingPacket = packet;
    }

    public void clearSending() {
        this.sendingPacket = null;
    }
}
