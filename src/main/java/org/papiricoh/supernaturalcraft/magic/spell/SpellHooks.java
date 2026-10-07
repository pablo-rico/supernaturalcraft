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
}
