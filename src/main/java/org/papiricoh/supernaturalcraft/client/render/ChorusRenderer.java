package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusGeometry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The Broken Chorus. Its body turns with its heading, its wheels spin and wander, its wings beat,
 * and its eyes open — all from {@link ChorusGeometry}, the same numbers its hit boxes use. Broken
 * parts swap to their ruined variants; the cracks in its marble glow gold, then orange, then red.
 */
public class ChorusRenderer extends GeoEntityRenderer<ChorusEntity> {

    private static final ResourceLocation CRACKS = SupernaturalCraft.asResource("textures/entity/broken_chorus_cracks.png");
    private static final int[] CRACK_COLORS = {0xFFE7A0, 0xFFE7A0, 0xFFA040, 0xFF6A30, 0xFF3A20};
    /** How open each eye's lids are, eased toward the server's word over a few frames. */
    private static final Map<ChorusEntity, float[]> LIDS = new WeakHashMap<>();

    public ChorusRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        addRenderLayer(new CrackLayer(this));
        shadowRadius = 0f;
    }

    static class Model extends DefaultedEntityGeoModel<ChorusEntity> {

        Model() {
            super(SupernaturalCraft.asResource("broken_chorus"), false);
        }

        @Override
        public void setCustomAnimations(ChorusEntity e, long instanceId, AnimationState<ChorusEntity> state) {
            super.setCustomAnimations(e, instanceId, state);
            float partial = state.getPartialTick();
            double time = e.time(partial);
            rot("body_turn", 0, ChorusGeometry.bodyTurn(e.heading(partial)), 0);
            for (int w = 0; w < 3; w++) {
                Vector3f r = e.wheelRot(w, time);
                rot("wheel_" + ChorusGeometry.WHEEL_NAMES[w], r.x, r.y, r.z);
            }
            for (int w = 0; w < ChorusGeometry.WINGS; w++) {
                Vector3f r = e.wingRot(w, time);
                rot("wing_" + ChorusGeometry.WING_NAMES[w], r.x, r.y, r.z);
            }
            float[] lids = LIDS.computeIfAbsent(e, k -> new float[ChorusGeometry.EYES]);
            for (int i = 0; i < ChorusGeometry.EYES; i++) {
                float want = e.eyeOpen(i, time) ? 1 : 0;
                lids[i] += Mth.clamp(want - lids[i], -0.08f, 0.08f);
                float open = Mth.sin(lids[i] * Mth.HALF_PI) * 1.9f;
                rot("eye_" + i + "_lid_a", open, 0, 0);
                rot("eye_" + i + "_lid_b", -open, 0, 0);
            }
        }

        private void rot(String name, float x, float y, float z) {
            GeoBone b = getAnimationProcessor().getBone(name);
            if (b == null) return;
            b.setRotX(x);
            b.setRotY(y);
            b.setRotZ(z);
        }
    }

    @Override
    public void preRender(PoseStack pose, ChorusEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            scaleWidth = scaleHeight = ChorusGeometry.MODEL_SCALE;
            boolean last = e.phase() == 4;
            for (int i = 0; i < ChorusGeometry.FACES; i++) {
                String n = "face_" + ChorusGeometry.FACE_NAMES[i];
                boolean alive = e.partAlive(ChorusEntity.FIRST_FACE + i);
                show(n + "_intact", alive);
                show(n + "_cracked", alive && e.cracked(ChorusEntity.FIRST_FACE + i));
                show(n + "_broken", !alive);
            }
            for (int w = 0; w < ChorusGeometry.WINGS; w++) {
                String n = "wing_" + ChorusGeometry.WING_NAMES[w];
                int part = ChorusEntity.FIRST_WING + w;
                boolean alive = e.partAlive(part);
                show(n, alive);
                show(n + "_stump", !alive && !last);
                if (alive) {
                    // A cracked wing loses every other flight feather: holes you can see through.
                    boolean holes = e.cracked(part);
                    for (int k = 0; k < 3; k++) show(n + "_fb" + k, !holes);
                }
            }
            for (int i = 0; i < ChorusGeometry.EYES; i++) {
                boolean alive = e.partAlive(ChorusEntity.FIRST_EYE + i);
                show("eye_" + i + "_ball", alive);
                show("eye_" + i + "_lid_a", alive);
                show("eye_" + i + "_lid_b", alive);
                show("eye_" + i + "_socket", !alive);
            }
            show("wheels", !last);
            show("fragments", last);
            show("core_shell", !last);
            show("heads", !last);
            show("torso", !last);
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    private void show(String bone, boolean visible) {
        getGeoModel().getBone(bone).ifPresent(b -> {
            b.setHidden(!visible);
            b.setChildrenHidden(!visible);
        });
    }

    /** It is its own light: drawn full bright, storm or no storm. */
    @Override
    protected int getBlockLightLevel(ChorusEntity entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override
    protected float getDeathMaxRotation(ChorusEntity entity) {
        return 0f;
    }

    /** The cracks in its marble, lit from within and tinted by how far the fight has gone. */
    static class CrackLayer extends GeoRenderLayer<ChorusEntity> {

        CrackLayer(GeoRenderer<ChorusEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, ChorusEntity e, BakedGeoModel model, RenderType type, MultiBufferSource buffers,
                           VertexConsumer buffer, float partialTick, int light, int overlay) {
            RenderType eyes = RenderType.eyes(CRACKS);
            int rgb = CRACK_COLORS[Mth.clamp(e.phase(), 0, 4)];
            float pulse = 0.75f + 0.25f * Mth.sin((float) e.time(partialTick) * 0.15f);
            int r = (int) (((rgb >> 16) & 0xFF) * pulse), g = (int) (((rgb >> 8) & 0xFF) * pulse), b = (int) ((rgb & 0xFF) * pulse);
            getRenderer().reRender(model, pose, buffers, e, eyes, buffers.getBuffer(eyes), partialTick, 0xF00000, overlay,
                    0xFF000000 | (r << 16) | (g << 8) | b);
        }
    }
}
