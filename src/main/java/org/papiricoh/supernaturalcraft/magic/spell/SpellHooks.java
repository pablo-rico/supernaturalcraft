package org.papiricoh.supernaturalcraft.magic.spell;

/** Entities that react to specific sigils in their own way (the boss and his illusions). */
public final class SpellHooks {

    private SpellHooks() {
    }

    /** Overrides what Bind does. Return true if the bind took hold. */
    public interface Bindable {
        boolean onBound(int durationTicks);
    }

    /** Called when Reveal touches the entity. */
    public interface Revealable {
        void onRevealed();
    }

    /** Overrides what Exorcise does. Return true if it had any effect. */
    public interface Exorcisable {
        boolean onExorcised(float potency);
    }

    /**
     * A demon that can be forced out of the body it rides (v0.21: Abaddon in a knight) by the Exorcise sigil, Banish by Name or
     * a Salt Shot. Asked first, before the usual effect. Return true if it was forced out (the usual effect is then skipped).
     */
    public interface Expellable {
        int EXORCISE = 0, NAME = 1, SALT = 2;

        boolean onExpelled(@org.jetbrains.annotations.Nullable net.minecraft.world.entity.Entity by, int how);
    }
}
