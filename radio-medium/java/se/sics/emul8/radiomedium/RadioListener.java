package se.sics.emul8.radiomedium;

public interface RadioListener {
    /* The radio listener will receive all packets transmitted during a simulation */
    public void packetTransmission(RadioPacket packet);
}
