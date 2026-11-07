package org.papiricoh.supernaturalcraft.client.godstiel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.ArrayList;
import java.util.List;

/**
 * Godstiel's fight in the world (v0.39), all on the client: the light (gold and soul blue) and the Leviathans' black, as
 * particles. One-off bursts are spawned at once; the door, the souls pouring and his struggle are emitters that run for their
 * duration ({@link #tick}).
 */
public final class GodstielWorldFx {

    static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.89f, 0.54f), 1.4f);
    static final DustParticleOptions SOUL = new DustParticleOptions(new Vector3f(0.62f, 0.89f, 1f), 1.2f);
    static final DustParticleOptions GOO = new DustParticleOptions(new Vector3f(0.06f, 0.03f, 0.09f), 1.6f);
    static final DustParticleOptions VIOLET = new DustParticleOptions(new Vector3f(0.55f, 0.3f, 0.9f), 1.2f);

    private abstract static class Emitter {
        int age;
        final int life;

        Emitter(int life) {
            this.life = Math.max(1, life);
        }

        abstract void tick(ClientLevel level, RandomSource r);
    }

    private static final List<Emitter> EMITTERS = new ArrayList<>();

    private GodstielWorldFx() {
    }

    static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            EMITTERS.clear();
            return;
        }
        RandomSource r = level.random;
        for (Emitter e : EMITTERS) {
            e.age++;
            e.tick(level, r);
        }
        EMITTERS.removeIf(e -> e.age >= e.life);
    }

    static void clear() {
        EMITTERS.clear();
    }

    private static ClientLevel level() {
        return Minecraft.getInstance().level;
    }

    private static void ring(ClientLevel level, Vec3 c, double r, ParticleOptions p, int n, double up) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            level.addParticle(p, c.x + Math.cos(a) * r, c.y + 0.1, c.z + Math.sin(a) * r, 0, up, 0);
        }
    }

    private static void column(ClientLevel level, Vec3 at, ParticleOptions p, double height, int n, RandomSource r) {
        for (int i = 0; i < n; i++) {
            level.addParticle(p, at.x + (r.nextDouble() - 0.5) * 0.6, at.y + height * i / n, at.z + (r.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
        }
    }

    private static void line(ClientLevel level, Vec3 from, Vec3 to, ParticleOptions p, int n, RandomSource r) {
        for (int i = 0; i <= n; i++) {
            Vec3 at = from.lerp(to, i / (double) n);
            level.addParticle(p, at.x + (r.nextDouble() - 0.5) * 0.4, at.y + 0.2 + r.nextDouble() * 0.6, at.z + (r.nextDouble() - 0.5) * 0.4, 0, 0.03, 0);
        }
    }

    // --- the light ---------------------------------------------------------------------------------------------------------

    static void smite(Vec3 at, float radius) {
        ClientLevel level = level();
        if (level == null) return;
        column(level, at, ParticleTypes.END_ROD, 7, 28, level.random);
        column(level, at, GOLD, 5, 20, level.random);
        ring(level, at, radius, GOLD, 16, 0.05);
        level.addParticle(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 0, 0, 0);
    }

    static void palm(Vec3 from, float yaw, float reach) {
        ClientLevel level = level();
        if (level == null) return;
        Vec3 dir = new Vec3(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
        line(level, from.add(0, 0.6, 0), from.add(dir.scale(reach)).add(0, 0.6, 0), ParticleTypes.END_ROD, Math.round(reach * 3), level.random);
        line(level, from, from.add(dir.scale(reach)), GOLD, Math.round(reach * 2), level.random);
    }

    static void radiance(Vec3 at, float radius) {
        ClientLevel level = level();
        if (level == null) return;
        for (int k = 1; k <= 4; k++) ring(level, at, radius * k / 4, k % 2 == 0 ? GOLD : SOUL, 12 + k * 8, 0.08);
        level.addParticle(ParticleTypes.FLASH, at.x, at.y + 1.4, at.z, 0, 0, 0);
        for (int i = 0; i < 16; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            level.addParticle(AllParticles.SHADOW_FEATHER.get(), at.x + Math.cos(a) * 1.5, at.y + 1.8 + level.random.nextDouble(), at.z + Math.sin(a) * 1.5,
                    Math.cos(a) * 0.1, -0.02, Math.sin(a) * 0.1);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.position().distanceTo(at) <= radius + 1) GodstielOverlay.flash(0.8f, 0xFFFFFF);
    }

    static void judged(Vec3 at) {
        ClientLevel level = level();
        if (level == null) return;
        for (int i = 0; i < 40; i++) {
            double a = level.random.nextDouble() * Math.PI * 2, s = 0.15 + level.random.nextDouble() * 0.25;
            level.addParticle(i % 3 == 0 ? ParticleTypes.FLAME : GOLD, at.x, at.y + 1, at.z, Math.cos(a) * s, 0.1 + level.random.nextDouble() * 0.2, Math.sin(a) * s);
        }
        level.addParticle(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 0, 0, 0);
    }

    static void bolt(Vec3 from, Vec3 to, boolean goo) {
        ClientLevel level = level();
        if (level == null) return;
        line(level, from, to, goo ? GOO : ParticleTypes.END_ROD, Math.max(6, (int) (from.distanceTo(to) * 3)), level.random);
        line(level, from, to, goo ? VIOLET : GOLD, Math.max(4, (int) (from.distanceTo(to) * 2)), level.random);
    }

    // --- the Leviathans ----------------------------------------------------------------------------------------------------

    static void crack(Entity e, int sets) {
        ClientLevel level = level();
        if (level == null) return;
        Vec3 at = e.position();
        for (int i = 0; i < 20 + sets * 15; i++) {
            level.addParticle(i % 2 == 0 ? GOO : ParticleTypes.SQUID_INK, at.x + (level.random.nextDouble() - 0.5) * 0.8, at.y + 0.5 + level.random.nextDouble() * 1.4,
                    at.z + (level.random.nextDouble() - 0.5) * 0.8, (level.random.nextDouble() - 0.5) * 0.2, 0.05, (level.random.nextDouble() - 0.5) * 0.2);
        }
        for (int i = 0; i < 10; i++) level.addParticle(SOUL, at.x, at.y + 1.3, at.z, (level.random.nextDouble() - 0.5) * 0.3, 0.2, (level.random.nextDouble() - 0.5) * 0.3);
    }

    static void goo(Vec3 from, float yaw, float reach) {
        ClientLevel level = level();
        if (level == null) return;
        for (int i = 0; i < 50; i++) {
            float a = yaw + (level.random.nextFloat() - 0.5f) * 110f;
            double d = level.random.nextDouble() * reach;
            Vec3 dir = new Vec3(-Math.sin(Math.toRadians(a)), 0, Math.cos(Math.toRadians(a)));
            Vec3 at = from.add(dir.scale(d)).add(0, 1.2 - d * 0.1, 0);
            level.addParticle(i % 3 == 0 ? AllParticles.GOO_DROP.get() : GOO, at.x, at.y, at.z, dir.x * 0.1, -0.05, dir.z * 0.1);
        }
    }

    static void sweep(Vec3 at, float radius) {
        ClientLevel level = level();
        if (level == null) return;
        ring(level, at, radius, GOO, 28, 0.06);
        ring(level, at, radius * 0.6f, ParticleTypes.SQUID_INK, 18, 0.1);
    }

    static void surge(Vec3 at, float radius) {
        ClientLevel level = level();
        if (level == null) return;
        column(level, at, ParticleTypes.SQUID_INK, 5, 30, level.random);
        column(level, at, GOO, 4, 24, level.random);
        ring(level, at, radius, VIOLET, 12, 0.15);
    }

    static void blackWater(Vec3 at, int duration) {
        EMITTERS.add(new Emitter(duration) {
            @Override
            void tick(ClientLevel level, RandomSource r) {
                for (int i = 0; i < 6; i++) {
                    double a = r.nextDouble() * Math.PI * 2, d = 4 + r.nextDouble() * 14;
                    level.addParticle(ParticleTypes.SQUID_INK, at.x + Math.cos(a) * d, at.y - 0.6, at.z + Math.sin(a) * d, 0, 0.05, 0);
                }
            }
        });
    }

    // --- the story ---------------------------------------------------------------------------------------------------------

    /** The door to Purgatory, hanging in the air: a tall ring of soul light (open) or its last flicker (closing). */
    static void door(Vec3 at, boolean open, int duration) {
        if (!open) {
            ClientLevel level = level();
            if (level != null) for (int i = 0; i < 30; i++) level.addParticle(SOUL, at.x, at.y, at.z, (level.random.nextDouble() - 0.5) * 0.4,
                    (level.random.nextDouble() - 0.5) * 0.4, (level.random.nextDouble() - 0.5) * 0.4);
            return;
        }
        EMITTERS.add(new Emitter(duration) {
            @Override
            void tick(ClientLevel level, RandomSource r) {
                float grow = Math.min(1f, age / 30f);
                for (int i = 0; i < 10; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double x = at.x + Math.cos(a) * 1.6 * grow, y = at.y + Math.sin(a) * 2.4 * grow;
                    level.addParticle(i % 3 == 0 ? ParticleTypes.END_ROD : AllParticles.PURGATORY_SOUL.get(), x, y, at.z, 0, 0, 0);
                }
                if (age % 3 == 0) level.addParticle(ParticleTypes.SOUL, at.x + (r.nextDouble() - 0.5) * 2, at.y + (r.nextDouble() - 0.5) * 3, at.z, 0, -0.02, 0);
            }
        });
    }

    /** The souls pouring out of the door into whoever takes them. */
    static void souls(Vec3 from, int entity, int duration) {
        EMITTERS.add(new Emitter(duration) {
            @Override
            void tick(ClientLevel level, RandomSource r) {
                Entity e = level.getEntity(entity);
                if (e == null) return;
                Vec3 to = e.position().add(0, 1.2, 0);
                for (int i = 0; i < 4; i++) {
                    Vec3 at = from.lerp(to, r.nextDouble());
                    Vec3 v = to.subtract(at).normalize().scale(0.25);
                    level.addParticle(i % 2 == 0 ? ParticleTypes.SOUL : AllParticles.PURGATORY_SOUL.get(), at.x, at.y, at.z, v.x, v.y, v.z);
                }
            }
        });
    }

    /** His struggle: black seeping out of him and soul light fighting back, for as long as it lasts. */
    static void struggle(int entity, int duration) {
        EMITTERS.add(new Emitter(duration) {
            @Override
            void tick(ClientLevel level, RandomSource r) {
                Entity e = level.getEntity(entity);
                if (e == null) return;
                Vec3 at = e.position();
                level.addParticle(age % 2 == 0 ? AllParticles.GOO_DROP.get() : AllParticles.GRACE_MOTE.get(), at.x + (r.nextDouble() - 0.5) * 0.8, at.y + 0.6 + r.nextDouble() * 1.4,
                        at.z + (r.nextDouble() - 0.5) * 0.8, 0, 0.03, 0);
            }
        });
    }

    static void graceMote(Vec3 at) {
        ClientLevel level = level();
        if (level != null) level.addParticle(AllParticles.GRACE.get(), at.x, at.y, at.z, 0, 0.05, 0);
    }
}
