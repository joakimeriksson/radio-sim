package se.sics.emul8.radiomedium;

public abstract class AbstractRadioMedium implements RadioMedium {

    protected Simulator simulator;

    @Override
    public void setSimulator(Simulator sim) {
        this.simulator = sim;
    }

}
