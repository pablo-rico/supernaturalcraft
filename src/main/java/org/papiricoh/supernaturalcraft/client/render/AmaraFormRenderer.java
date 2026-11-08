package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.OptionalGlowLayer;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

/**
 * Amara as a woman (v0.42): the {@code amara_form} rig ({@link AmaraAssets#RIG_FORM}) in one of her ages. The age picks the
 * texture and the scale ({@link AmaraAssets.Age}); a child's head is drawn larger ({@link #headScale}); the baby is the
 * {@code bundle} bone alone. Which {@link AmaraAssets#TOGGLED} bones show is the caller's ({@code shown}): the boss decides by
 * phase, a visitor by its scene. Usable for any GeckoLib entity on this rig: {@code new AmaraFormRenderer<>(ctx, e -> age, e -> bones)}.
 */
public class AmaraFormRenderer<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T> {

    public static final ResourceLocation GEO = SupernaturalCraft.asResource(AmaraAssets.FORM_GEO);
    public static final ResourceLocation ANIM = SupernaturalCraft.asResource(AmaraAssets.FORM_ANIM);
    /** Every humanoid bone hangs from this one (hidden for the baby). */
    static final String BODY = "body";

    private final Function<T, AmaraAssets.Age> age;
    private final Function<T, Collection<String>> shown;

    public AmaraFormRenderer(EntityRendererProvider.Context ctx, Function<T, AmaraAssets.Age> age, Function<T, Collection<String>> shown) {
        super(ctx, new FormModel<>(age));
        this.age = age;
        this.shown = shown;
        addRenderLayer(new OptionalGlowLayer<>(this));
        shadowRadius = 0.4f;
    }

    /** The woman by default, nothing toggled. */
    public AmaraFormRenderer(EntityRendererProvider.Context ctx) {
        this(ctx, e -> AmaraAssets.Age.WOMAN, e -> Set.of());
    }

    /** Whether GeckoLib has the rig's files (the art may still be on its way). */
    public static boolean ready() {
        return GeoGuard.ready(GEO, ANIM);
    }

    static AmaraAssets.Age ageOr(AmaraAssets.Age a) {
        return a == null ? AmaraAssets.Age.WOMAN : a;
    }

    /** A child's head is larger for her body: the girl's by a third, the teen's a little. */
    static float headScale(AmaraAssets.Age a) {
        return switch (a) {
            case GIRL -> 1.3f;
            case TEEN -> 1.12f;
            default -> 1f;
        };
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!ready()) return;
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, T entity, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            AmaraAssets.Age a = ageOr(age.apply(entity));
            scaleWidth = scaleHeight = a.scale;
            shadowRadius = 0.4f * a.scale;
            Collection<String> on = shown.apply(entity);
            boolean baby = a == AmaraAssets.Age.BABY;
            show(BODY, !baby);
            for (String bone : AmaraAssets.TOGGLED) {
                if (bone.equals("bundle")) continue;
                show(bone, !baby && on.contains(bone));
            }
            show("bundle", baby);
        }
        super.preRender(pose, entity, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    private void show(String bone, boolean visible) {
        getGeoModel().getBone(bone).ifPresent(b -> {
            b.setHidden(!visible);
            b.setChildrenHidden(!visible);
        });
    }

    @Override
    protected float getDeathMaxRotation(T entity) {
        return 0f;
    }

    static class FormModel<T extends LivingEntity & GeoEntity> extends DefaultedEntityGeoModel<T> {

        private final Function<T, AmaraAssets.Age> age;

        FormModel(Function<T, AmaraAssets.Age> age) {
            super(SupernaturalCraft.asResource(AmaraAssets.RIG_FORM), true);
            this.age = age;
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return SupernaturalCraft.asResource(ageOr(age.apply(animatable)).texturePath());
        }

        @Override
        public void setCustomAnimations(T e, long instanceId, AnimationState<T> state) {
            super.setCustomAnimations(e, instanceId, state);
            // Clips never scale the head (the contract), so this is set outright every frame.
            GeoBone head = getAnimationProcessor().getBone("head");
            if (head != null) {
                float s = headScale(ageOr(age.apply(e)));
                head.setScaleX(s);
                head.setScaleY(s);
                head.setScaleZ(s);
            }
        }
    }
}
