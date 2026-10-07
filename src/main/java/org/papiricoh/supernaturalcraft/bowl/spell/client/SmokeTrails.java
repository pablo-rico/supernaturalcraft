package org.papiricoh.supernaturalcraft.bowl.spell.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.SmokeTrailPayload;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Draws a locating spell's smoke: it rises from the bowl about a block and a half, then a head of
 * smoke travels toward the target (at most {@link #MAX_LENGTH} blocks, swaying gently), and wisps
 * hang along the path for the payload's {@code linger} ticks.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class SmokeTrails {

    public static final double MAX_LENGTH = 40;
    static final double RISE = 1.5;
    static final int RISE_TICKS = 15;
    static final double HEAD_SPEED = 0.8;
    static final double SWAY = 0.35;
    /** Trails further than this from the viewer are not drawn. */
    static final double VIEW_RANGE = 128;

    private static final List<Trail> TRAILS = new ArrayList<>();

    private SmokeTrails() {
    }

    public static void add(SmokeTrailPayload payload) {
        TRAILS.add(new Trail(payload.from(), payload.to(), payload.color() & 0xFFFFFF, Math.max(0, payload.linger())));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            TRAILS.clear();
            return;
        }
        if (mc.isPaused() || TRAILS.isEmpty()) return;
        Vec3 eye = mc.player == null ? null : mc.player.getEyePosition();
        for (Iterator<Trail> it = TRAILS.iterator(); it.hasNext(); ) {
            Trail t = it.next();
            if (t.tick(level, eye)) it.remove();
        }
    }

    static final class Trail {
        final Vec3 from;
        final Vec3 top;
        final Vec3 dir;
        final Vec3 side;
        final double length;
        final double climb;
        final int color;
        final int linger;
        final int travelTicks;
        int age;

        Trail(Vec3 from, Vec3 to, int color, int linger) {
            this.from = from;
            this.top = from.add(0, RISE, 0);
            Vec3 flat = new Vec3(to.x - from.x, 0, to.z - from.z);
            double dist = flat.length();
            this.dir = dist < 1e-3 ? new Vec3(0, 0, 0) : flat.scale(1 / dist);
            this.side = new Vec3(-dir.z, 0, dir.x);
            this.length = Math.min(dist, MAX_LENGTH);
            // Climb or sink toward the target's height, gently, over the drawn part of the path.
            this.climb = dist < 1e-3 ? 0 : Mth.clamp((to.y - top.y) * (length / dist), -length * 0.3, length * 0.3);
            this.color = color;
            this.linger = linger;
            this.travelTicks = (int) Math.ceil(length / HEAD_SPEED);
        }

        /** The point {@code s} blocks along the path from the top of the rise. */
        Vec3 at(double s) {
            double f = length <= 0 ? 0 : s / length;
            double sway = Math.sin(s * 0.45) * SWAY * Math.min(1, s / 3);
            return top.add(dir.scale(s)).add(side.scale(sway)).add(0, climb * f + Math.sin(s * 0.3) * 0.15, 0);
        }

        /** @return true when it is done */
        boolean tick(ClientLevel level, Vec3 eye) {
            int t = age++;
            if (t > RISE_TICKS + travelTicks + linger) return true;
            if (eye != null && eye.distanceToSqr(from) > VIEW_RANGE * VIEW_RANGE) return false;
            RandomSource r = level.random;
            ColorParticleOption smoke = ColorParticleOption.create(AllParticles.BOWL_SMOKE.get(), 0xFF000000 | color);
            if (t < RISE_TICKS) {
                double y = RISE * (t + 1) / RISE_TICKS;
                for (int i = 0; i < 3; i++) {
                    puff(level, smoke, from.add(jitter(r, 0.12), y + jitter(r, 0.1), jitter(r, 0.12)), 0.01);
                }
                return false;
            }
            // The column above the bowl keeps smoking a little while the trail lives.
            if (r.nextInt(3) == 0) puff(level, smoke, from.add(jitter(r, 0.1), r.nextDouble() * RISE, jitter(r, 0.1)), 0.02);
            int travelled = t - RISE_TICKS;
            if (travelled <= travelTicks && length > 0) {
                double head = Math.min(length, travelled * HEAD_SPEED);
                for (int i = 0; i < 4; i++) {
                    double s = Math.max(0, head - r.nextDouble() * HEAD_SPEED * 1.5);
                    puff(level, smoke, at(s).add(jitter(r, 0.15), jitter(r, 0.1), jitter(r, 0.15)), 0.004);
                }
                if (r.nextInt(5) == 0) {
                    Vec3 p = at(head);
                    level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, p.x, p.y, p.z, 0, 0.005, 0);
                }
                return false;
            }
            // Lingering: sparse wisps along the path, thinning toward the end.
            int left = RISE_TICKS + travelTicks + linger - t;
            double density = Math.min(1.0, left / 120.0);
            int wisps = length <= 0 ? 0 : (int) Math.ceil(Math.max(1, length / 10) * density);
            for (int i = 0; i < wisps; i++) {
                if (r.nextDouble() > 0.35 + 0.4 * density) continue;
                puff(level, smoke, at(r.nextDouble() * length).add(jitter(r, 0.25), jitter(r, 0.2), jitter(r, 0.25)), 0.003);
            }
            return false;
        }

        private static double jitter(RandomSource r, double amount) {
            return (r.nextDouble() - 0.5) * 2 * amount;
        }

        private static void puff(ClientLevel level, ColorParticleOption smoke, Vec3 p, double rise) {
            level.addParticle(smoke, true, p.x, p.y, p.z, 0, rise, 0);
        }
    }
}
