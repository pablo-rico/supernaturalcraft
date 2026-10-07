package org.papiricoh.supernaturalcraft.client.colt;

import net.minecraft.world.entity.player.Player;

/**
 * Whole-body animations of a player handling the Colt. Without an animation library this does
 * nothing and the arm poses ({@link ColtArmPoses}) carry the third-person view; with
 * PlayerAnimationLib installed, compat/pal swaps in an implementation that plays real ones.
 */
public interface ColtPlayerAnims {

    ColtPlayerAnims NONE = new ColtPlayerAnims() {
    };

    default void fire(Player player) {
    }

    default void dryFire(Player player) {
    }

    default void reload(Player player, int rounds) {
    }

    default void abort(Player player) {
    }

    default void inspect(Player player) {
    }

    /** True while a full-body animation is playing on {@code player}, so the fallback arm poses step aside. */
    default boolean active(Player player) {
        return false;
    }

    static ColtPlayerAnims get() {
        return Holder.current;
    }

    static void install(ColtPlayerAnims anims) {
        Holder.current = anims;
    }

    final class Holder {
        private static ColtPlayerAnims current = NONE;

        private Holder() {
        }
    }
}
