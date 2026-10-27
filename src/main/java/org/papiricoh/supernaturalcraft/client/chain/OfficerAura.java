package org.papiricoh.supernaturalcraft.client.chain;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;

/**
 * The visible mark of an officer of the Chain of Command (client only): motes around it that grow with its rank, soul fire for
 * the Pit, light for the Host, ash for the Fallen, a guild's dye for its hired officers; a Lord also walks inside a slow ring of them.
 */
final class OfficerAura {

    private static final double RANGE_SQR = 48 * 48;

    private OfficerAura() {
    }

    static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        RandomSource r = mc.level.random;
        for (Entity e : mc.level.entitiesForRendering()) {
            int rank = e instanceof BlackEyedDemon d ? d.officerRank() : e instanceof AngelEntity a ? a.officerRank() : -1;
            if (rank < 0 || e.distanceToSqr(mc.player) > RANGE_SQR || e.isInvisible()) continue;
            ParticleOptions mote = e instanceof AngelEntity a ? a.fallen() ? ParticleTypes.ASH : ParticleTypes.END_ROD
                    : ParticleTypes.SOUL_FIRE_FLAME;
            var post = ClientChain.entryFor(e);
            if (post != null && ClientChain.isGuild(post.faction())) {
                // A guild's hired officer wears its colours: motes of its dye.
                int c = ClientChain.guildGlow(post.faction());
                mote = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(((c >> 16) & 255) / 255f,
                        ((c >> 8) & 255) / 255f, (c & 255) / 255f), 1f);
            }
            if (r.nextFloat() < 0.12f + 0.12f * rank) {
                double x = e.getX() + (r.nextDouble() - 0.5) * e.getBbWidth() * 1.6;
                double y = e.getY() + r.nextDouble() * e.getBbHeight();
                double z = e.getZ() + (r.nextDouble() - 0.5) * e.getBbWidth() * 1.6;
                mc.level.addParticle(mote, x, y, z, 0, 0.02, 0);
            }
            if (rank >= org.papiricoh.supernaturalcraft.chain.ChainRank.LORD.ordinal()) {
                double angle = (mc.level.getGameTime() % 80) / 80.0 * Math.PI * 2;
                for (int i = 0; i < 2; i++) {
                    double t = angle + i * Math.PI;
                    mc.level.addParticle(mote, e.getX() + Math.cos(t) * 0.9, e.getY() + 0.05, e.getZ() + Math.sin(t) * 0.9, 0, 0.01, 0);
                }
            }
        }
    }
}
