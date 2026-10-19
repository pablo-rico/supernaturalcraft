package org.papiricoh.supernaturalcraft.client.witchboss;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicEffect;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.client.mark.MarkArm;
import org.papiricoh.supernaturalcraft.client.mark.MarkGlyph;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.HashMap;
import java.util.Map;

/**
 * The Mark of Cain coming off (v0.21, P6: {@code Unmarking}, Rowena's or the Grand Witch's cure): the client sees a bearer it was
 * watching lose the Mark and plays the beat itself — the rune lifts off the forearm in red light, hangs a moment burning, then
 * cracks apart and crumbles to ash and embers. In first person it peels off the arm held before the camera. No payload of its own:
 * the server's seal, flare and protecting hex play alongside.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class UnmarkFx {

    /** Players seen bearing the Mark last tick (by entity id). */
    private static final Map<Integer, Boolean> BORNE = new HashMap<>();
    private static ClientLevel seenIn;

    private UnmarkFx() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            BORNE.clear();
            return;
        }
        if (level != seenIn) {
            BORNE.clear();
            seenIn = level;
        }
        Map<Integer, Boolean> now = new HashMap<>();
        for (AbstractClientPlayer p : level.players()) {
            if (p.distanceToSqr(mc.player) > 64 * 64) continue;
            boolean marked = MarkArm.marked(p);
            now.put(p.getId(), marked);
            if (!marked && Boolean.TRUE.equals(BORNE.get(p.getId())) && p.isAlive()) peel(p);
        }
        BORNE.clear();
        BORNE.putAll(now);
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        BORNE.clear();
        seenIn = null;
    }

    /** Plays the Mark peeling off {@code p}'s arm (previews call it too). */
    public static void peel(AbstractClientPlayer p) {
        MagicFxManager.add(new Peel(p.getId()));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == p) MagicOverlay.flash(p.position(), 0xB0202A, 0.18f, 8);
    }

    /** The rune lifting off, burning, cracking and crumbling. */
    static final class Peel extends MagicEffect {
        private static final int LIFT = 16, HOLD = 14, CRUMBLE = 30;
        private static final int HOT = 0xFFB070, RED = 0xD0141E, ASH = 0x9A8C80;
        private final int player;
        private final long seed;

        Peel(int player) {
            super(Vec3.ZERO, School.INFERNAL, LIFT + HOLD + CRUMBLE);
            this.player = player;
            this.seed = player * 7919L + System.nanoTime();
        }

        @Override
        protected Vec3 anchor() {
            Entity e = WitchFxKit.entity(player);
            return e != null ? e.position() : at;
        }

        @Override
        protected double reach() {
            return 4;
        }

        @Override
        protected double floor(net.minecraft.client.multiplayer.ClientLevel level) {
            return Double.NaN;
        }

        private boolean firstPerson(Entity e) {
            Minecraft mc = Minecraft.getInstance();
            return e == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON && mc.getCameraEntity() == mc.player;
        }

        /** Where the Mark sits on the arm now, and which way is "out" from it. */
        private Vec3[] arm(Entity e, float partial) {
            if (firstPerson(e)) {
                Minecraft mc = Minecraft.getInstance();
                var cam = mc.gameRenderer.getMainCamera();
                Vector3f look = cam.getLookVector(), up = cam.getUpVector(), left = cam.getLeftVector();
                Vec3 eye = cam.getPosition();
                Vec3 p = eye.add(look.x * 0.75 - left.x * 0.32 - up.x * 0.3, look.y * 0.75 - left.y * 0.32 - up.y * 0.3,
                        look.z * 0.75 - left.z * 0.32 - up.z * 0.3);
                return new Vec3[]{p, new Vec3(-look.x, -look.y, -look.z).add(0, 0.6, 0).normalize()};
            }
            float yaw = (e instanceof AbstractClientPlayer pl ? Mth.rotLerp(partial, pl.yBodyRotO, pl.yBodyRot) : e.getYRot()) * Mth.DEG_TO_RAD;
            // Right of the facing is (-cos, -sin); the forearm hangs at about the hip.
            double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);
            Vec3 feet = e.getPosition(partial);
            return new Vec3[]{feet.add(rx * 0.38, 0.95, rz * 0.38), new Vec3(rx, 0.4, rz).normalize()};
        }

        @Override
        protected void tick(net.minecraft.client.multiplayer.ClientLevel level) {
            Entity e = WitchFxKit.entity(player);
            if (e == null) return;
            RandomSource r = level.random;
            Vec3[] a = arm(e, 1f);
            float t = age;
            Vec3 c = a[0].add(a[1].scale(0.5 * easeOut(Math.min(1, t / LIFT))));
            if (age < LIFT && r.nextFloat() < 0.6f) {
                FxParticles.ember(level, r.nextBoolean() ? RED : HOT, 0.4f, 14, a[0].x, a[0].y, a[0].z, a[1].x * 0.03, 0.02, a[1].z * 0.03);
            }
            if (age >= LIFT + HOLD) {
                for (int i = FxParticles.rate(r, 2.5f); i > 0; i--) {
                    FxParticles.plain(level, AllParticles.ASH.get(), c.x + FxParticles.j(r) * 0.12, c.y + FxParticles.j(r) * 0.15, c.z + FxParticles.j(r) * 0.12,
                            0, -0.02, 0);
                }
                if (r.nextFloat() < 0.5f) {
                    FxParticles.ember(level, HOT, 0.35f, 12, c.x, c.y, c.z, FxParticles.j(r) * 0.03, 0.02, FxParticles.j(r) * 0.03);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            Entity e = WitchFxKit.entity(player);
            if (e == null) return;
            float t = time(d);
            Vec3[] a = arm(e, d.partial);
            float lift = easeOut(Math.min(1, t / LIFT)), crumble = Mth.clamp((t - LIFT - HOLD) / CRUMBLE, 0, 1);
            boolean fp = firstPerson(e);
            float size = fp ? 0.22f : 0.3f;
            Vec3 c = a[0].add(a[1].scale((fp ? 0.25 : 0.5) * lift)).add(0, 0.25 * lift + (fp ? 0.05 : 0.1) * crumble, 0);
            float burn = 0.75f + 0.25f * Mth.sin(t * 1.3f);
            int col = WitchBossOverlay.lerpRgb(HOT, ASH, Mth.clamp(crumble * 1.5f, 0, 1));
            float alpha = (1 - crumble * crumble) * Mth.clamp(t / 3f, 0, 1);
            // Red light round it as it tears away.
            d.glowDisc(c.x, c.y, c.z, size * 2.4f, RED, 0.45f * alpha * burn * (1 - crumble));
            if (t < LIFT) {
                // The thread of it still clinging to the skin.
                d.softBeam(a[0].x, a[0].y, a[0].z, c.x, c.y, c.z, 0.08f, RED, 0.7f * (1 - lift), 0.4f, false);
            }
            // The rune itself, its strokes splitting apart and sinking as it crumbles.
            Vector3f l = d.camera.getLeftVector(), u = d.camera.getUpVector();
            float w = size * MarkGlyph.ASPECT;
            int[] idx = {0};
            MarkGlyph.walk(1f, (x0, y0, x1, y1) -> {
                int i = idx[0]++;
                float jx = (WitchFxKit.hash(seed, i) - 0.5f) * 0.5f * crumble, jy = -WitchFxKit.hash(seed, i + 50) * 0.35f * crumble * crumble;
                float ax = -(x0 - 0.5f) * w + jx, ay = (0.5f - y0) * size + jy, bx = -(x1 - 0.5f) * w + jx, by = (0.5f - y1) * size + jy;
                float sa = alpha * (crumble > WitchFxKit.hash(seed, i + 90) ? 0.35f : 1f);
                d.softBeam(c.x + l.x * ax + u.x * ay, c.y + l.y * ax + u.y * ay, c.z + l.z * ax + u.z * ay,
                        c.x + l.x * bx + u.x * by, c.y + l.y * bx + u.y * by, c.z + l.z * bx + u.z * by, size * 0.09f, col, sa * burn, sa * burn, crumble < 0.4f);
            });
        }
    }
}
