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


    /* Calculate distance between two positions */
    public double getDistance(Position p2) {
        double dx = x - p2.x;
        double dy = y - p2.y;
        double dz = z - p2.z;
        dx = dx * dx;
        dy = dy * dy;
        dz = dz * dz;
        return Math.sqrt(dx + dy + dz);
    }
}