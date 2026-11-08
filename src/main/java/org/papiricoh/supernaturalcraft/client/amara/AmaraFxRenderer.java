package org.papiricoh.supernaturalcraft.client.amara;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.fx.BeamFx;
import org.papiricoh.supernaturalcraft.client.fx.TubeFx;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraLook;
import org.papiricoh.supernaturalcraft.network.AmaraFxPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Every {@link AmaraFxPayload} kind (v0.42): the client entry point ({@link #add}) sorts each payload to the screen
 * ({@link AmaraOverlay}: titles, voices, flashes, cracks), the post-processing ({@link ClientPostFx}: chromatic hits, waves),
 * her sky ({@link ClientAmara}: stage, gutter, the pull) or the world, where this class draws it from the tick it arrived:
 * the mass's tentacles, spikes, flares and rings (phase I), the girl's shadow steps and smoke, the woman's lifted debris and
 * drawn souls, Chuck's light and the chains that wound it, the ground crumbling and the islands sinking, the pull toward the
 * black sun, falling stars, walls of smoke and the last spiral of light and dark. Sprites and block chunks go through
 * {@link AmaraMotes}.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class AmaraFxRenderer {

    public static final ResourceLocation RING = SupernaturalCraft.asResource("textures/effect/energy_ring.png");
    /** Violet, her shadow's rim, the souls' pale blue, Chuck's light, star white, dawn gold. */
    static final int VIOLET = 0x8A5AE0, VIOLET_DEEP = 0x3A1F6A, SOUL = 0xBFE9FF, GOD = 0xFFF4D6, STAR = 0xEDE8FF, DAWN = 0xFFD58A,
            INK = 0x0A0610, GOLD = 0xFFCB5A;

    private record Active(AmaraFxPayload fx, long start) {
    }

    private static final List<Active> ACTIVE = new ArrayList<>();

    private AmaraFxRenderer() {
    }

    /** The client handler of {@link AmaraFxPayload} (also called by the previews). */
    public static void add(AmaraFxPayload fx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        switch (fx.kind()) {
            case AmaraFxPayload.TITLE -> AmaraOverlay.title(Math.round(fx.yaw()), fx.duration());
            case AmaraFxPayload.VOICE -> AmaraOverlay.voice(Math.round(fx.yaw()), fx.duration());
            case AmaraFxPayload.FLASH -> AmaraOverlay.flash(Math.round(fx.yaw()), fx.radius());
            case AmaraFxPayload.CHROMA_HIT -> ClientPostFx.hit(fx.radius());
            case AmaraFxPayload.SKY -> ClientAmara.sky(Math.round(fx.radius()), fx.duration());
            case AmaraFxPayload.SUN_DIE -> {
                float left = Mth.clamp(fx.radius(), 0, 1);
                ClientAmara.gutterPulse(0.55f + 0.45f * (1 - left));
                ClientPostFx.waveOut(0.4f + 0.4f * (1 - left));
            }
            default -> {
                // World effects; some also reach the screen.
                switch (fx.kind()) {
                    case AmaraFxPayload.CHUCK_WOUND -> {
                        ClientAmara.chains(fx.boss(), Math.max(20, fx.duration()));
                        ClientPostFx.pourInk(0.45f);
                        if (near(fx.point(), 48)) AmaraOverlay.cracks(Math.max(30, fx.duration()));
                    }
                    case AmaraFxPayload.GRAVITY -> ClientAmara.gravity(fx.point(), fx.duration());
                    case AmaraFxPayload.FOUND, AmaraFxPayload.SHATTER -> ClientPostFx.waveOut(fx.kind() == AmaraFxPayload.SHATTER ? 1f : 0.6f);
                    case AmaraFxPayload.RECONCILE -> ClientPostFx.waveOut(0.5f);
                    case AmaraFxPayload.CHUCK_LIGHT -> ACTIVE.removeIf(a -> a.fx.kind() == AmaraFxPayload.CHUCK_LIGHT && a.fx.boss() == fx.boss());
                    default -> {
                    }
                }
                Active a = new Active(fx, mc.level.getGameTime());
                ACTIVE.add(a);
                begin(mc.level, a);
            }
        }
    }

    private static boolean near(Vec3 at, double r) {
        var p = Minecraft.getInstance().player;
        return p != null && p.position().distanceToSqr(at) < r * r;
    }

    public static void clear() {
        ACTIVE.clear();
        AmaraMotes.clear();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            clear();
            return;
        }
        if (mc.isPaused()) return;
        long now = mc.level.getGameTime();
        ACTIVE.removeIf(a -> now - a.start > a.fx.duration() + 20);
        for (Active a : ACTIVE) tickFx(mc.level, a, (int) (now - a.start));
        if (ClientAmara.gravity(0) > 0 && mc.player != null) gravityStreaks(mc, ClientAmara.gravity(0));
        AmaraMotes.tick();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE.isEmpty() && AmaraMotes.count() == 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = mc.level.getGameTime();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        float time = (now + partial) / 20f;
        for (Active a : ACTIVE) {
            Entity e = mc.level.getEntity(a.fx.boss());
            float age = now - a.start + partial;
            float t = age / Math.max(1, a.fx.duration());
            draw(pose, buffers, e, a.fx, Mth.clamp(t, 0, 1.2f), age, cam, partial, time);
        }
        AmaraMotes.render(pose, buffers, event.getCamera(), partial);
        buffers.endBatch();
    }

    // --- where she is ----------------------------------------------------------------------------------------------------

    private static Vec3 body(Entity e, Vec3 fallback, float partial) {
        return e == null ? fallback : e.getPosition(partial);
    }

    /** Her mouth (the woman) or her core (the mass): where drawn souls go. */
    static Vec3 mouth(Entity e, Vec3 fallback, float partial) {
        if (e == null) return fallback.add(0, 2, 0);
        AmaraLook look = ClientAmara.look(e);
        Vec3 at = e.getPosition(partial);
        if (look == null || look.form() == AmaraLook.MASS) return at.add(0, AmaraEntity.MASS_CENTER, 0);
        AmaraAssets.Age age = look.age() == null ? AmaraAssets.Age.WOMAN : look.age();
        return at.add(0, AmaraAssets.FORM_HEIGHT * age.scale * 0.88, 0);
    }

    /** Her right hand, reaching forward a little. */
    static Vec3 hand(Entity e, Vec3 fallback, float partial) {
        if (e == null) return fallback.add(0, 1.2, 0);
        AmaraLook look = ClientAmara.look(e);
        float s = look == null || look.age() == null ? 1f : look.age().scale;
        float yaw = e.getViewYRot(partial) * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)), right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        return e.getPosition(partial).add(fwd.scale(0.45 * s)).add(right.scale(0.3 * s)).add(0, 1.05 * s, 0);
    }

    static Vec3 dir(float yawDeg) {
        float y = yawDeg * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(y), 0, Mth.cos(y));
    }

    private static Vec3 rnd(double spread) {
        var r = AmaraMotes.RANDOM;
        return new Vec3((r.nextDouble() - 0.5) * spread, (r.nextDouble() - 0.5) * spread, (r.nextDouble() - 0.5) * spread);
    }

    private static BlockState groundAt(ClientLevel level, double x, double z) {
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        return level.getBlockState(new BlockPos(Mth.floor(x), h - 1, Mth.floor(z)));
    }

    private static double groundY(ClientLevel level, double x, double z, double fallback) {
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        return h <= level.getMinBuildHeight() ? fallback : h;
    }

    // --- spawning: once on arrival -----------------------------------------------------------------------------------------

    private static void begin(ClientLevel level, Active a) {
        AmaraFxPayload fx = a.fx;
        Vec3 p = fx.point();
        var r = AmaraMotes.RANDOM;
        switch (fx.kind()) {
            case AmaraFxPayload.SHADOW_STEP -> puff(p, 14, 1.4f);
            case AmaraFxPayload.FOUND -> {
                puff(p.add(0, 0.8, 0), 34, 2.4f);
                for (int i = 0; i < 18; i++) {
                    AmaraMotes.add(AmaraMotes.Sprite.INK, p.add(0, 1.2, 0), rnd(0.9).add(0, 0.25, 0), 0.03f, 0.25f, 0xFFFFFF, 26, false);
                }
            }
            case AmaraFxPayload.TELEKINESIS -> {
                int n = Mth.clamp(Math.round(fx.radius() * 2.2f), 6, 18);
                int hold = Math.max(10, Math.round(fx.duration() * 0.7f));
                Vec3 fling = dir(fx.yaw()).scale(1.3).add(0, 0.25, 0);
                for (int i = 0; i < n; i++) {
                    double ang = r.nextDouble() * Mth.TWO_PI, d = Math.sqrt(r.nextDouble()) * fx.radius();
                    double x = p.x + Math.cos(ang) * d, z = p.z + Math.sin(ang) * d;
                    double y = groundY(level, x, z, p.y);
                    AmaraMotes.Chunk c = AmaraMotes.chunk(groundAt(level, x, z), new Vec3(x, y - 0.3, z),
                            new Vec3(0, 0.18 + r.nextDouble() * 0.2, 0), 0.45f + r.nextFloat() * 0.4f, 0.05f, hold + 40);
                    c.holdUntil = hold;
                    c.fx = fling.x + (r.nextDouble() - 0.5) * 0.4;
                    c.fy = fling.y + r.nextDouble() * 0.2;
                    c.fz = fling.z + (r.nextDouble() - 0.5) * 0.4;
                }
            }
            case AmaraFxPayload.CRUMBLE -> {
                int n = Mth.clamp(Math.round(fx.radius() * 2.6f), 10, 70);
                for (int i = 0; i < n; i++) {
                    double ang = Mth.TWO_PI * i / n + r.nextDouble() * 0.1;
                    double x = p.x + Math.cos(ang) * fx.radius(), z = p.z + Math.sin(ang) * fx.radius();
                    double y = groundY(level, x, z, p.y);
                    if (i % 2 == 0) {
                        AmaraMotes.chunk(groundAt(level, x, z), new Vec3(x, y - 0.5, z), new Vec3((r.nextDouble() - 0.5) * 0.08,
                                0.05 + r.nextDouble() * 0.12, (r.nextDouble() - 0.5) * 0.08), 0.4f + r.nextFloat() * 0.35f, 0.04f, 50 + r.nextInt(30));
                    }
                    for (int k = 0; k < 2; k++) {
                        AmaraMotes.add(AmaraMotes.Sprite.ASH, new Vec3(x, y, z).add(rnd(1)), new Vec3(0, 0.04 + r.nextDouble() * 0.06, 0)
                                .add(rnd(0.05)), -0.002f, 0.35f, 0x1A1424, 40 + r.nextInt(30), false);
                    }
                }
            }
            case AmaraFxPayload.ISLAND_SINK -> dustCurtain(level, p, fx.radius(), 2);
            case AmaraFxPayload.SHATTER -> {
                for (int i = 0; i < 70; i++) {
                    Vec3 off = rnd(2).normalize().scale(4 + r.nextDouble() * 5);
                    AmaraMotes.Mote m = AmaraMotes.smoke(p.add(0, AmaraEntity.MASS_CENTER, 0).add(off), Vec3.ZERO, 1.6f, 70);
                    m.home = p.add(0, 1.0, 0);
                }
                for (int i = 0; i < 30; i++) {
                    AmaraMotes.add(AmaraMotes.Sprite.VOID, p.add(0, AmaraEntity.MASS_CENTER, 0), rnd(1.6), 0, 0.5f, 0xFFFFFF, 30, false);
                }
            }
            case AmaraFxPayload.RECONCILE -> {
                for (int i = 0; i < 40; i++) {
                    AmaraMotes.add(AmaraMotes.Sprite.DAWN, p.add(rnd(3)), new Vec3(0, 0.05 + r.nextDouble() * 0.05, 0), -0.001f, 0.35f, DAWN,
                            fx.duration(), true);
                }
            }
            default -> {
            }
        }
    }

    private static void puff(Vec3 at, int n, float spread) {
        var r = AmaraMotes.RANDOM;
        for (int i = 0; i < n; i++) {
            Vec3 v = new Vec3((r.nextDouble() - 0.5) * 0.2 * spread, 0.02 + r.nextDouble() * 0.08, (r.nextDouble() - 0.5) * 0.2 * spread);
            AmaraMotes.smoke(at.add((r.nextDouble() - 0.5) * spread, r.nextDouble() * 1.4, (r.nextDouble() - 0.5) * spread), v,
                    0.8f + r.nextFloat() * 0.7f, 26 + r.nextInt(18));
        }
    }

    private static void dustCurtain(ClientLevel level, Vec3 c, float radius, int perStep) {
        var r = AmaraMotes.RANDOM;
        int n = Mth.clamp(Math.round(radius * 1.6f), 8, 48) * perStep;
        for (int i = 0; i < n; i++) {
            double ang = r.nextDouble() * Mth.TWO_PI;
            double x = c.x + Math.cos(ang) * radius, z = c.z + Math.sin(ang) * radius;
            double y = groundY(level, x, z, c.y);
            AmaraMotes.add(AmaraMotes.Sprite.ASH, new Vec3(x, y - 0.4, z), new Vec3(0, -0.05 - r.nextDouble() * 0.1, 0), 0.01f,
                    0.5f + r.nextFloat() * 0.4f, 0x4A4256, 30 + r.nextInt(20), false);
            if (i % 5 == 0) {
                AmaraMotes.chunk(groundAt(level, x, z), new Vec3(x, y - 0.8, z), new Vec3(0, -0.05, 0), 0.3f + r.nextFloat() * 0.3f, 0.04f, 40);
            }
        }
    }

    // --- spawning: every tick while it lasts ---------------------------------------------------------------------------------

    private static void tickFx(ClientLevel level, Active a, int age) {
        AmaraFxPayload fx = a.fx;
        if (age > fx.duration()) return;
        Vec3 p = fx.point();
        var r = AmaraMotes.RANDOM;
        Entity e = level.getEntity(fx.boss());
        switch (fx.kind()) {
            case AmaraFxPayload.SHADOW_STEP -> {
                if (age == fx.duration() / 2) puff(p.add(dir(fx.yaw()).scale(fx.radius())), 18, 1.6f);
            }
            case AmaraFxPayload.HIDE -> {
                if (age % 2 == 0) {
                    double ang = age * 0.05;
                    Vec3 c = p.add(Math.cos(ang) * 1.2, 0.6, Math.sin(ang) * 1.2);
                    for (int i = 0; i < 3; i++) AmaraMotes.smoke(c.add(rnd(1.4)), rnd(0.04), 1.1f + r.nextFloat() * 0.6f, 30);
                }
            }
            case AmaraFxPayload.TELEKINESIS -> {
                if (age < fx.duration() * 0.7f && age % 2 == 0) {
                    AmaraMotes.add(AmaraMotes.Sprite.VOID, p.add(rnd(fx.radius())).add(0, 0.2, 0), new Vec3(0, 0.08, 0), 0, 0.25f, 0xB48CFF, 20, true);
                }
            }
            case AmaraFxPayload.SOUL_DRAIN -> {
                Vec3 to = mouth(e, p, 0);
                for (int i = 0; i < 2; i++) {
                    AmaraMotes.Mote m = AmaraMotes.add(AmaraMotes.Sprite.SOUL, p.add(0, 1.1, 0).add(rnd(0.5)), rnd(0.15).add(0, 0.05, 0), 0,
                            0.32f, SOUL, 60, true);
                    m.home = to;
                }
            }
            case AmaraFxPayload.CHUCK_LIGHT -> {
                if (age % 3 == 0 && fx.radius() > 0.05f) {
                    AmaraMotes.add(AmaraMotes.Sprite.DAWN, p.add(rnd(1.6)).add(0, 0.4, 0), new Vec3(0, 0.06 + 0.06 * fx.radius(), 0), -0.001f,
                            0.3f, GOD, 40, true);
                }
            }
            case AmaraFxPayload.CHUCK_WOUND -> {
                if (age % 2 == 0) {
                    Vec3 from = hand(e, p, 0), to = p.add(0, 1.3, 0);
                    double u = r.nextDouble();
                    AmaraMotes.smoke(from.lerp(to, u).add(rnd(0.3)), rnd(0.02), 0.35f, 16);
                    if (r.nextInt(3) == 0) AmaraMotes.add(AmaraMotes.Sprite.DAWN, to.add(rnd(0.6)), rnd(0.12), 0.002f, 0.2f, GOD, 14, true);
                }
            }
            case AmaraFxPayload.CRUMBLE -> {
                if (age % 3 == 0) {
                    double ang = r.nextDouble() * Mth.TWO_PI;
                    double x = p.x + Math.cos(ang) * fx.radius(), z = p.z + Math.sin(ang) * fx.radius();
                    AmaraMotes.add(AmaraMotes.Sprite.ASH, new Vec3(x, groundY(level, x, z, p.y), z), new Vec3(0, 0.06, 0).add(rnd(0.04)),
                            -0.002f, 0.3f, 0x1A1424, 40, false);
                }
            }
            case AmaraFxPayload.ISLAND_SINK -> {
                if (age < 24 && age % 4 == 0) dustCurtain(level, p, fx.radius(), 1);
            }
            case AmaraFxPayload.SMOKE_WALL -> {
                Vec3 along = dir(fx.yaw());
                if (age % 2 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double u = (r.nextDouble() - 0.5) * fx.radius();
                        Vec3 at = p.add(along.scale(u));
                        AmaraMotes.smoke(at.add(0, r.nextDouble() * 3.5, 0), new Vec3(0, 0.02, 0).add(rnd(0.03)), 1.4f + r.nextFloat(), 34);
                    }
                }
            }
            case AmaraFxPayload.STARFALL -> {
                if (age == fx.duration() - 1) {
                    for (int i = 0; i < 26; i++) {
                        AmaraMotes.add(AmaraMotes.Sprite.STAR, p.add(0, 0.4, 0), rnd(1.2).add(0, 0.35, 0), 0.03f, 0.35f, STAR, 30, true);
                    }
                    for (int i = 0; i < 10; i++) AmaraMotes.smoke(p.add(rnd(fx.radius())), new Vec3(0, 0.06, 0), 1.2f, 30);
                    if (near(p, 24)) {
                        ClientPostFx.hit(0.35f);
                        AmaraOverlay.flash(1, 0.25f);
                    }
                }
            }
            case AmaraFxPayload.RECONCILE -> {
                if (age % 2 == 0) {
                    float k = age / (float) Math.max(1, fx.duration());
                    AmaraMotes.add(AmaraMotes.Sprite.DAWN, p.add(rnd(1.5)), new Vec3(0, 0.12, 0), -0.002f, 0.3f, k > 0.5f ? DAWN : GOD, 50, true);
                    if (k < 0.6f) AmaraMotes.add(AmaraMotes.Sprite.INK, p.add(0, 2.5, 0).add(rnd(1.5)), new Vec3(0, -0.05, 0), 0.01f, 0.22f, 0xFFFFFF, 30, false);
                }
            }
            default -> {
            }
        }
    }

    /** Phase V's pull: streaks of dust and starlight rising round the viewer toward the black sun. */
    private static void gravityStreaks(Minecraft mc, float k) {
        var r = AmaraMotes.RANDOM;
        int n = Math.round(4 * k);
        Vec3 c = mc.player.position();
        for (int i = 0; i < n; i++) {
            Vec3 at = c.add((r.nextDouble() - 0.5) * 24, r.nextDouble() * 6 - 2, (r.nextDouble() - 0.5) * 24);
            boolean star = r.nextInt(3) == 0;
            AmaraMotes.add(star ? AmaraMotes.Sprite.STAR : AmaraMotes.Sprite.ASH, at, new Vec3(0, 0.35 + r.nextDouble() * 0.35, 0),
                    -0.02f, star ? 0.2f : 0.28f, star ? STAR : 0x2A2238, 40, star);
        }
    }

    // --- drawing ---------------------------------------------------------------------------------------------------------

    private static void draw(PoseStack pose, MultiBufferSource buffers, Entity e, AmaraFxPayload fx, float t, float age, Vec3 cam,
                             float partial, float time) {
        Vec3 p = fx.point();
        Vec3 body = body(e, p, partial);
        switch (fx.kind()) {
            case AmaraFxPayload.SLAM, AmaraFxPayload.SWEEP, AmaraFxPayload.GRASP, AmaraFxPayload.SPIKE, AmaraFxPayload.FLARE,
                 AmaraFxPayload.RING_OUT, AmaraFxPayload.RING_IN -> massAttack(pose, buffers, body, fx, t, cam, time);
            case AmaraFxPayload.SHADOW_STEP -> {
                float out = t < 0.5f ? 1 - t * 2 : 0, in = t >= 0.5f ? Math.min(1, (t - 0.5f) * 4) * (t > 0.85f ? (1 - t) / 0.15f : 1) : 0;
                pool(pose, buffers, p, cam, 1.4f * (0.4f + out * 0.6f), out);
                pool(pose, buffers, p.add(dir(fx.yaw()).scale(fx.radius())), cam, 1.4f * (0.4f + in * 0.6f), in);
            }
            case AmaraFxPayload.FOUND -> {
                float k = Math.min(1, t * 3);
                TubeFx.ring(pose, buffers, RING, p.add(0, 0.05, 0).subtract(cam), Math.max(0.4f, fx.radius() * k), 0.9f, 1.4f, VIOLET,
                        (1 - k) * 0.9f, time);
            }
            case AmaraFxPayload.TELEKINESIS -> {
                float k = t < 0.7f ? Math.min(1, t * 5) : Math.max(0, 1 - (t - 0.7f) * 5);
                TubeFx.ring(pose, buffers, RING, p.add(0, 0.05, 0).subtract(cam), Math.max(0.5f, fx.radius()), 0.5f, 0.8f + 0.4f * Mth.sin(time * 5),
                        0xB48CFF, 0.55f * k, time);
            }
            case AmaraFxPayload.SOUL_DRAIN -> {
                Vec3 from = p.add(0, 1.1, 0), to = mouth(e, p, partial);
                float k = Math.min(1, t * 4) * Math.min(1, (1 - t) * 6 + 0.001f);
                BeamFx.draw(pose, buffers, from.subtract(cam), to.subtract(cam), 0.18f + 0.05f * Mth.sin(time * 9), SOUL, 0.45f * k, time);
            }
            case AmaraFxPayload.CHUCK_LIGHT -> {
                float light = Mth.clamp(fx.radius(), 0, 1);
                float fade = Math.min(1, age / 10f) * Math.min(1, (fx.duration() + 20 - age) / 20f);
                if (light > 0.01f && fade > 0) {
                    Vec3 base = p.subtract(cam);
                    BeamFx.draw(pose, buffers, base, base.add(0, 40, 0), 0.6f + 1.4f * light + 0.1f * Mth.sin(time * 3), GOD, 0.75f * light * fade, time);
                    // A few rays turning about him.
                    for (int i = 0; i < 6; i++) {
                        float a = time * 0.4f + i * Mth.TWO_PI / 6;
                        Vec3 tip = base.add(Mth.cos(a) * 5 * light, 1.6 + 2.2 * light, Mth.sin(a) * 5 * light);
                        BeamFx.draw(pose, buffers, base.add(0, 1.3, 0), tip, 0.25f * light, DAWN, 0.35f * light * fade, time + i);
                    }
                    TubeFx.ring(pose, buffers, RING, base.add(0, 0.05, 0), 1.5f + light * 1.5f, 0.6f, 0.5f, GOD, 0.5f * light * fade, time);
                }
            }
            case AmaraFxPayload.CHUCK_WOUND -> {
                Vec3 from = hand(e, p, partial), to = p.add(0, 1.3, 0);
                float k = Math.min(1, t * 5);
                Vec3 tip = from.lerp(to, k);
                Vec3 mid = from.lerp(tip, 0.5).add(0, 0.6 * (1 - k) + 0.3f * Mth.sin(time * 4), 0);
                TubeFx.tentacle(pose, buffers, from.subtract(cam), mid.subtract(cam), tip.subtract(cam), 0.14f, 0.12f, time);
                chainLinks(pose, buffers, from, mid, tip, cam, time, Math.max(0, 1 - Math.max(0, t - 0.85f) * 6));
            }
            case AmaraFxPayload.CRUMBLE -> {
                float k = t < 0.2f ? t / 0.2f : Math.max(0, 1 - (t - 0.2f) / 0.8f);
                TubeFx.ring(pose, buffers, RING, p.add(0, 0.08, 0).subtract(cam), fx.radius() + 0.6f, 1.6f, 0.6f, VIOLET_DEEP, 0.7f * k, time);
            }
            case AmaraFxPayload.ISLAND_SINK -> {
                float k = Math.max(0, 1 - t * 1.5f);
                TubeFx.ring(pose, buffers, RING, p.add(0, -0.4, 0).subtract(cam), fx.radius(), 0.8f, 2.4f, 0x4A3A6A, 0.5f * k, time);
            }
            case AmaraFxPayload.GRAVITY -> {
                // A column of pull over the arena's heart.
                float k = Math.min(1, t * 4) * Math.min(1, (1 - t) * 4 + 0.001f);
                Vec3 base = p.subtract(cam);
                for (int i = 0; i < 5; i++) {
                    float a = -time * 1.4f + i * Mth.TWO_PI / 5;
                    float rr = 3 + 2 * Mth.sin(time + i);
                    BeamFx.draw(pose, buffers, base.add(Mth.cos(a) * rr, 0, Mth.sin(a) * rr), base.add(Mth.cos(a + 1.2f) * rr * 0.4, 60, Mth.sin(a + 1.2f) * rr * 0.4),
                            0.35f, VIOLET, 0.35f * k, time + i);
                }
            }
            case AmaraFxPayload.STARFALL -> {
                float k = Mth.clamp(t, 0, 1);
                TubeFx.ring(pose, buffers, RING, p.add(0, 0.05, 0).subtract(cam), Math.max(0.6f, fx.radius()), 0.35f, 0.4f + k * 0.8f, GOLD,
                        0.4f + 0.5f * k, time);
                if (t < 1) {
                    Vec3 from = p.add(18, 70, -10), head = from.lerp(p, k * k), tail = from.lerp(p, Math.max(0, k * k - 0.12f));
                    BeamFx.draw(pose, buffers, tail.subtract(cam), head.subtract(cam), 0.6f + k, STAR, 0.9f, time);
                }
            }
            case AmaraFxPayload.SMOKE_WALL -> {
                float k = Math.min(1, t * 6) * Math.min(1, (1 - t) * 5 + 0.001f);
                curtain(pose, buffers, p, dir(fx.yaw()), fx.radius(), 4.2f * Math.min(1, t * 4), cam, k);
            }
            case AmaraFxPayload.RECONCILE -> {
                float k = Mth.clamp(t, 0, 1);
                Vec3 base = p.subtract(cam);
                // Two strands, light and dark, spiralling up together; then rays of dawn.
                for (int s = 0; s < 2; s++) {
                    List<Vec3> pts = new ArrayList<>();
                    float[] radii = new float[24];
                    for (int i = 0; i < 24; i++) {
                        float u = i / 23f;
                        float a = u * 9 + time * 2 + s * Mth.PI;
                        float rr = 1.6f * (1 - u) * (1 - k * 0.5f);
                        pts.add(base.add(Mth.cos(a) * rr, u * 6 * Math.min(1, k * 2), Mth.sin(a) * rr));
                        radii[i] = 0.18f * (1 - u * 0.7f);
                    }
                    if (s == 0) TubeFx.tube(pose, buffers, pts, radii, time);
                    else {
                        for (int i = 1; i < pts.size(); i++) {
                            BeamFx.draw(pose, buffers, pts.get(i - 1), pts.get(i), 0.22f, GOD, 0.8f * (1 - k * 0.3f), time);
                        }
                    }
                }
                if (k > 0.45f) {
                    float rk = (k - 0.45f) / 0.55f;
                    for (int i = 0; i < 10; i++) {
                        float a = i * Mth.TWO_PI / 10 + time * 0.2f;
                        BeamFx.draw(pose, buffers, base.add(0, 3, 0), base.add(Mth.cos(a) * 24 * rk, 3 + 12 * rk, Mth.sin(a) * 24 * rk),
                                0.6f, DAWN, 0.5f * (1 - Math.max(0, rk - 0.7f) / 0.3f), time + i);
                    }
                }
            }
            case AmaraFxPayload.SHATTER -> {
                float k = Mth.clamp(t, 0, 1);
                Vec3 c = p.add(0, AmaraEntity.MASS_CENTER * (1 - k) + 1 * k, 0);
                TubeFx.ring(pose, buffers, RING, new Vec3(c.x, p.y + 0.05, c.z).subtract(cam), Math.max(0.3f, 12 * (1 - k)), 1.2f, 2.5f,
                        VIOLET, 0.9f * (1 - k), time);
            }
            default -> {
            }
        }
    }

    /** Phase I, as before: tentacles that rise and slam, sweep and grasp; spikes; the turning flare; rings of black light. */
    private static void massAttack(PoseStack pose, MultiBufferSource buffers, Vec3 body, AmaraFxPayload fx, float t, Vec3 cam, float time) {
        Vec3 core = body.add(0, AmaraEntity.MASS_CENTER, 0);
        Vec3 p = fx.point();
        Vec3 root = rootOf(body, p);
        switch (fx.kind()) {
            case AmaraFxPayload.SLAM -> {
                float lift = t < 0.6f ? t / 0.6f : t < 0.7f ? 1 - (t - 0.6f) / 0.1f : 0;
                float reach = t > 0.95f ? Math.max(0, 1 - (t - 0.95f) * 8) : 1;
                Vec3 tip = root.lerp(p.add(0, lift * 7, 0), reach);
                Vec3 ctrl = root.lerp(tip, 0.5).add(0, 4 + lift * 3, 0);
                TubeFx.tentacle(pose, buffers, root.subtract(cam), ctrl.subtract(cam), tip.subtract(cam), 0.9f, 0.4f, time);
            }
            case AmaraFxPayload.SWEEP -> {
                float a = (fx.yaw() + t * 360f) * Mth.DEG_TO_RAD;
                Vec3 tip = body.add(Mth.cos(a) * fx.radius(), 0.4 - (body.y - p.y), Mth.sin(a) * fx.radius());
                Vec3 r0 = rootOf(body, tip);
                Vec3 ctrl = r0.lerp(tip, 0.5).add(0, 1.5, 0);
                TubeFx.tentacle(pose, buffers, r0.subtract(cam), ctrl.subtract(cam), tip.subtract(cam), 1.0f, 0.2f, time);
            }
            case AmaraFxPayload.GRASP -> {
                float reach = Math.min(1, t * 6);
                Vec3 tip = root.lerp(p.add(0, 1.0, 0), reach);
                Vec3 ctrl = root.lerp(tip, 0.5).add(0, 3, 0);
                TubeFx.tentacle(pose, buffers, root.subtract(cam), ctrl.subtract(cam), tip.subtract(cam), 0.7f, 0.15f, time);
                if (reach >= 1) TubeFx.ring(pose, buffers, RING, p.add(0, 0.9, 0).subtract(cam), 0.8f, 0.3f, 0.6f, 0x6A3FA8, 0.8f, time);
            }
            case AmaraFxPayload.SPIKE -> {
                float h = t < 0.3f ? t / 0.3f : t > 0.8f ? Math.max(0, 1 - (t - 0.8f) * 5) : 1;
                Vec3 top = p.add(0, 3.5 * h, 0);
                TubeFx.tube(pose, buffers, List.of(p.subtract(cam), p.add(0, 1.2 * h, 0).subtract(cam), top.subtract(cam)),
                        new float[]{0.7f, 0.45f, 0.02f}, time);
            }
            case AmaraFxPayload.FLARE -> {
                float a = (fx.yaw() + t * 360f) * Mth.DEG_TO_RAD;
                Vec3 from = core.subtract(cam);
                Vec3 to = new Vec3(core.x + Mth.cos(a) * fx.radius(), p.y + 0.6, core.z + Mth.sin(a) * fx.radius()).subtract(cam);
                BeamFx.draw(pose, buffers, from, to, 0.9f, 0xFFE9B8, 0.95f, time);
            }
            default -> {
                float k = fx.kind() == AmaraFxPayload.RING_OUT ? t : 1 - t;
                float r = Math.max(0.5f, fx.radius() * k);
                TubeFx.ring(pose, buffers, RING, new Vec3(core.x, p.y + 0.05, core.z).subtract(cam), r, 1.2f, 2.5f,
                        fx.kind() == AmaraFxPayload.RING_OUT ? VIOLET : 0x2A1240, 0.9f * (1.1f - t * 0.5f), time);
            }
        }
    }

    private static Vec3 rootOf(Vec3 body, Vec3 toward) {
        Vec3 flat = toward.subtract(body).multiply(1, 0, 1);
        Vec3 dir = flat.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : flat.normalize();
        return body.add(dir.scale(2.2)).add(0, AmaraEntity.MASS_CENTER - 2.4, 0);
    }

    /** A pool of her shadow on the ground: a black disc with a violet rim. */
    private static void pool(PoseStack pose, MultiBufferSource buffers, Vec3 at, Vec3 cam, float radius, float alpha) {
        if (alpha <= 0.01f) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f m = pose.last().pose();
        float y = (float) (at.y + 0.04 - cam.y), cx = (float) (at.x - cam.x), cz = (float) (at.z - cam.z);
        int n = 24, core = (int) (alpha * 235), rim = (int) (alpha * 160);
        for (int i = 0; i < n; i++) {
            float a0 = Mth.TWO_PI * i / n, a1 = Mth.TWO_PI * (i + 1) / n;
            float r0 = radius * (0.92f + 0.08f * Mth.sin(a0 * 5)), r1 = radius * (0.92f + 0.08f * Mth.sin(a1 * 5));
            vc.addVertex(m, cx, y, cz).setColor(4, 2, 8, core);
            vc.addVertex(m, cx, y, cz).setColor(4, 2, 8, core);
            vc.addVertex(m, cx + Mth.cos(a1) * r1, y, cz + Mth.sin(a1) * r1).setColor(20, 8, 36, core);
            vc.addVertex(m, cx + Mth.cos(a0) * r0, y, cz + Mth.sin(a0) * r0).setColor(20, 8, 36, core);
            vc.addVertex(m, cx + Mth.cos(a0) * r0, y + 0.002f, cz + Mth.sin(a0) * r0).setColor(138, 90, 224, rim);
            vc.addVertex(m, cx + Mth.cos(a1) * r1, y + 0.002f, cz + Mth.sin(a1) * r1).setColor(138, 90, 224, rim);
            vc.addVertex(m, cx + Mth.cos(a1) * r1 * 1.12f, y + 0.002f, cz + Mth.sin(a1) * r1 * 1.12f).setColor(138, 90, 224, 0);
            vc.addVertex(m, cx + Mth.cos(a0) * r0 * 1.12f, y + 0.002f, cz + Mth.sin(a0) * r0 * 1.12f).setColor(138, 90, 224, 0);
        }
    }

    /** Smoke chains: dark links along the chain's curve, each glinting violet. */
    private static void chainLinks(PoseStack pose, MultiBufferSource buffers, Vec3 a, Vec3 c, Vec3 b, Vec3 cam, float time, float alpha) {
        if (alpha <= 0.01f) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f m = pose.last().pose();
        int n = 14;
        for (int i = 0; i <= n; i++) {
            float u = i / (float) n;
            Vec3 at = a.scale((1 - u) * (1 - u)).add(c.scale(2 * (1 - u) * u)).add(b.scale(u * u)).subtract(cam);
            Vec3 next = a.scale((1 - u - 0.02) * (1 - u - 0.02)).add(c.scale(2 * (1 - u - 0.02) * (u + 0.02))).add(b.scale((u + 0.02) * (u + 0.02))).subtract(cam);
            Vec3 along = next.subtract(at);
            along = along.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : along.normalize();
            Vec3 side = (i % 2 == 0 ? along.cross(new Vec3(0, 1, 0)) : new Vec3(0, 1, 0));
            side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize();
            float w = 0.11f, l = 0.2f;
            Vec3 p0 = at.add(along.scale(-l)).add(side.scale(-w)), p1 = at.add(along.scale(-l)).add(side.scale(w)),
                    p2 = at.add(along.scale(l)).add(side.scale(w)), p3 = at.add(along.scale(l)).add(side.scale(-w));
            int glint = (int) (alpha * (120 + 100 * Math.max(0, Mth.sin(time * 6 - u * 10))));
            for (int pass = 0; pass < 2; pass++) {
                int r = pass == 0 ? 10 : 138, g = pass == 0 ? 6 : 90, bl = pass == 0 ? 18 : 224, al = pass == 0 ? (int) (alpha * 230) : glint;
                float s = pass == 0 ? 1f : 0.55f;
                Vec3 q0 = at.add(p0.subtract(at).scale(s)), q1 = at.add(p1.subtract(at).scale(s)), q2 = at.add(p2.subtract(at).scale(s)),
                        q3 = at.add(p3.subtract(at).scale(s));
                vc.addVertex(m, (float) q0.x, (float) q0.y, (float) q0.z).setColor(r, g, bl, al);
                vc.addVertex(m, (float) q1.x, (float) q1.y, (float) q1.z).setColor(r, g, bl, al);
                vc.addVertex(m, (float) q2.x, (float) q2.y, (float) q2.z).setColor(r, g, bl, al);
                vc.addVertex(m, (float) q3.x, (float) q3.y, (float) q3.z).setColor(r, g, bl, al);
            }
        }
    }

    /** A wall of smoke: a dark curtain along {@code along}, its top ragged and fading. */
    private static void curtain(PoseStack pose, MultiBufferSource buffers, Vec3 c, Vec3 along, float length, float height, Vec3 cam, float alpha) {
        if (alpha <= 0.01f || height <= 0.01f) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f m = pose.last().pose();
        int n = Math.max(4, Math.round(length * 2));
        for (int i = 0; i < n; i++) {
            float u0 = -0.5f + i / (float) n, u1 = -0.5f + (i + 1) / (float) n;
            Vec3 a = c.add(along.scale(u0 * length)).subtract(cam), b = c.add(along.scale(u1 * length)).subtract(cam);
            float h0 = height * (0.75f + 0.25f * Mth.sin(i * 1.7f)), h1 = height * (0.75f + 0.25f * Mth.sin((i + 1) * 1.7f));
            int al = (int) (alpha * 200);
            vc.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setColor(6, 3, 10, al);
            vc.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setColor(6, 3, 10, al);
            vc.addVertex(m, (float) b.x, (float) (b.y + h1), (float) b.z).setColor(26, 14, 42, 0);
            vc.addVertex(m, (float) a.x, (float) (a.y + h0), (float) a.z).setColor(26, 14, 42, 0);
        }
    }
}
