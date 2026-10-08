package org.papiricoh.supernaturalcraft.crossroads;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Extension points other parts of the mod fill in, so the crossroads does not depend on them.
 */
public final class CrossroadsHooks {

    private CrossroadsHooks() {
    }

    /**
     * Bringing back a dead pet as the "Recover what was lost" wish (variant 1).
     *
     * <p><b>HOOK (wave 2, spells agent):</b> assign {@link #petRevival} from the pet ledger code
     * (e.g. during common setup). Until then the demon never offers it.
     */
    public interface PetRevival {
        /** Whether {@code player} has a dead pet the demon could bring back. */
        boolean available(ServerPlayer player);

        /** Brings it back near {@code at}; @return false if there was nothing to revive after all. */
        boolean revive(ServerPlayer player, Vec3 at);
    }

    /** No pets can be revived: the default until the pet ledger plugs itself in. */
    public static final PetRevival NO_PETS = new PetRevival() {
        @Override
        public boolean available(ServerPlayer player) {
            return false;
        }

        @Override
        public boolean revive(ServerPlayer player, Vec3 at) {
            return false;
        }
    };

    public static volatile PetRevival petRevival = NO_PETS;

    /**
     * Selling the soul for good (v0.13): "Make me one of you" and "Bind my soul". The allegiance plugs itself in
     * ({@code allegiance.AllegianceCrossroads}); until then neither is offered.
     */
    public interface Soul {
        /** Whether {@code player} is free to give their soul to Hell (a human not on a cure's cooldown). */
        boolean mayConvert(ServerPlayer player);

        /** "Make me one of you": a demon now. @return false if it could not happen */
        boolean convert(ServerPlayer player);

        /** The soul was bound when the deal was sealed. */
        void bound(ServerPlayer player);

        /** The hounds took a bound soul. @return true if it will rise a demon (no other penalty then) */
        boolean collected(ServerPlayer player);
    }

    public static final Soul NO_SOUL = new Soul() {
        @Override
        public boolean mayConvert(ServerPlayer player) {
            return false;
        }

        @Override
        public boolean convert(ServerPlayer player) {
            return false;
        }

        @Override
        public void bound(ServerPlayer player) {
        }

        @Override
        public boolean collected(ServerPlayer player) {
            return false;
        }
    };

    public static volatile Soul soul = NO_SOUL;
}
