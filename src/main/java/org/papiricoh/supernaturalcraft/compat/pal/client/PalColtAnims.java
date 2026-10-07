package org.papiricoh.supernaturalcraft.compat.pal.client;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.colt.ColtPlayerAnims;

/** Plays the whole-body Colt animations (assets/supernaturalcraft/player_animations/colt.json). */
final class PalColtAnims implements ColtPlayerAnims {

    private static void play(Player player, String name) {
        if (!(player instanceof AbstractClientPlayer p)) return;
        if (PlayerAnimationAccess.getPlayerAnimationLayer(p, PalClient.LAYER) instanceof PlayerAnimationController c) {
            c.triggerAnimation(SupernaturalCraft.asResource(name));
        }
    }

    private static void stop(Player player) {
        if (player instanceof AbstractClientPlayer p
                && PlayerAnimationAccess.getPlayerAnimationLayer(p, PalClient.LAYER) instanceof PlayerAnimationController c) {
            c.stopTriggeredAnimation();
        }
    }

    /** A shot ends whatever was playing: the gun arm then levels along the gaze (ColtArmPoses). */
    @Override
    public void fire(Player player) {
        stop(player);
    }

    @Override
    public void dryFire(Player player) {
        play(player, "colt_dry");
    }

    @Override
    public void reload(Player player, int rounds) {
        play(player, "colt_reload_" + rounds);
    }

    @Override
    public void abort(Player player) {
        stop(player);
    }

    @Override
    public void inspect(Player player) {
        play(player, "colt_inspect");
    }

    @Override
    public boolean active(Player player) {
        return player instanceof AbstractClientPlayer p
                && PlayerAnimationAccess.getPlayerAnimationLayer(p, PalClient.LAYER) instanceof PlayerAnimationController c && c.isActive();
    }
}
