package org.papiricoh.supernaturalcraft.client.michael.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.datagen.michael.MichaelAssetData;
import org.papiricoh.supernaturalcraft.reward.michael.ThrownLanceEntity;

/**
 * The Lance of Michael in flight or in the ground, his ({@code MichaelLanceEntity}) or a hunter's ({@link ThrownLanceEntity}):
 * the one lance model ({@code geo/item/michael_lance.geo.json}), its tip ({@link MichaelAssetData#LANCE_TIP} along local +Z)
 * on the entity's point and its shaft along the flight; the borrowed one brighter.
 */
public class LanceRenderer<T extends AbstractArrow> extends EntityRenderer<T> {

    public static final ResourceLocation MODEL = SupernaturalCraft.asResource("geo/item/michael_lance.geo.json");
    public static final ResourceLocation ANIMATION = SupernaturalCraft.asResource("animations/item/michael_lance.animation.json");
    public static final ResourceLocation TEXTURE = SupernaturalCraft.asResource("textures/item/michael_lance.png");
    public static final ResourceLocation BORROWED = SupernaturalCraft.asResource("textures/item/michael_lance_borrowed.png");

    private static GeoProp prop;

    public LanceRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    static GeoProp prop() {
        if (prop == null) prop = new GeoProp(MODEL, TEXTURE, ANIMATION, "animation.michael_lance.idle").fullBright();
        return prop;
    }

    @Override
    public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        GeoProp p = prop();
        if (!p.ready()) return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, e.yRotO, e.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partial, e.xRotO, e.getXRot())));
        pose.translate(0, 0, -MichaelAssetData.LANCE_TIP);
        boolean borrowed = e instanceof ThrownLanceEntity t && t.borrowed();
        p.draw(pose, buffers, borrowed ? BORROWED : TEXTURE, e.tickCount + partial, partial, light);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T e) {
        return TEXTURE;
    }
}
