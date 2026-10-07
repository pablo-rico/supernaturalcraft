package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.Color;

/**
 * A hellhound as you see it: unseen, only a ripple of bent air in its shape (a faint, wavering haze
 * texture) — unless it has been revealed, you carry the Eclipse Sight or you have Second Sight, and then the
 * beast itself.
 */
public class HellhoundRenderer extends GeoEntityRenderer<HellhoundEntity> {

    private static final ResourceLocation HAZE = SupernaturalCraft.asResource("textures/entity/hellhound_haze.png");

    public HellhoundRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("hellhound"), true));
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            public void render(PoseStack pose, HellhoundEntity hound, BakedGeoModel model, RenderType type, MultiBufferSource buffers,
                               VertexConsumer buffer, float partialTick, int light, int overlay) {
                if (seen(hound)) super.render(pose, hound, model, type, buffers, buffer, partialTick, light, overlay);
            }
        });
        shadowRadius = 0.6f;
    }

    /** Whether this hound shows itself to the local player. */
    public static boolean seen(HellhoundEntity hound) {
        var player = Minecraft.getInstance().player;
        if (hound.isRevealed()) return true;
        return player != null && (player.getMainHandItem().is(AllItems.ECLIPSE_SIGHT.get()) || player.getOffhandItem().is(AllItems.ECLIPSE_SIGHT.get())
                || player.isSpectator() || player.hasEffect(AllMobEffects.SECOND_SIGHT));
    }

    @Override
    public RenderType getRenderType(HellhoundEntity hound, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
        return seen(hound) ? super.getRenderType(hound, texture, buffers, partialTick) : RenderType.entityTranslucent(HAZE);
    }

    @Override
    public Color getRenderColor(HellhoundEntity hound, float partialTick, int light) {
        if (seen(hound)) return super.getRenderColor(hound, partialTick, light);
        float t = hound.tickCount + partialTick;
        int alpha = (int) (34 + 22 * Mth.sin(t * 0.35f) + 10 * Mth.sin(t * 1.3f));
        return Color.ofARGB(Mth.clamp(alpha, 8, 80), 255, 230, 220);
    }

    @Override
    public void preRender(PoseStack pose, HellhoundEntity hound, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        // The air shivers around an unseen hound: a slight, restless wobble of its shape.
        if (!seen(hound)) {
            float t = hound.tickCount + partialTick;
            pose.scale(1f + 0.03f * Mth.sin(t * 0.9f), 1f + 0.02f * Mth.sin(t * 1.1f + 1), 1f + 0.03f * Mth.cos(t * 0.8f));
        }
        super.preRender(pose, hound, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }
}
