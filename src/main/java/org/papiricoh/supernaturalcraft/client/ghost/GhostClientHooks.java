package org.papiricoh.supernaturalcraft.client.ghost;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostBalance;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/**
 * Client side of a ghost: whether the local player sees it (and its outline), how much of it shows,
 * and the breath that mists in the cold it brings. Only ever called when the level is client-side
 * on a physical client.
 */
public final class GhostClientHooks {

    private GhostClientHooks() {
    }

    /** Whether the local player has Second Sight (or watches as a spectator). */
    public static boolean secondSight() {
        Player p = Minecraft.getInstance().player;
        return p != null && (p.hasEffect(AllMobEffects.SECOND_SIGHT) || p.isSpectator());
    }

    /** Its outline glows only for a local player with Second Sight. */
    public static boolean glows(GhostEntity g) {
        return !g.isInert() && secondSight();
    }

    /** How much of it should show right now (0 = unseen). */
    public static float targetAlpha(GhostEntity g) {
        long now = g.level().getGameTime();
        if (g.isFading()) return GhostBalance.SEEN_ALPHA * (1 - GhostBalance.fadeProgress(g.fadeStart(), now));
        if (g.isDispersed()) return GhostBalance.SEEN_ALPHA * (1 - GhostBalance.fadeProgress(g.dispersedAt(), now));
        if (g.isManifest() || g.isRevealed() || secondSight()) return GhostBalance.SEEN_ALPHA;
        if (GhostBalance.flickers(g.getId(), now)) return GhostBalance.FLICKER_ALPHA;
        return 0f;
    }

    public static void tick(GhostEntity g) {
        g.clientAlphaO = g.clientAlpha;
        float target = targetAlpha(g);
        g.clientAlpha += (target - g.clientAlpha) * (target > g.clientAlpha ? 0.35f : 0.5f);
        if (g.clientAlpha < 0.005f) g.clientAlpha = 0f;
        breath(g);
    }

    /** In a ghost's cold, the local player's breath mists in front of their face. */
    private static void breath(GhostEntity g) {
        Player p = Minecraft.getInstance().player;
        if (p == null || g.isInert() || !g.nightHere() || p.isSpectator()) return;
        if (p.distanceToSqr(g) > GhostBalance.HAUNT_RANGE * GhostBalance.HAUNT_RANGE) return;
        // One puff every ~1.5 s, whichever ghost is nearest doing it (ids spread them out).
        if ((g.level().getGameTime() + g.getId()) % 30 != 0) return;
        Vec3 look = p.getLookAngle();
        Vec3 mouth = p.getEyePosition().add(look.scale(0.45)).add(0, -0.12, 0);
        for (int i = 0; i < 3; i++) {
            g.level().addParticle(ParticleTypes.WHITE_SMOKE, mouth.x, mouth.y, mouth.z,
                    look.x * 0.03 + (g.getRandom().nextDouble() - 0.5) * 0.01, 0.01, look.z * 0.03 + (g.getRandom().nextDouble() - 0.5) * 0.01);
        }
    }
}
