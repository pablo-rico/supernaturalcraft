package org.papiricoh.supernaturalcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.ScribeConstruct;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Metatron's Hand or Book, drawn large; the Hand is half-transparent light. */
public class ScribeConstructRenderer<T extends ScribeConstruct> extends GeoEntityRenderer<T> {

    private final boolean translucent;

    public ScribeConstructRenderer(EntityRendererProvider.Context ctx, String model, float scale, boolean translucent) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(model)));
        this.translucent = translucent;
        scaleWidth = scale;
        scaleHeight = scale;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0f;
    }

    public static <T extends ScribeConstruct> ScribeConstructRenderer<T> hand(EntityRendererProvider.Context ctx) {
        return new ScribeConstructRenderer<>(ctx, "scribe_hand", org.papiricoh.supernaturalcraft.entity.boss.metatron.ScribeHandEntity.SCALE, true);
    }

    public static <T extends ScribeConstruct> ScribeConstructRenderer<T> book(EntityRendererProvider.Context ctx) {
        return new ScribeConstructRenderer<>(ctx, "scribe_book", 2.0f, false);
    }

    /** The Hand gives light rather than taking it: drawn at full brightness, day or night. */
    @Override
    public void render(T entity, float yaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, translucent ? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT : light);
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return translucent ? RenderType.entityTranslucent(texture) : super.getRenderType(animatable, texture, bufferSource, partialTick);
    }
}
