package se.sics.emul8.radiomedium;

public interface RadioMedium {

    public String getName();
    
    public void setSimulator(Simulator sim);

    public void transmit(RadioPacket packet);

}
