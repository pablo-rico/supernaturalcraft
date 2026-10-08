package org.papiricoh.supernaturalcraft.client.raphael;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.WorldDraw;
import org.papiricoh.supernaturalcraft.client.fx.BeamFx;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelBalance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Raphael's storm in the world (v0.16), drawn by the client from {@link ClientRaphael}'s state:
 * <ul>
 *   <li>the shadow of his wings: at each flash (FLASH with arg 1) the giant silhouette of two pairs of wings is thrown from
 *   him away from the lightning -- long across the floor and, if a wall stands within reach, up the wall -- for a moment, as
 *   in the show;</li>
 *   <li>the threads of grace from his garrison (TETHER): a ribbon of warm light from each angel's chest to his;</li>
 *   <li>the ring of holy fire that holds him (TRAP): curtains of golden light rising from the square of oil;</li>
 *   <li>the snap (SNAP): its safe band glowing green on the floor while it gathers, then a ring of white light racing out.</li>
 * </ul>
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class RaphaelWorldFx {

    /** Wings' shadow: (pair) leading-edge angle, trailing-edge angle (degrees above the horizontal, out from his shoulder), length. */
    private static final float[][] WINGS = {{32, -48, 5.6f}, {-12, -78, 4.0f}};
    private static final int FEATHERS = 10;
    private static final int GRACE = 0xFFE9B0, HOLY_FIRE = 0xFFB040, SAFE = 0x5CFF7A, STORM = 0xB9D8FF;

    static final class Shadow {
        final int entity;
        final Vec3 flash;
        Vec3 at;
        final int life;
        int age;

        Shadow(int entity, Vec3 flash, Vec3 at, int life) {
            this.entity = entity;
            this.flash = flash;
            this.at = at;
            this.life = life;
        }
    }

    record Tether(int raphael, long from, long until) {
    }

    record Trap(Vec3 point, long from, long until) {
    }

    static final class Snap {
        final Vec3 point;
        final float radius, width;
        final long from;
        final int windup;

        Snap(Vec3 point, float radius, float width, long from, int windup) {
            this.point = point;
            this.radius = radius;
            this.width = width;
            this.from = from;
            this.windup = windup;
        }
    }

    static final List<Shadow> SHADOWS = new ArrayList<>();
    static final Map<Integer, Tether> TETHERS = new HashMap<>();
    static final Map<Integer, Trap> TRAPS = new HashMap<>();
    static final List<Snap> SNAPS = new ArrayList<>();
    static long ticks;

    private RaphaelWorldFx() {
    }

    // --- state (ClientRaphael and the previews call these) -----------------------------------------------------------------

    public static void shadow(int entity, Vec3 flash, int duration) {
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null || entity < 0 ? null : mc.level.getEntity(entity);
        Vec3 at = e != null ? e.position() : flash;
        // One shadow of him at a time: a new flash replaces the last one's.
        if (entity >= 0) SHADOWS.removeIf(s -> s.entity == entity);
        SHADOWS.add(new Shadow(entity, flash, at, Mth.clamp(duration * 2 + 6, 10, 30)));
    }

    public static void tether(int angel, int raphael, int duration) {
        if (duration <= 0) TETHERS.remove(angel);
        else TETHERS.put(angel, new Tether(raphael, ticks, ticks + duration));
    }

    public static void trap(int raphael, Vec3 point, int duration) {
        if (duration <= 0) TRAPS.remove(raphael);
        else TRAPS.put(raphael, new Trap(point, ticks, ticks + duration));
    }

    public static void snap(Vec3 point, float radius, float width, int windup) {
        SNAPS.add(new Snap(point, radius, width, ticks, Math.max(1, windup)));
    }

    /** Whether any of it is showing (previews). */
    public static boolean busy() {
        return !SHADOWS.isEmpty() || !TETHERS.isEmpty() || !TRAPS.isEmpty() || !SNAPS.isEmpty();
    }

    public static boolean shadowing() {
        return !SHADOWS.isEmpty();
    }

    static void tick() {
        ticks++;
        for (Shadow s : SHADOWS) s.age++;
        SHADOWS.removeIf(s -> s.age >= s.life);
        TETHERS.values().removeIf(t -> t.until() < ticks);
        TRAPS.values().removeIf(t -> t.until() < ticks);
        // Gone with him: a thread or a ring whose archangel (or angel) has left the world goes too (after a grace period, since
        // a payload can come a tick before the entity is tracked).
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            TETHERS.entrySet().removeIf(e -> ticks - e.getValue().from() > 10
                    && (mc.level.getEntity(e.getKey()) == null || mc.level.getEntity(e.getValue().raphael()) == null));
            TRAPS.entrySet().removeIf(e -> ticks - e.getValue().from() > 10 && mc.level.getEntity(e.getKey()) == null);
        }
        SNAPS.removeIf(s -> ticks - s.from > s.windup + 14);
    }

    static void clear() {
        SHADOWS.clear();
        TETHERS.clear();
        TRAPS.clear();
        SNAPS.clear();
    }

    // --- drawing ---------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !busy()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        float now = ticks + partial;
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        VertexConsumer q = WorldDraw.quads(buffers);
        for (Shadow s : SHADOWS) shadow(mc, m, q, s, partial);
        for (Map.Entry<Integer, Trap> t : TRAPS.entrySet()) trap(m, q, t.getValue(), now);
        for (Snap s : SNAPS) snap(mc, m, q, s, now);
        pose.popPose();
        buffers.endBatch();
        // The threads: camera-relative ribbons.
        for (Map.Entry<Integer, Tether> t : TETHERS.entrySet()) {
            Entity angel = mc.level.getEntity(t.getKey()), raphael = mc.level.getEntity(t.getValue().raphael());
            if (angel == null || raphael == null || !angel.isAlive()) continue;
            Vec3 a = angel.getPosition(partial).add(0, angel.getBbHeight() * 0.62, 0).subtract(cam);
            Vec3 b = raphael.getPosition(partial).add(0, raphael.getBbHeight() * 0.65, 0).subtract(cam);
            float pulse = 0.75f + 0.25f * Mth.sin(now * 0.35f + t.getKey());
            BeamFx.draw(pose, buffers, a, b, 0.13f + 0.04f * pulse, GRACE, 0.85f * pulse, now * 0.08f);
        }
        buffers.endBatch();
    }

    // --- the wings' shadow -------------------------------------------------------------------------------------------------

    private static void shadow(Minecraft mc, Matrix4f m, VertexConsumer q, Shadow s, float partial) {
        Entity e = s.entity >= 0 ? mc.level.getEntity(s.entity) : null;
        if (e != null) s.at = e.getPosition(partial);
        float t = s.age + partial;
        // A lightning-flash curve: there at once, flickering, gone.
        float a = Mth.clamp(Math.min(t / 1.5f, (s.life - t) / (s.life * 0.6f)), 0, 1);
        a *= 0.8f + 0.2f * Mth.sin(t * 2.1f);
        if (a <= 0.02f) return;
        // Thrown away from the flash (if the bolt fell on him, behind him).
        Vec3 d = new Vec3(s.at.x - s.flash.x, 0, s.at.z - s.flash.z);
        if (d.lengthSqr() < 0.25) {
            float yaw = e instanceof LivingEntity l ? l.yBodyRot : e != null ? e.getYRot() : 0;
            d = Vec3.directionFromRotation(0, yaw + 180);
            d = new Vec3(d.x, 0, d.z);
        }
        d = d.normalize();
        Vec3 side = new Vec3(-d.z, 0, d.x);
        int dark = argb(0.62f * a, 0x05060A), deep = argb(0.4f * a, 0x020305);
        // Across the floor: long, stretched away from the light, from his feet.
        double floor = floor(mc, s.at);
        Vector3f o = new Vector3f((float) s.at.x, (float) floor + 0.04f, (float) s.at.z);
        Vector3f u = new Vector3f((float) side.x, 0, (float) side.z), v = new Vector3f((float) d.x, 0, (float) d.z).mul(1.5f);
        List<float[][]> floorWings = silhouette(1.0f);
        Vector3f fo = new Vector3f(o).add(new Vector3f(v).mul(0.9f));
        for (float[][] poly : floorWings) {
            for (int i = 1; i + 1 < poly.length; i++) {
                WorldDraw.quad(q, m, point(fo, u, v, poly[0][0], poly[0][1]), point(fo, u, v, poly[i][0], poly[i][1]),
                        point(fo, u, v, poly[i + 1][0], poly[i + 1][1]), point(fo, u, v, poly[0][0], poly[0][1]), dark);
            }
        }
        // Up the wall behind him, if one stands within reach: huge, his shoulders at his height, spread over the wall -- and
        // only where there is wall (rastered in quarter blocks, each cell checked).
        Vec3 eye = s.at.add(0, 1.5, 0);
        var hit = wall(mc, eye, d, 14);
        if (hit != null && hit.getLocation().distanceTo(eye) > 0.8 && hit.getDirection().getAxis().isHorizontal()) {
            double dist = hit.getLocation().distanceTo(eye);
            float scale = (float) Mth.clamp(0.9 + dist * 0.12, 1.0, 2.2);
            // The wall's own plane: its face normal (towards him) and the horizontal line along it.
            Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            Vec3 along = side.subtract(n.scale(side.dot(n)));
            if (along.lengthSqr() < 1e-4) along = new Vec3(-n.z, 0, n.x);
            along = along.normalize();
            Vec3 w = hit.getLocation().add(n.scale(0.03)).add(0, 0.4 * scale, 0);
            wallShadow(mc, m, q, w, n.scale(-1), along, scale, argb(0.8f * a, 0x030407), deep, argb(0.28f * a, 0xD8E8FF));
        }
    }

    /**
     * Two pairs of wings as flat polygons (x across, y up/out) from the shoulders at (0, 0): each wing a fan of long feathers
     * from its leading edge down to its trailing edge, a ragged edge between their tips: root, tip, notch, tip, ... .
     */
    static List<float[][]> silhouette(float scale) {
        List<float[][]> out = new ArrayList<>();
        for (int pair = 0; pair < WINGS.length; pair++) {
            float lead = WINGS[pair][0], trail = WINGS[pair][1], len = WINGS[pair][2] * scale;
            for (int side = -1; side <= 1; side += 2) {
                float rx = side * 0.25f * scale, ry = (pair == 0 ? 0.1f : -0.35f) * scale;
                float[][] poly = new float[FEATHERS * 2][];
                poly[0] = new float[]{rx, ry};
                float[][] tip = new float[FEATHERS][];
                for (int f = 0; f < FEATHERS; f++) {
                    float k = f / (float) (FEATHERS - 1);
                    float ang = (float) Math.toRadians(lead + (trail - lead) * k);
                    float fl = len * (0.72f + 0.28f * Mth.sin(Mth.PI * (0.2f + 0.7f * k)));
                    tip[f] = new float[]{rx + side * Mth.cos(ang) * fl, ry + Mth.sin(ang) * fl};
                }
                int n = 1;
                for (int f = 0; f < FEATHERS; f++) {
                    poly[n++] = tip[f];
                    if (f + 1 < FEATHERS) {
                        float nx = (tip[f][0] + tip[f + 1][0]) / 2, ny = (tip[f][1] + tip[f + 1][1]) / 2;
                        poly[n++] = new float[]{rx + (nx - rx) * 0.84f, ry + (ny - ry) * 0.84f};
                    }
                }
                out.add(java.util.Arrays.copyOf(poly, n));
            }
        }
        return out;
    }

    static boolean inside(float[][] poly, float x, float y) {
        boolean in = false;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            if ((poly[i][1] > y) != (poly[j][1] > y)
                    && x < (poly[j][0] - poly[i][0]) * (y - poly[i][1]) / (poly[j][1] - poly[i][1]) + poly[i][0]) in = !in;
        }
        return in;
    }

    /** The wings' silhouette on the wall plane through {@code w} facing back along {@code d}, cell by cell where it is solid. */
    private static void wallShadow(Minecraft mc, Matrix4f m, VertexConsumer q, Vec3 w, Vec3 d, Vec3 side, float scale, int dark, int deep,
                                   int light) {
        List<float[][]> wings = silhouette(scale);
        float cell = 0.15f;
        float reach = 6.4f * scale;
        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
        for (float y = -reach; y <= reach * 0.75f; y += cell) {
            float runStart = Float.NaN;
            int runColour = 0;
            for (float x = -reach; x <= reach + cell; x += cell) {
                int colour = 0;
                if (x <= reach) {
                    float cx = x + cell / 2, cy = y + cell / 2;
                    // The wall lit white-blue by the flash, the wings dark against it.
                    colour = light;
                    for (float[][] poly : wings) {
                        if (inside(poly, cx, cy)) {
                            colour = dark;
                            break;
                        }
                    }
                    Vec3 at = w.add(side.scale(cx)).add(0, cy, 0).add(d.scale(0.3));
                    bp.set(at.x, at.y, at.z);
                    if (mc.level.getBlockState(bp).getCollisionShape(mc.level, bp).isEmpty()) colour = 0;
                }
                if (colour != runColour) {
                    if (runColour != 0) cellRun(m, q, w, side, runStart, x, y, cell, runColour);
                    runStart = x;
                    runColour = colour;
                }
            }
        }
        // His own shadow, a tall figure under the wings.
        Vector3f c = new Vector3f((float) w.x, (float) (w.y - 1.9 * scale), (float) w.z);
        Vector3f u = new Vector3f((float) side.x, 0, (float) side.z);
        Vector3f half = new Vector3f(u).mul(0.3f * scale), up = new Vector3f(0, 2.0f * scale, 0);
        WorldDraw.quad(q, m, new Vector3f(c).sub(half), new Vector3f(c).add(half), new Vector3f(c).add(half).add(up),
                new Vector3f(c).sub(half).add(up), deep);
    }

    private static void cellRun(Matrix4f m, VertexConsumer q, Vec3 w, Vec3 side, float x0, float x1, float y, float cell, int argb) {
        Vector3f o = new Vector3f((float) w.x, (float) w.y, (float) w.z);
        Vector3f u = new Vector3f((float) side.x, 0, (float) side.z), up = new Vector3f(0, 1, 0);
        WorldDraw.quad(q, m, point(o, u, up, x0, y), point(o, u, up, x1, y), point(o, u, up, x1, y + cell), point(o, u, up, x0, y + cell), argb);
    }

    private static Vector3f point(Vector3f o, Vector3f u, Vector3f v, float x, float y) {
        return new Vector3f(o).add(new Vector3f(u).mul(x)).add(new Vector3f(v).mul(y));
    }

    /** The floor under a point, up to 16 blocks down. */
    static double floor(Minecraft mc, Vec3 at) {
        BlockPos.MutableBlockPos p = BlockPos.containing(at.x, at.y + 0.2, at.z).mutable();
        for (int i = 0; i < 16; i++, p.move(0, -1, 0)) {
            var shape = mc.level.getBlockState(p).getCollisionShape(mc.level, p);
            if (!shape.isEmpty()) return p.getY() + shape.max(Direction.Axis.Y);
        }
        return at.y;
    }

    /** The first solid block face along {@code dir} from {@code from} within {@code reach}, or null. */
    static net.minecraft.world.phys.BlockHitResult wall(Minecraft mc, Vec3 from, Vec3 dir, double reach) {
        var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(from, from.add(dir.scale(reach)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? hit : null;
    }

    // --- the ring of holy fire ---------------------------------------------------------------------------------------------

    private static void trap(Matrix4f m, VertexConsumer q, Trap t, float now) {
        float age = now - t.from(), left = t.until() - now;
        float a = Mth.clamp(Math.min(age / 6f, left / 10f), 0, 1);
        if (a <= 0.01f) return;
        double y = Math.floor(t.point().y) + 0.02;
        float cx = (float) t.point().x, cz = (float) t.point().z;
        // The oil is the border of a 5 x 5 square: the light rises from its middle line, two cells out.
        float r = 2.0f;
        Vector3f[] corner = {new Vector3f(cx - r, (float) y, cz - r), new Vector3f(cx + r, (float) y, cz - r),
                new Vector3f(cx + r, (float) y, cz + r), new Vector3f(cx - r, (float) y, cz + r)};
        for (int i = 0; i < 4; i++) {
            Vector3f p0 = corner[i], p1 = corner[(i + 1) % 4];
            int steps = 8;
            for (int k = 0; k < steps; k++) {
                Vector3f a0 = new Vector3f(p0).lerp(p1, k / (float) steps), a1 = new Vector3f(p0).lerp(p1, (k + 1) / (float) steps);
                float h = 1.6f + 0.5f * Mth.sin(now * 0.5f + i * 2 + k * 1.3f) + 0.3f * Mth.sin(now * 1.3f + k);
                int base = argb(0.42f * a, HOLY_FIRE), top = argb(0, 0xFFF2C4);
                // A curtain of light: bright at the oil, fading as it rises.
                quadGradient(q, m, a0, a1, new Vector3f(a1).add(0, h, 0), new Vector3f(a0).add(0, h, 0), base, top);
            }
        }
        // A wash of firelight on the floor inside.
        int floorGlow = argb(0.18f * a, HOLY_FIRE);
        WorldDraw.quad(q, m, corner[0], corner[1], corner[2], corner[3], floorGlow);
    }

    private static void quadGradient(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, Vector3f c, Vector3f d, int lo, int hi) {
        vertex(vc, m, a, lo);
        vertex(vc, m, b, lo);
        vertex(vc, m, c, hi);
        vertex(vc, m, d, hi);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vector3f p, int argb) {
        vc.addVertex(m, p.x, p.y, p.z).setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, argb >>> 24);
    }

    // --- the snap ----------------------------------------------------------------------------------------------------------

    private static void snap(Minecraft mc, Matrix4f m, VertexConsumer q, Snap s, float now) {
        float t = now - s.from;
        double y = floor(mc, s.point) + 0.05;
        Vector3f c = new Vector3f((float) s.point.x, (float) y, (float) s.point.z);
        if (t < s.windup) {
            float grow = Mth.clamp(t / 8f, 0, 1);
            float pulse = 0.6f + 0.4f * Mth.sin(t * (0.25f + 0.5f * t / s.windup));
            float in = (float) RaphaelBalance.snapSafeInner(s.radius, s.width), out = (float) RaphaelBalance.snapSafeOuter(s.radius, s.width);
            annulus(m, q, c, in, out, argb(0.32f * grow * pulse, SAFE), 48);
            annulus(m, q, c, out - 0.12f, out, argb(0.8f * grow, SAFE), 48);
            annulus(m, q, c, in, in + 0.12f, argb(0.8f * grow, SAFE), 48);
            annulus(m, q, c, s.radius - 0.15f, s.radius, argb(0.7f * grow * pulse, STORM), 64);
        } else {
            // The burst: a ring of white light racing out to the edge.
            float k = Mth.clamp((t - s.windup) / 10f, 0, 1);
            float r = s.radius * k;
            float a = 1 - k;
            annulus(m, q, new Vector3f(c).add(0, 0.05f, 0), Math.max(0, r - 1.2f), r, argb(0.75f * a, 0xFFFFFF), 64);
            annulus(m, q, new Vector3f(c).add(0, 0.02f, 0), Math.max(0, r - 3.0f), Math.max(0, r - 1.2f), argb(0.3f * a, STORM), 64);
        }
    }

    private static void annulus(Matrix4f m, VertexConsumer q, Vector3f c, float r0, float r1, int argb, int segments) {
        if (r1 <= r0) return;
        for (int i = 0; i < segments; i++) {
            float a0 = i * Mth.TWO_PI / segments, a1 = (i + 1) * Mth.TWO_PI / segments;
            WorldDraw.quad(q, m, new Vector3f(c.x + Mth.cos(a0) * r0, c.y, c.z + Mth.sin(a0) * r0),
                    new Vector3f(c.x + Mth.cos(a0) * r1, c.y, c.z + Mth.sin(a0) * r1),
                    new Vector3f(c.x + Mth.cos(a1) * r1, c.y, c.z + Mth.sin(a1) * r1),
                    new Vector3f(c.x + Mth.cos(a1) * r0, c.y, c.z + Mth.sin(a1) * r0), argb);
        }
    }

    static int argb(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }
}
