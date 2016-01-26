package se.sics.emul8.radiomedium;

public class Transciever {
    
    private double txpower = 0.0;
    private int wirelessChannel = 26;
    private boolean isEnabled = true;

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
}
