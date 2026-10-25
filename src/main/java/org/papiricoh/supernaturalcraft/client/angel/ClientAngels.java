package org.papiricoh.supernaturalcraft.client.angel;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicShake;
import org.papiricoh.supernaturalcraft.entity.angel.AngelAssets;
import org.papiricoh.supernaturalcraft.network.SmiteFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The client's side of the angels (v0.22): smitten angels and the Fall ({@link SmiteFxPayload}).
 * <ul>
 *   <li>A smite ({@link Smite}): for its {@code duration} the light pours out of the angel's eyes and mouth and through its body
 *   ({@link SmiteGlowLayer} on its renderer, the beams by {@link SmiteFx} in a world stage), then the flash, and its wings stay
 *   burnt into the ground ({@link Scorch}) for {@link AngelAssets#SCORCH_TICKS}.</li>
 *   <li>The Fall ({@link FallSky}): the night's rain of falling angels over the Overworld, and single stars.</li>
 * </ul>
 * Every clock here is {@code level.getGameTime() + partial}. Everything is dropped when the level changes (a dimension change, a
 * logout): the server says again what still holds.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientAngels {

    /** Grace: the white-blue light of an angel, and its white-hot heart. */
    public static final int GRACE = 0xB4D8FF, GRACE_HOT = 0xEEF6FF;
    /** How long the flash lasts after a smitten angel falls. */
    static final int FLASH_TICKS = 14;
    /** How long the burnt wings stay hot (embers, smoke). */
    static final int SCORCH_HOT = 70;
    private static final int MAX_SCORCHES = 32;
    /** How far the flash reaches a viewer's eyes. */
    private static final double FLASH_RANGE = 48;

    static final Int2ObjectMap<Smite> SMITES = new Int2ObjectOpenHashMap<>();
    static final List<Scorch> SCORCHES = new ArrayList<>();
    private static final List<Pending> SOUNDS = new ArrayList<>();
    /** Bumped every frame: a light captured by a renderer this frame is current. */
    static int frame;
    private static @Nullable ClientLevel lastLevel;

    private ClientAngels() {
    }

    // --- the payload ----------------------------------------------------------------------------------------------------------

    public static void handleFx(SmiteFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        // A payload sent on arrival in a new level (the Fall's night) may come before the tick that notices the change.
        sync(level);
        switch (payload.kind()) {
            case SmiteFxPayload.SMITE -> {
                Smite s = new Smite(payload.entity(), level.getGameTime(), Math.max(1, payload.duration()), payload.point(), payload.yaw(),
                        payload.arg());
                SMITES.put(s.id, s);
                sound(payload.point().add(0, 1.5, 0), "smite_ring", 2.2f, 1f, 0);
            }
            case SmiteFxPayload.FALL_NIGHT -> FallSky.night(payload.arg() != 0);
            case SmiteFxPayload.FALL_STAR -> FallSky.star(level, payload.point().x, payload.point().z, Math.max(10, payload.duration()));
            default -> {
            }
        }
    }

    // --- state ------------------------------------------------------------------------------------------------------------------

    /** One smitten angel. Its lights ({@link #EYE_L}, {@link #EYE_R}, {@link #MOUTH}) are captured by its renderer each frame. */
    static final class Smite {
        static final int EYE_L = 0, EYE_R = 1, MOUTH = 2;
        final int id;
        final long start;
        final int duration;
        final Vec3 point;
        final float yaw;
        final int blade;
        /** Each light's world position and direction ({@code x, y, z, dx, dy, dz}), and the frame it was captured on. */
        final double[] lights = new double[18];
        final int[] frames = {-1, -1, -1};
        /** Where its chest was last drawn: the flash goes off there once it is gone. */
        Vec3 chest;
        boolean burst;

        Smite(int id, long start, int duration, Vec3 point, float yaw, int blade) {
            this.id = id;
            this.start = start;
            this.duration = duration;
            this.point = point;
            this.yaw = yaw;
            this.blade = blade;
            this.chest = point.add(0, 1.2, 0);
        }

        float age(float partial) {
            ClientLevel level = Minecraft.getInstance().level;
            return level == null ? 0 : level.getGameTime() - start + partial;
        }

        /** Light {@code slot}, if its renderer captured it this frame. */
        boolean current(int slot) {
            return frames[slot] == frame;
        }

        /** The direction the angel faces (its body yaw when struck). */
        Vec3 forward() {
            float r = yaw * Mth.DEG_TO_RAD;
            return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
        }
    }

    /** Burnt wings on the ground: the decal's corner heights on a grid (NaN where there is no ground). */
    record Scorch(double x, double y, double z, float yaw, long start, float[] heights, int index) {
        static final int GRID = 6;
        static final float HALF = 2.4f;

        /** How far before the centre texture row {@code v} lies: the texture is twice as wide as deep, the body at its bottom. */
        static float along(float v) {
            return (v - 1) * HALF;
        }
    }

    private record Pending(long at, Vec3 pos, String id, float volume, float pitch) {
    }

    /** The smite of entity {@code id}, if it is still burning or flashing. */
    static @Nullable Smite smite(int id) {
        return SMITES.get(id);
    }

    /** How brightly entity {@code id} burns now (0-1), or 0 if it is not being smitten. */
    public static float glow(int id, float partial) {
        Smite s = SMITES.get(id);
        if (s == null) return 0;
        float age = s.age(partial);
        if (age >= s.duration) return 0;
        return Mth.clamp(age / s.duration, 0.02f, 1);
    }

    /** A renderer saw light {@code slot} of entity {@code id} at (x, y, z), shining along (dx, dy, dz), this frame. */
    static void capture(int id, int slot, double x, double y, double z, float dx, float dy, float dz) {
        Smite s = SMITES.get(id);
        if (s == null) return;
        int o = slot * 6;
        s.lights[o] = x;
        s.lights[o + 1] = y;
        s.lights[o + 2] = z;
        s.lights[o + 3] = dx;
        s.lights[o + 4] = dy;
        s.lights[o + 5] = dz;
        s.frames[slot] = frame;
    }

    /** Whether anything is showing (previews). */
    public static boolean busy() {
        return !SMITES.isEmpty() || !SCORCHES.isEmpty() || FallSky.busy();
    }

    public static void clear() {
        SMITES.clear();
        SCORCHES.clear();
        SOUNDS.clear();
        FallSky.clear();
    }

    /** Plays angel sound {@code id} at {@code pos} in {@code delay} ticks. */
    static void sound(Vec3 pos, String id, float volume, float pitch, int delay) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (delay <= 0) play(level, pos, id, volume, pitch);
        else SOUNDS.add(new Pending(level.getGameTime() + delay, pos, id, volume, pitch));
    }

    private static void play(ClientLevel level, Vec3 pos, String id, float volume, float pitch) {
        level.playLocalSound(pos.x, pos.y, pos.z, AllSounds.angel(id), SoundSource.HOSTILE, volume, pitch, false);
    }

    /** Everything is dropped when the level changes (a dimension change, a logout). */
    private static void sync(@Nullable ClientLevel level) {
        if (level == lastLevel) return;
        clear();
        lastLevel = level;
    }

    // --- ticking -------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        frame++;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        sync(level);
        if (level == null || mc.isPaused()) return;
        long now = level.getGameTime();
        RandomSource r = level.random;
        for (Iterator<Smite> it = SMITES.values().iterator(); it.hasNext(); ) {
            Smite s = it.next();
            long age = now - s.start;
            if (age < s.duration) {
                burning(level, s, age, r);
            } else if (!s.burst) {
                s.burst = true;
                burst(mc, level, s, r);
            }
            if (age > s.duration + FLASH_TICKS || age < -5) it.remove();
        }
        for (Iterator<Scorch> it = SCORCHES.iterator(); it.hasNext(); ) {
            Scorch s = it.next();
            long age = now - s.start();
            if (age > AngelAssets.SCORCH_TICKS || age < -5) it.remove();
            else if (age < SCORCH_HOT) smoulder(level, s, age, r);
        }
        for (Iterator<Pending> it = SOUNDS.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (p.at() > now) continue;
            play(level, p.pos(), p.id(), p.volume(), p.pitch());
            it.remove();
        }
        FallSky.tick(mc, level);
    }

    /** Motes of light rising off a burning angel, more as it burns. */
    private static void burning(ClientLevel level, Smite s, long age, RandomSource r) {
        float k = age / (float) s.duration;
        Entity e = level.getEntity(s.id);
        Vec3 at = e != null ? e.position() : s.point;
        float h = e != null ? e.getBbHeight() : 1.8f;
        int n = FxParticles.rate(r, 0.4f + 2.2f * k * k);
        for (int i = 0; i < n; i++) {
            double y = at.y + h * (0.35 + 0.6 * r.nextDouble());
            FxParticles.spark(level, r.nextBoolean() ? GRACE : GRACE_HOT, 0.25f + 0.25f * k, 14 + r.nextInt(10),
                    at.x + FxParticles.j(r) * 0.4, y, at.z + FxParticles.j(r) * 0.4,
                    FxParticles.j(r) * 0.03, 0.02 + 0.05 * r.nextDouble(), FxParticles.j(r) * 0.03);
        }
    }

    /** The flash: light gone out of it, its wings burnt into the ground. */
    private static void burst(Minecraft mc, ClientLevel level, Smite s, RandomSource r) {
        Vec3 c = s.chest;
        sound(c, "smite_burst", 3f, 0.95f + 0.1f * r.nextFloat(), 0);
        if (mc.gameRenderer != null) {
            var camera = mc.gameRenderer.getMainCamera();
            Vec3 eye = camera.getPosition();
            double d = eye.distanceTo(c);
            if (d < FLASH_RANGE && seen(level, eye, c)) {
                Vec3 look = new Vec3(camera.getLookVector());
                float facing = (float) Math.max(0, look.dot(c.subtract(eye).normalize()));
                MagicOverlay.flash(c, 0xE6F2FF, 0.45f + 0.45f * facing, FLASH_RANGE);
            }
            MagicShake.kick(c, 1.4f, 8, 14);
        }
        int n = FxParticles.n(40);
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, up = r.nextDouble() * 0.25, sp = 0.15 + 0.25 * r.nextDouble();
            FxParticles.spark(level, i % 3 == 0 ? GRACE : GRACE_HOT, 0.5f, 12 + r.nextInt(12), c.x, c.y, c.z,
                    Math.cos(a) * sp, up, Math.sin(a) * sp);
        }
        Scorch sc = scorch(level, s.point, s.yaw, level.getGameTime());
        if (sc != null) {
            SCORCHES.add(sc);
            while (SCORCHES.size() > MAX_SCORCHES) SCORCHES.removeFirst();
        }
    }

    /** Whether the line from the eye to {@code at} is clear. */
    static boolean seen(ClientLevel level, Vec3 eye, Vec3 at) {
        return level.clip(new ClipContext(eye, at, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()))
                .getType() == HitResult.Type.MISS;
    }

    /** Embers and thin smoke rising off the burnt wings for the first seconds. */
    private static void smoulder(ClientLevel level, Scorch s, long age, RandomSource r) {
        float k = 1 - age / (float) SCORCH_HOT;
        int n = FxParticles.rate(r, 1.6f * k);
        float yr = s.yaw() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yr), fz = Mth.cos(yr);
        for (int i = 0; i < n; i++) {
            // Along the wings: out to either side and back from the body.
            double side = (r.nextBoolean() ? 1 : -1) * (0.4 + 1.8 * r.nextDouble()), back = -0.2 + 1.6 * r.nextDouble() * (Math.abs(side) / 2.2);
            double x = s.x() - fz * side - fx * back, z = s.z() + fx * side - fz * back;
            if (r.nextFloat() < 0.6f) {
                FxParticles.ember(level, r.nextBoolean() ? 0xFFB070 : GRACE, 0.3f, 14 + r.nextInt(12), x, s.y() + 0.05, z,
                        FxParticles.j(r) * 0.01, 0.02 + 0.02 * r.nextDouble(), FxParticles.j(r) * 0.01);
            } else {
                FxParticles.plain(level, net.minecraft.core.particles.ParticleTypes.SMOKE, x, s.y() + 0.05, z, 0, 0.03, 0);
            }
        }
    }

    /** The burnt wings of an angel that fell at {@code at} facing {@code yaw}, laid on the ground there (null if there is none). */
    static @Nullable Scorch scorch(ClientLevel level, Vec3 at, float yaw, long start) {
        float yr = yaw * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yr), fz = Mth.cos(yr);
        // The texture's bottom middle (v 1) is the body, a little before its feet; the wings spread out behind it.
        double cx = at.x + fx * 0.25, cz = at.z + fz * 0.25;
        if (Double.isNaN(surface(level, at.x, at.y, at.z))) return null;
        int g = Scorch.GRID;
        float[] h = new float[(g + 1) * (g + 1)];
        for (int j = 0; j <= g; j++) {
            for (int i = 0; i <= g; i++) {
                double u = (i / (double) g * 2 - 1) * Scorch.HALF, v = Scorch.along(j / (float) g);
                // u across (the angel's right), v from behind it (-HALF) to the body (0).
                double x = cx - fz * u + fx * v, z = cz + fx * u + fz * v;
                h[j * (g + 1) + i] = (float) (surface(level, x, at.y, z) - at.y);
            }
        }
        int index = SCORCHES.isEmpty() ? 0 : SCORCHES.getLast().index() + 1;
        return new Scorch(cx, at.y, cz, yaw, start, h, index);
    }

    /** The top of the ground under (x, z) near {@code baseY}, or NaN (a drop, or a wall). */
    private static double surface(ClientLevel level, double x, double baseY, double z) {
        int by = Mth.floor(baseY + 0.01);
        var pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int y = by + 1; y >= by - 2; y--) {
            pos.set(Mth.floor(x), y, Mth.floor(z));
            var shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (shape.isEmpty()) continue;
            double top = y + shape.max(net.minecraft.core.Direction.Axis.Y);
            return top > baseY + 1.05 ? Double.NaN : top;
        }
        return Double.NaN;
    }

    // --- drawing -------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = Minecraft.useShaderTransparency() ? RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                : RenderLevelStageEvent.Stage.AFTER_WEATHER;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            FallSky.render(event);
            return;
        }
        if (event.getStage() != stage) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || SMITES.isEmpty() && SCORCHES.isEmpty() && !FallSky.anyImpact()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var camera = event.getCamera();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        long time = level.getGameTime();
        FxDraw d = new FxDraw(event.getPoseStack(), buffers, camera, camera.getPosition(), partial, (time % 100000) + partial);
        for (Scorch s : SCORCHES) SmiteFx.scorch(d, s, time - s.start() + partial);
        buffers.endBatch();
        for (Smite s : SMITES.values()) SmiteFx.smite(d, mc, level, s, partial);
        FallSky.renderImpacts(d, time, partial);
        d.floor(Double.NaN);
        buffers.endBatch();
    }
}
