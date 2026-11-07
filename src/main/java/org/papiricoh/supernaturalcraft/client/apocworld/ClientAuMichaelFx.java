package org.papiricoh.supernaturalcraft.client.apocworld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.client.apocalypse.seal.SealOverlay;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * Plays AU Michael's moments on this client (v0.41): the title cards (drawn by the seals' card layer, which shows during camera
 * sequences), his blade's arcs, the holy fire, the gale, the burned sky's bolts, Jack's grace flaring gold at a hunter's side and
 * the rift taking him. Every kind is in {@link AuMichaelFxPayload}'s javadoc.
 */
public final class ClientAuMichaelFx {

    private static final int GOLD = 0xFFD27A, PALE = 0xCFE6FF;

    private ClientAuMichaelFx() {
    }

    public static void handle(AuMichaelFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        RandomSource r = level.random;
        Vec3 at = p.point();
        switch (p.kind()) {
            case AuMichaelFxPayload.TITLE -> {
                String key = p.arg2() == 1 ? "title.supernaturalcraft.au_michael.flight" : "title.supernaturalcraft.au_michael.phase" + Math.max(1, p.arg());
                SealOverlay.card(Component.translatable(key), Component.translatable(key + ".sub"), p.arg() >= 3 ? PALE : GOLD, p.duration());
            }
            case AuMichaelFxPayload.SMITE -> {
                float radius = p.arg() / 10f;
                ring(level, at, radius, AllParticles.GRACE.get(), 40);
                for (int i = 0; i < 40; i++) {
                    double a = r.nextDouble() * Mth.TWO_PI, d = r.nextDouble() * radius;
                    level.addParticle(ParticleTypes.FLAME, at.x + Math.cos(a) * d, at.y + 0.1, at.z + Math.sin(a) * d, 0, 0.12 + r.nextDouble() * 0.1, 0);
                }
            }
            case AuMichaelFxPayload.GALE -> {
                Vec3 dir = dir(p.arg());
                for (int i = 0; i < 70; i++) {
                    double spread = (r.nextDouble() - 0.5) * 1.6, d = r.nextDouble() * p.arg2();
                    Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(spread * d * 0.6);
                    Vec3 q = at.add(dir.scale(d)).add(side);
                    level.addParticle(ParticleTypes.CLOUD, q.x, q.y + 0.4 + r.nextDouble() * 1.4, q.z, dir.x * 0.6, 0.02, dir.z * 0.6);
                }
            }
            case AuMichaelFxPayload.BOLT -> {
                level.addParticle(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 0, 0, 0);
                ring(level, at, 1.5f, ParticleTypes.ELECTRIC_SPARK, 24);
                for (int i = 0; i < 16; i++) level.addParticle(ParticleTypes.ASH, at.x, at.y + r.nextDouble() * 3, at.z, 0, 0.05, 0);
            }
            case AuMichaelFxPayload.JACK -> {
                // Jack's grace beside the hunter: a gold flash at their shoulder.
                Entity hunter = level.getEntity(p.arg());
                Vec3 c = hunter != null ? hunter.position().add(hunter.getLookAngle().yRot(Mth.HALF_PI).multiply(1, 0, 1).normalize().scale(0.9)) : at;
                level.addParticle(ParticleTypes.FLASH, c.x, c.y + 1.2, c.z, 0, 0, 0);
                for (int i = 0; i < 60; i++) {
                    level.addParticle(AllParticles.GRACE.get(), c.x + (r.nextDouble() - 0.5) * 0.8, c.y + r.nextDouble() * 2, c.z + (r.nextDouble() - 0.5) * 0.8,
                            (r.nextDouble() - 0.5) * 0.1, 0.05, (r.nextDouble() - 0.5) * 0.1);
                }
                for (int i = 0; i < 20; i++) level.addParticle(ParticleTypes.END_ROD, c.x, c.y + 1.2, c.z, (r.nextDouble() - 0.5) * 0.3, r.nextDouble() * 0.2, (r.nextDouble() - 0.5) * 0.3);
            }
            case AuMichaelFxPayload.FLEE -> {
                level.addParticle(ParticleTypes.FLASH, at.x, at.y + 2, at.z, 0, 0, 0);
                for (int i = 0; i < 80; i++) {
                    level.addParticle(ParticleTypes.FLAME, at.x + (r.nextDouble() - 0.5) * 3, at.y + r.nextDouble() * 5, at.z + (r.nextDouble() - 0.5) * 3, 0, 0.03, 0);
                    level.addParticle(AllParticles.GRACE.get(), at.x, at.y + 2, at.z, (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.4);
                }
            }
            case AuMichaelFxPayload.SLASH -> {
                Vec3 dir = dir(p.arg());
                float reach = p.arg2() / 10f;
                for (int i = 0; i < 18; i++) {
                    double a = Math.toRadians(-55 + 110.0 * i / 17);
                    Vec3 d = dir.yRot((float) a).scale(reach * 0.8);
                    level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x + d.x, at.y + 1.1, at.z + d.z, 0, 0, 0);
                }
            }
            default -> {
            }
        }
    }

    private static Vec3 dir(float yaw) {
        return new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
    }

    private static void ring(ClientLevel level, Vec3 at, float radius, ParticleOptions particle, int n) {
        for (int i = 0; i < n; i++) {
            double a = Mth.TWO_PI * i / n;
            level.addParticle(particle, at.x + Math.cos(a) * radius, at.y + 0.2, at.z + Math.sin(a) * radius, 0, 0.05, 0);
        }
    }
}
