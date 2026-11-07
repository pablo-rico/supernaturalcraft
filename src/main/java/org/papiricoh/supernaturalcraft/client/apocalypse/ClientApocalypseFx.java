package org.papiricoh.supernaturalcraft.client.apocalypse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.client.apocalypse.seal.SealOverlay;
import org.papiricoh.supernaturalcraft.network.AlastairFxPayload;
import org.papiricoh.supernaturalcraft.network.RubyFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * Plays Alastair's and Ruby's moments on this client (v0.40): the title cards (drawn by the seals' card layer, which shows during
 * camera sequences), the chains, scalpels, hook, smoke and rack as particles. Every kind is in its payload's javadoc.
 */
public final class ClientApocalypseFx {

    private static final int RED = 0xC0281C, BONE = 0xE8DCC4;

    private ClientApocalypseFx() {
    }

    public static void handle(AlastairFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        RandomSource r = level.random;
        Vec3 at = p.point();
        switch (p.kind()) {
            case AlastairFxPayload.TITLE -> {
                String key = p.arg2() == 1 ? "title.supernaturalcraft.alastair.fall" : "title.supernaturalcraft.alastair.phase" + Math.max(1, p.arg());
                SealOverlay.card(Component.translatable(key), Component.translatable(key + ".sub"), p.arg() >= 3 ? BONE : RED, p.duration());
            }
            case AlastairFxPayload.CHAINS -> ring(level, at, p.arg() / 10f, ParticleTypes.CRIT, 50);
            case AlastairFxPayload.SCALPEL -> lane(level, at, p.arg(), p.arg2(), ParticleTypes.CRIT, 16);
            case AlastairFxPayload.HOOK -> lane(level, at, p.arg(), 18, ParticleTypes.CRIT, 24);
            case AlastairFxPayload.SMOKE -> {
                float radius = p.arg() / 10f;
                for (int i = 0; i < 60; i++) {
                    double a = r.nextDouble() * Mth.TWO_PI, d = r.nextDouble() * radius;
                    level.addParticle(ParticleTypes.CLOUD, at.x + Math.cos(a) * d, at.y + 0.2 + r.nextDouble() * 1.8, at.z + Math.sin(a) * d, 0, 0.02, 0);
                }
            }
            case AlastairFxPayload.RACK -> ring(level, at, p.arg() / 10f, AllParticles.HELLFIRE.get(), 40);
            case AlastairFxPayload.FREE -> {
                ring(level, at, 2.5f, ParticleTypes.CRIT, 60);
                level.addParticle(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 0, 0, 0);
            }
            default -> {
            }
        }
    }

    public static void handle(RubyFxPayload p) {
        if (p.kind() == RubyFxPayload.TITLE) {
            SealOverlay.card(Component.translatable("title.supernaturalcraft.ruby"), Component.translatable("title.supernaturalcraft.ruby.sub"), RED, p.duration());
        }
    }

    private static void ring(ClientLevel level, Vec3 at, float radius, ParticleOptions particle, int n) {
        for (int i = 0; i < n; i++) {
            double a = Mth.TWO_PI * i / n;
            level.addParticle(particle, at.x + Math.cos(a) * radius, at.y + 0.2, at.z + Math.sin(a) * radius, 0, 0.05, 0);
        }
    }

    private static void lane(ClientLevel level, Vec3 from, float yaw, float length, ParticleOptions particle, int n) {
        Vec3 dir = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
        for (int i = 0; i < n; i++) {
            Vec3 q = from.add(dir.scale(length * i / n));
            level.addParticle(particle, q.x, q.y + 1.1, q.z, dir.x * 0.2, 0, dir.z * 0.2);
        }
    }
}
