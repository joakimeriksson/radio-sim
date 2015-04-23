package se.sics.emul8.radiomedium;

public class Position {

    public double x;
    public double y;
    public double z;

    public Position() {
    }

    public void set(double x, double y) {
        set(x, y, 0.0);
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
