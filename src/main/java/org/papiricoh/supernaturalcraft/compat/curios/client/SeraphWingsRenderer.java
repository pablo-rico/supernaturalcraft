package org.papiricoh.supernaturalcraft.compat.curios.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The Seraph Wings on a player's back: a GeckoLib model hung from the body, posed by hand each
 * frame — resting with a slow beat, folded tight while crouching, spread wide in a fall or a glide.
 */
public class SeraphWingsRenderer implements ICurioRenderer {

    private static final String[] WINGS = {"wing_top_l", "wing_top_r", "wing_mid_l", "wing_mid_r", "wing_low_l", "wing_low_r"};
    private static final float[] REST = {0.55f, 0.1f, -0.45f}, SPREAD = {0.85f, 0.25f, -0.25f}, FOLD = {0.2f, -0.25f, -0.7f};
    private static final float SCALE = 1.3f;

    /** How open the wings of each wearer are (0 folded … 1 rest … 2 spread), eased frame to frame. */
    private static final Map<LivingEntity, float[]> OPEN = new WeakHashMap<>();

    private final Wings animatable = new Wings();
    private final GeoObjectRenderer<Wings> renderer;

    public SeraphWingsRenderer() {
        // Drawn full bright instead of with a glow layer: the wings shine on their own.
        renderer = new GeoObjectRenderer<>(new Model());
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack pose,
            RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount, float partial,
            float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity wearer = slot.entity();
        float[] open = OPEN.computeIfAbsent(wearer, k -> new float[]{1});
        float want = wearer.isCrouching() ? 0 : wearer.isFallFlying() || (!wearer.onGround() && wearer.getDeltaMovement().y < -0.4) ? 2 : 1;
        open[0] += Mth.clamp(want - open[0], -0.08f, 0.08f);
        animatable.open = open[0];
        animatable.time = ageInTicks;
        pose.pushPose();
        if (parent.getModel() instanceof HumanoidModel<?> humanoid) humanoid.body.translateAndRotate(pose);
        // Undo the living renderer's flip: Y up, the model's front toward -Z like GeckoLib's.
        pose.scale(-SCALE, -SCALE, SCALE);
        pose.translate(0, -0.2, 0.02);
        // GeoObjectRenderer centres its model in a block; take that back out.
        pose.translate(-0.5, -0.51, -0.5);
        renderer.render(pose, animatable, buffers, null, null, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, partial);
        pose.popPose();
    }

    /** The one animatable every wearer's wings are drawn through; posed before each draw. */
    static class Wings implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        float open = 1, time;

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public double getTick(Object object) {
            return time;
        }
    }

    static class Model extends GeoModel<Wings> {
        private static final ResourceLocation GEO = SupernaturalCraft.asResource("geo/entity/seraph_wings.geo.json");
        private static final ResourceLocation TEX = SupernaturalCraft.asResource("textures/entity/seraph_wings.png");
        private static final ResourceLocation ANIM = SupernaturalCraft.asResource("animations/entity/seraph_wings.animation.json");

        @Override
        public ResourceLocation getModelResource(Wings animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(Wings animatable) {
            return TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(Wings animatable) {
            return ANIM;
        }

        @Override
        public void setCustomAnimations(Wings w, long instanceId, AnimationState<Wings> state) {
            float o = w.open;
            for (int i = 0; i < WINGS.length; i++) {
                GeoBone bone = getAnimationProcessor().getBone(WINGS[i]);
                if (bone == null) continue;
                int level = i / 2;
                float lift = o < 1 ? Mth.lerp(o, FOLD[level], REST[level]) : Mth.lerp(o - 1, REST[level], SPREAD[level]);
                lift += 0.07f * Mth.sin(w.time * 0.12f + level) * Math.min(1, o);
                float sweep = (1 - Math.min(1, o)) * 1.1f;    // folded wings sweep back along the body
                boolean left = i % 2 == 0;
                bone.setRotZ(left ? -lift : lift);
                bone.setRotY(left ? sweep : -sweep);
            }
        }
    }
}
