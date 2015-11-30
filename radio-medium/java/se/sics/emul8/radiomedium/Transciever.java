package se.sics.emul8.radiomedium;

public class Transciever {
    
    private double txpower = 0.0;
    private int wirelessChannel = 26;

    
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
