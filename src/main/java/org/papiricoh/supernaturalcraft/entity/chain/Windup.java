package org.papiricoh.supernaturalcraft.entity.chain;

/** A blow that lands a few ticks after its clip starts (the clip's hit tick in {@link ChainMobAssets}). Server side. */
final class Windup {
    private int ticks;
    private Runnable hit;

    void start(int ticks, Runnable hit) {
        this.ticks = Math.max(1, ticks);
        this.hit = hit;
    }

    boolean busy() {
        return hit != null;
    }

    void cancel() {
        hit = null;
    }

    void tick() {
        if (hit != null && --ticks <= 0) {
            Runnable r = hit;
            hit = null;
            r.run();
        }
    }
}
