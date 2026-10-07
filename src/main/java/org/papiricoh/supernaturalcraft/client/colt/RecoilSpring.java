package org.papiricoh.supernaturalcraft.client.colt;

/**
 * A damped spring that rests at zero: kicked, it overshoots once and settles. Integrated in small
 * fixed substeps so it behaves the same at any frame rate. Pure, for the unit tests.
 */
public final class RecoilSpring {

    private static final double SUBSTEP = 1 / 240.0, MAX_DT = 0.25;
    private final double stiffness, damping, peakPerVelocity;
    private double pos, vel;

    /** @param stiffness k, per second squared  @param dampingRatio ζ, below 1 to overshoot */
    public RecoilSpring(double stiffness, double dampingRatio) {
        this.stiffness = stiffness;
        double omega = Math.sqrt(stiffness);
        this.damping = 2 * dampingRatio * omega;
        // Peak displacement of an underdamped spring released from rest with unit velocity.
        double omegaD = omega * Math.sqrt(1 - dampingRatio * dampingRatio);
        double tPeak = Math.atan2(omegaD, dampingRatio * omega) / omegaD;
        this.peakPerVelocity = Math.exp(-dampingRatio * omega * tPeak) * Math.sin(omegaD * tPeak) / omegaD;
    }

    /** Throws the spring so that it peaks at about {@code amount} from wherever it is. */
    public void kick(double amount) {
        vel += amount / peakPerVelocity;
    }

    public void step(double dt) {
        if (!(dt > 0)) return;
        dt = Math.min(dt, MAX_DT);
        int n = (int) Math.ceil(dt / SUBSTEP);
        double h = dt / n;
        for (int i = 0; i < n; i++) {
            vel += (-stiffness * pos - damping * vel) * h;
            pos += vel * h;
        }
        if (Math.abs(pos) < 1e-5 && Math.abs(vel) < 1e-4) {
            pos = 0;
            vel = 0;
        }
    }

    public double value() {
        return pos;
    }

    public boolean atRest() {
        return pos == 0 && vel == 0;
    }

    public void reset() {
        pos = 0;
        vel = 0;
    }
}
