package se.sics.sim.interfaces;

public interface RadioModes {

    public static final int MODE_OFF = 0;
    public static final int MODE_LISTEN = 1;
    public static final int MODE_TRANSMIT = 2;
    public static final int MODE_MAX = MODE_TRANSMIT;
    public static final String MODE_NAME[] = new String[] { "Off", "Listen", "Transmit" };

}
