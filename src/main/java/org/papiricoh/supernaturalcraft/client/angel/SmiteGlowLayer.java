package org.papiricoh.supernaturalcraft.client.angel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.angel.AngelAssets;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

/**
 * A smitten angel burning from within (v0.22), for any angel's GeckoLib renderer: its whole body brightening toward white (its
 * model again, additive, rising and flickering), its eyes blazing; and where its lights are ({@link AngelAssets#LIGHT_LOCATORS}
 * in {@code head}, or fixed points on the head where a rig has none), each frame, for {@link SmiteFx}'s beams. Inert unless
 * {@link ClientAngels} has a smite for the entity.
 */
public class SmiteGlowLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {

    /** Where the lights sit on a head without locators: from its pivot (the neck), in blocks, facing -z. */
    private static final float[][] HEAD_LIGHTS = {{0.11f, 0.29f, -0.27f}, {-0.11f, 0.29f, -0.27f}, {0, 0.13f, -0.27f}};

    public SmiteGlowLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack pose, T animatable, GeoBone bone, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                              float partialTick, int light, int overlay) {
        if (!(animatable instanceof Entity e) || ClientAngels.glow(e.getId(), partialTick) <= 0) return;
        String name = bone.getName();
        int slot = AngelAssets.LIGHT_LOCATORS.indexOf(name);
        boolean head = "head".equals(name);
        if (slot < 0 && !head) return;
        pose.pushPose();
        RenderUtil.translateToPivotPoint(pose, bone);
        Matrix4f m = pose.last().pose();
        Vector3f f = pose.last().normal().transform(new Vector3f(0, 0, -1));
        if (f.lengthSquared() < 1e-6f) f.set(0, 0, -1);
        f.normalize();
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 at = e.getPosition(partialTick);
        double reach = 3 + e.getBbHeight();
        if (head) {
            // The head comes before its locators, which then overwrite these.
            for (int i = 0; i < 3; i++) {
                Vector3f p = m.transformPosition(HEAD_LIGHTS[i][0], HEAD_LIGHTS[i][1], HEAD_LIGHTS[i][2], new Vector3f());
                record(e, i, cam, at, reach, p, f);
            }
        } else {
            record(e, slot, cam, at, reach, m.transformPosition(0, 0, 0, new Vector3f()), f);
        }
        pose.popPose();
    }

    /** A light seen in a world pass (camera-relative); a GUI's model (an inventory, a book) is far from the entity and ignored. */
    private static void record(Entity e, int slot, Vec3 cam, Vec3 at, double reach, Vector3f p, Vector3f f) {
        double x = cam.x + p.x, y = cam.y + p.y, z = cam.z + p.z;
        double dx = x - at.x, dy = y - at.y, dz = z - at.z;
        if (dx * dx + dy * dy + dz * dz > reach * reach) return;
        ClientAngels.capture(e.getId(), slot, x, y, z, f.x, f.y, f.z);
    }

    @Override
    public void render(PoseStack pose, T animatable, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                       float partialTick, int light, int overlay) {
        if (!(animatable instanceof Entity e)) return;
        float k = ClientAngels.glow(e.getId(), partialTick);
        if (k <= 0) return;
        float t = (e.level().getGameTime() % 100000) + partialTick;
        float flick = 0.82f + 0.18f * Mth.sin(t * 2.9f + e.getId()) * Mth.sin(t * 0.83f + 1.3f);
        ResourceLocation tex = getTextureResource(animatable);
        // The body lit from within: blue-white light rising over it, then white.
        pass(pose, animatable, model, buffers, RenderType.eyes(tex), partialTick, overlay, argb(Math.min(1, 0.15f + k) * flick, ClientAngels.GRACE));
        if (k > 0.45f) {
            float w = (k - 0.45f) / 0.55f;
            pose.pushPose();
            // A faint shimmer round it, as if it could not hold the light.
            float j = 0.012f * w;
            pose.translate(j * Mth.sin(t * 7.3f), j * Mth.sin(t * 5.1f), j * Mth.cos(t * 6.7f));
            pass(pose, animatable, model, buffers, RenderType.eyes(tex), partialTick, overlay, argb(w * w * flick, 0xFFFFFF));
            pose.popPose();
        }
        ResourceLocation glow = GeoGuard.glowmask(tex);
        if (GeoGuard.exists(glow)) {
            pass(pose, animatable, model, buffers, RenderType.eyes(glow), partialTick, overlay, argb(1, ClientAngels.GRACE_HOT));
        }
    }

    private void pass(PoseStack pose, T animatable, BakedGeoModel model, MultiBufferSource buffers, RenderType type, float partial,
                      int overlay, int argb) {
        getRenderer().reRender(model, pose, buffers, animatable, type, buffers.getBuffer(type), partial, 0xF000F0, overlay, argb);
    }

    private static int argb(float k, int rgb) {
        k = Mth.clamp(k, 0, 1);
        int r = Math.round((rgb >> 16 & 0xFF) * k), g = Math.round((rgb >> 8 & 0xFF) * k), b = Math.round((rgb & 0xFF) * k);
        return Math.round(255 * k) << 24 | r << 16 | g << 8 | b;
    }
}
