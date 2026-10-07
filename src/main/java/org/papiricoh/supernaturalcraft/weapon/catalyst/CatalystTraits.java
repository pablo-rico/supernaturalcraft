package org.papiricoh.supernaturalcraft.weapon.catalyst;

/**
 * How a catalyst bends the forms of the spells cast through it.
 *
 * @param boltSplit        Bolt fires this many sigils in a fan (1 = normal)
 * @param boltPierce       Bolt passes through up to this many entities
 * @param boltBurstArea    extra splash radius when a Bolt lands
 * @param burstLingerTicks Burst leaves a zone that keeps working this long
 * @param wardScale        Ward radius and duration multiplier
 * @param touchChain       Touch jumps to this many more targets nearby
 */
public record CatalystTraits(int boltSplit, int boltPierce, float boltBurstArea, int burstLingerTicks, float wardScale, int touchChain) {

    public static final CatalystTraits NONE = new CatalystTraits(1, 0, 0f, 0, 1f, 0);

    public CatalystTraits withBoltSplit(int n) {
        return new CatalystTraits(n, boltPierce, boltBurstArea, burstLingerTicks, wardScale, touchChain);
    }

    public CatalystTraits withBoltPierce(int n) {
        return new CatalystTraits(boltSplit, n, boltBurstArea, burstLingerTicks, wardScale, touchChain);
    }

    public CatalystTraits withBoltBurstArea(float a) {
        return new CatalystTraits(boltSplit, boltPierce, a, burstLingerTicks, wardScale, touchChain);
    }

    public CatalystTraits withBurstLinger(int ticks) {
        return new CatalystTraits(boltSplit, boltPierce, boltBurstArea, ticks, wardScale, touchChain);
    }

    public CatalystTraits withWardScale(float s) {
        return new CatalystTraits(boltSplit, boltPierce, boltBurstArea, burstLingerTicks, s, touchChain);
    }

    public CatalystTraits withTouchChain(int n) {
        return new CatalystTraits(boltSplit, boltPierce, boltBurstArea, burstLingerTicks, wardScale, n);
    }
}
