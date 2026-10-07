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
}
