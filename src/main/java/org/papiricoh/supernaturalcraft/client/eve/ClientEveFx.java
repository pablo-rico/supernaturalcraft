package org.papiricoh.supernaturalcraft.client.eve;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.EveFxPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays out the moments of Eve's fight and the Alphas' (v0.39, {@link EveFxPayload}): the title cards ({@link EveOverlay}) and the
 * effects in the world, all particles: the lash's claw marks, roots breaking the ground, the earth heaving where a child climbs
 * out, the roach swarm running down its lane, the call's ring, the thorns, the whole glade erupting, the amber in an answered soul's
 * eyes, and the Alphas' moves.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientEveFx {

    private static final DustParticleOptions ROACH = new DustParticleOptions(new Vector3f(0.23f, 0.14f, 0.07f), 0.7f);
    private static final DustParticleOptions AMBER = new DustParticleOptions(new Vector3f(0.95f, 0.64f, 0.23f), 0.8f);
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55f, 0.04f, 0.06f), 0.9f);
    private static final ParticleOptions EARTH = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState());

    /** An effect running over several ticks. */
    private static final class Run {
        final byte kind;
        final Vec3 at;
        final float yaw, size;
        final int life;
        int age;

        Run(byte kind, Vec3 at, float yaw, float size, int life) {
            this.kind = kind;
            this.at = at;
            this.yaw = yaw;
            this.size = size;
            this.life = Math.max(1, life);
        }
    }

    private static final List<Run> RUNS = new ArrayList<>();

    private ClientEveFx() {
    }

    public static void handle(EveFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ClientLevel level = mc.level;
        RandomSource r = level.random;
        Vec3 at = p.point();
        switch (p.kind()) {
            case EveFxPayload.TITLE -> EveOverlay.title(p.arg(), p.arg2() == 1, p.duration());
            case EveFxPayload.LASH -> {
                float yaw = p.arg() * Mth.DEG_TO_RAD, reach = p.arg2() / 10f;
                for (int i = 0; i < 24; i++) {
                    float a = yaw + (i / 23f - 0.5f) * 2.0f, d = reach * (0.4f + r.nextFloat() * 0.6f);
                    level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x - Mth.sin(a) * d, at.y + 1.0, at.z + Mth.cos(a) * d, 0, 0, 0);
                }
            }
            case EveFxPayload.ROOTS -> RUNS.add(new Run(p.kind(), at, 0, p.arg() / 10f, Math.min(30, p.duration())));
            case EveFxPayload.BIRTH -> {
                for (int i = 0; i < 40; i++) {
                    level.addParticle(EARTH, at.x + (r.nextDouble() - 0.5) * 1.6, at.y + 0.1, at.z + (r.nextDouble() - 0.5) * 1.6, 0, 0.3, 0);
                }
                for (int i = 0; i < 10; i++) level.addParticle(ParticleTypes.SOUL, at.x, at.y + 0.5, at.z, (r.nextDouble() - 0.5) * 0.1, 0.08, (r.nextDouble() - 0.5) * 0.1);
            }
            case EveFxPayload.SWARM -> RUNS.add(new Run(p.kind(), at, p.arg(), p.arg2() / 10f, p.duration()));
            case EveFxPayload.CALL -> RUNS.add(new Run(p.kind(), at, 0, Math.min(24, p.arg()), p.duration()));
            case EveFxPayload.THORNS -> {
                float inner = p.arg() / 10f, outer = p.arg2() / 10f;
                for (int i = 0; i < 90; i++) {
                    double a = r.nextDouble() * Math.PI * 2, d = inner + r.nextDouble() * (outer - inner);
                    level.addParticle(EARTH, at.x + Math.cos(a) * d, at.y + 0.1, at.z + Math.sin(a) * d, 0, 0.35 + r.nextDouble() * 0.2, 0);
                }
            }
            case EveFxPayload.FIELD -> RUNS.add(new Run(p.kind(), at, 0, p.arg(), p.duration()));
            case EveFxPayload.ANSWERED -> {
                for (int i = 0; i < 8; i++) level.addParticle(AMBER, at.x + (r.nextDouble() - 0.5) * 0.4, at.y, at.z + (r.nextDouble() - 0.5) * 0.4, 0, 0, 0);
            }
            case EveFxPayload.ALPHA -> alpha(level, r, p);
            default -> {
            }
        }
    }

    private static void alpha(ClientLevel level, RandomSource r, EveFxPayload p) {
        Vec3 at = p.point();
        switch (p.arg()) {
            case 0 -> { // the vampire's lunge: a smear of shadow
                for (int i = 0; i < 20; i++) level.addParticle(ParticleTypes.LARGE_SMOKE, at.x + r.nextGaussian() * 0.3, at.y + 1 + r.nextGaussian() * 0.4,
                        at.z + r.nextGaussian() * 0.3, 0, 0.02, 0);
            }
            case 1 -> { // his drain
                for (int i = 0; i < 16; i++) level.addParticle(BLOOD, at.x + r.nextGaussian() * 0.4, at.y + 1.4, at.z + r.nextGaussian() * 0.4, 0, 0, 0);
            }
            case 2, 4 -> RUNS.add(new Run(EveFxPayload.CALL, at, 0, p.arg() == 2 ? 10 : 16, 24));
            case 3, 5 -> {
                for (int i = 0; i < 30; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    level.addParticle(ParticleTypes.CLOUD, at.x + Math.cos(a) * 1.5, at.y + 0.1, at.z + Math.sin(a) * 1.5, Math.cos(a) * 0.2, 0.02, Math.sin(a) * 0.2);
                }
            }
            default -> { // the shapeshifter's face, or a skin
                for (int i = 0; i < 24; i++) level.addParticle(ParticleTypes.CRIMSON_SPORE, at.x + r.nextGaussian() * 0.3, at.y + 1.2 + r.nextGaussian() * 0.5,
                        at.z + r.nextGaussian() * 0.3, 0, 0, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            RUNS.clear();
            EveOverlay.clear();
            return;
        }
        EveOverlay.tick();
        if (mc.isPaused() || RUNS.isEmpty()) return;
        ClientLevel level = mc.level;
        RandomSource r = level.random;
        for (Run run : RUNS) {
            float k = run.age / (float) run.life;
            switch (run.kind) {
                case EveFxPayload.SWARM -> {
                    // The carpet of roaches runs out down the lane and keeps seething in it.
                    Vec3 dir = new Vec3(-Mth.sin(run.yaw * Mth.DEG_TO_RAD), 0, Mth.cos(run.yaw * Mth.DEG_TO_RAD));
                    Vec3 side = new Vec3(-dir.z, 0, dir.x);
                    float front = Math.min(1, run.age / 8f) * run.size;
                    for (int i = 0; i < 10; i++) {
                        double along = r.nextDouble() * front, across = (r.nextDouble() - 0.5) * 2.6;
                        Vec3 p = run.at.add(dir.scale(along)).add(side.scale(across));
                        level.addParticle(ROACH, p.x, p.y + 0.08, p.z, 0, 0, 0);
                    }
                }
                case EveFxPayload.ROOTS -> {
                    for (int i = 0; i < 6; i++) {
                        double a = r.nextDouble() * Math.PI * 2, d = r.nextDouble() * run.size;
                        level.addParticle(EARTH, run.at.x + Math.cos(a) * d, run.at.y + 0.1, run.at.z + Math.sin(a) * d, 0, 0.25, 0);
                    }
                }
                case EveFxPayload.CALL -> {
                    float rad = k * run.size;
                    for (int i = 0; i < 12; i++) {
                        double a = r.nextDouble() * Math.PI * 2;
                        level.addParticle(ParticleTypes.SOUL, run.at.x + Math.cos(a) * rad, run.at.y + 0.4, run.at.z + Math.sin(a) * rad, 0, 0.02, 0);
                    }
                }
                case EveFxPayload.FIELD -> {
                    for (int i = 0; i < 14; i++) {
                        double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * run.size;
                        level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, run.at.x + Math.cos(a) * d, run.at.y + 0.2, run.at.z + Math.sin(a) * d, 0, 0.02, 0);
                    }
                }
                default -> {
                }
            }
            run.age++;
        }
        RUNS.removeIf(run -> run.age >= run.life);
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    /** Forget everything (left the world, or a preview between scenes). */
    public static void clear() {
        RUNS.clear();
        EveOverlay.clear();
    }
}
