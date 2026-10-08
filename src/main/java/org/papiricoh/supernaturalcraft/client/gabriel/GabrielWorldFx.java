package org.papiricoh.supernaturalcraft.client.gabriel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.WorldDraw;
import org.papiricoh.supernaturalcraft.client.michael.render.GeoProp;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielAssets;

import java.util.ArrayList;
import java.util.List;

/**
 * Gabriel in the world (v0.14), outside his own renderer: the six-winged golden shadow cast on the ground round the real one
 * in the commercial (everyone sees it), and the prank's party hat on whatever mob wears one (drawn on its head: at its eyes'
 * height, turned with its head).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class GabrielWorldFx {

    /** Six wings, in three pairs: forward, out to the sides, back (degrees off his facing), and how long each is. */
    private static final float[] WING_ANGLES = {38, 90, 142};
    private static final float[] WING_LENGTH = {3.2f, 3.9f, 3.0f};
    private static final ResourceLocation HAT_MODEL = SupernaturalCraft.asResource(GabrielAssets.PARTY_HAT_GEO),
            HAT_TEXTURE = SupernaturalCraft.asResource(GabrielAssets.PARTY_HAT_TEXTURE);

    private static final class Shadow {
        final int entity;
        final Vec3 point;
        int life, age;
        boolean seen;

        Shadow(int entity, Vec3 point, int life) {
            this.entity = entity;
            this.point = point;
            this.life = life;
        }
    }

    private static final List<Shadow> SHADOWS = new ArrayList<>();
    private static GeoProp hat;

    private GabrielWorldFx() {
    }

    public static void reveal(int entity, Vec3 point, int duration) {
        // Refreshed every second while he stands there: the same shadow lasts longer instead of fading in again.
        for (Shadow s : SHADOWS) {
            if (entity >= 0 && s.entity == entity && s.age < s.life) {
                s.life = Math.max(s.life, s.age + Math.max(10, duration));
                return;
            }
        }
        SHADOWS.add(new Shadow(entity, point, Math.max(10, duration)));
    }

    static void tick() {
        for (Shadow s : SHADOWS) s.age++;
        SHADOWS.removeIf(s -> s.age >= s.life);
    }

    static void clear() {
        SHADOWS.clear();
    }

    /** Whether a shadow is on the ground now (previews). */
    public static boolean revealing() {
        return !SHADOWS.isEmpty();
    }

    // --- the shadow of six wings ---------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || SHADOWS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        // Gone with him (but the payload can come a tick before he is tracked: until then, the shadow stays where it was sent).
        SHADOWS.removeIf(s -> {
            if (s.entity < 0) return false;
            Entity e = mc.level.getEntity(s.entity);
            if (e != null) s.seen = true;
            return e == null ? s.seen : !e.isAlive();
        });
        for (Shadow s : SHADOWS) {
            Vec3 at = s.point;
            float facing = 0;
            Entity e = s.entity >= 0 ? mc.level.getEntity(s.entity) : null;
            if (e != null) {
                at = e.getPosition(partial);
                facing = e instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : e.getYRot();
            }
            // A shadow falls on the floor round him: on a podium, the stage floor beside it, not the podium's top.
            double floor = floor(mc, at);
            double[] around = new double[4];
            for (int i = 0; i < 4; i++) {
                double ang = i * Math.PI / 2 + Math.PI / 4;
                around[i] = floor(mc, at.add(Math.cos(ang) * 2.5, 0, Math.sin(ang) * 2.5));
            }
            java.util.Arrays.sort(around);
            if (around[1] < floor && floor - around[1] <= 2.5) floor = around[1];
            float t = s.age + partial;
            float a = Mth.clamp(Math.min(t / 15f, (s.life - t) / 8f), 0, 1);
            pose.pushPose();
            pose.translate(at.x - cam.x, floor + 0.03 - cam.y, at.z - cam.z);
            pose.mulPose(Axis.YP.rotationDegrees(180 - facing));
            wings(pose.last().pose(), WorldDraw.quads(buffers), t, a);
            pose.popPose();
        }
        buffers.endBatch();
    }

    /** The floor under a point (a shadow falls to the ground, even under someone in the air), up to 16 blocks down. */
    private static double floor(Minecraft mc, Vec3 at) {
        BlockPos.MutableBlockPos p = BlockPos.containing(at.x, at.y + 0.2, at.z).mutable();
        for (int i = 0; i < 16; i++, p.move(0, -1, 0)) {
            var shape = mc.level.getBlockState(p).getCollisionShape(mc.level, p);
            if (!shape.isEmpty()) return p.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
        }
        return at.y;
    }

    /**
     * Six wings of shadow fanned out from his feet, three to a side (forward, out, back): each a solid silhouette of nine
     * long feathers with a scalloped trailing edge, a darker band of coverts near the root, gold along the leading edge and,
     * faintly, along each quill. They breathe.
     */
    private static void wings(Matrix4f m, VertexConsumer q, float t, float a) {
        float breathe = 1 + 0.04f * Mth.sin(t * 0.12f);
        int dark = GabrielGui.argb(0.72f * a, 0x1C1306), deeper = GabrielGui.argb(0.4f * a, 0x0E0903);
        // A dark pool under him.
        for (int i = 0; i < 16; i++) {
            float a0 = i * Mth.TWO_PI / 16, a1 = (i + 1) * Mth.TWO_PI / 16;
            WorldDraw.quad(q, m, new Vector3f(0, 0, 0), new Vector3f(Mth.cos(a0) * 0.8f, 0, Mth.sin(a0) * 0.8f),
                    new Vector3f(Mth.cos(a1) * 0.8f, 0, Mth.sin(a1) * 0.8f), new Vector3f(0, 0, 0), dark);
        }
        int feathers = 9;
        float spread = 0.3f, root = 0.35f;
        for (int pair = 0; pair < 3; pair++) {
            for (int side = -1; side <= 1; side += 2) {
                float base = (float) Math.toRadians(WING_ANGLES[pair]) * side;
                float len = WING_LENGTH[pair] * breathe;
                float sway = 0.03f * Mth.sin(t * 0.08f + pair * 1.7f);
                Vector3f[] tip = new Vector3f[feathers], rootP = new Vector3f[feathers];
                for (int f = 0; f < feathers; f++) {
                    // f = 0 is the leading edge (towards his front), the last the trailing edge.
                    float k = f / (float) (feathers - 1);
                    float ang = base + side * (-spread + 2 * spread * k) + side * sway;
                    // Forward is -z in the entity's frame after the yaw turn.
                    float dx = Mth.sin(ang), dz = -Mth.cos(ang);
                    float fl = len * (0.78f + 0.22f * Mth.sin(Mth.PI * (0.25f + 0.6f * k)));
                    tip[f] = new Vector3f(dx * fl, 0, dz * fl);
                    rootP[f] = new Vector3f(dx * root, 0, dz * root);
                }
                for (int f = 0; f < feathers - 1; f++) {
                    // Between two quills: the vane out to a notch short of both tips.
                    Vector3f notch = new Vector3f(tip[f]).add(tip[f + 1]).mul(0.5f * 0.86f);
                    WorldDraw.quad(q, m, rootP[f], tip[f], notch, rootP[f + 1], dark);
                    WorldDraw.quad(q, m, notch, tip[f + 1], rootP[f + 1], rootP[f + 1], dark);
                    // Coverts: a second, darker layer near the root.
                    Vector3f c0 = new Vector3f(tip[f]).mul(0.45f), c1 = new Vector3f(tip[f + 1]).mul(0.45f);
                    WorldDraw.quad(q, m, rootP[f], c0, c1, rootP[f + 1], deeper);
                }
                for (int f = 0; f < feathers; f++) {
                    float glow = (f == 0 ? 0.6f : 0.1f) + 0.15f * Mth.sin(t * 0.15f + f * 0.7f + pair);
                    WorldDraw.groundStroke(q, m, rootP[f].x, rootP[f].z, tip[f].x, tip[f].z, 0.004f, f == 0 ? 0.06f : 0.03f,
                            GabrielGui.argb(glow * a, GabrielGui.GOLD));
                }
            }
        }
    }

    // --- the party hat -------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (!ClientGabriel.anyHats()) return;
        LivingEntity e = event.getEntity();
        if (!ClientGabriel.hatted(e.getId()) || e.isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        if (e == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) return;
        if (hat == null) hat = new GeoProp(HAT_MODEL, HAT_TEXTURE, null, null);
        if (!hat.ready()) return;
        float partial = event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        float headYaw = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float pitch = Mth.lerp(partial, e.xRotO, e.getXRot());
        float h = e.getBbHeight(), w = e.getBbWidth(), s = e.getScale();
        // The top of the head, roughly: above the eyes by a share of the body's height; a quadruped's head sits forward.
        float above = (0.08f + 0.3f * Math.min(1f, h / s / 1.8f)) * s;
        float forward = Math.max(0, w - 0.6f * s) * 2f;
        float size = Mth.clamp(w / s / 0.6f, 0.6f, 1.4f) * s;
        pose.pushPose();
        pose.translate(0, e.getEyeHeight(), 0);
        pose.mulPose(Axis.YP.rotationDegrees(180 - headYaw));
        pose.translate(0, 0, -forward);
        pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        pose.translate(0, above - 0.04f * s, 0);
        // Tilted a little, as a party hat always is.
        pose.mulPose(Axis.ZP.rotationDegrees(12));
        pose.scale(size, size, size);
        hat.draw(pose, event.getMultiBufferSource(), HAT_TEXTURE, e.tickCount, partial, event.getPackedLight());
        pose.popPose();
    }
}
