package org.papiricoh.supernaturalcraft.client.michael.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.OptionalGlowLayer;
import org.papiricoh.supernaturalcraft.reward.michael.GeneralArmorItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * The General's armour on whoever wears it: GeckoLib's armour renderer over {@code geo/item/armor/general_armor.geo.json}
 * (bones {@code armorHead}, {@code armorBody}, {@code armorRightArm}… as GeckoLib names them), with its glowmask. Until the
 * art has baked the model, nothing GeckoLib-made is drawn (the vanilla layer stands in).
 */
public class GeneralArmorRenderer extends GeoArmorRenderer<GeneralArmorItem> {

    public static final ResourceLocation MODEL = SupernaturalCraft.asResource("geo/item/armor/general_armor.geo.json");
    public static final ResourceLocation TEXTURE = SupernaturalCraft.asResource("textures/item/armor/general_armor.png");
    public static final ResourceLocation ANIMATION = SupernaturalCraft.asResource("animations/item/armor/general_armor.animation.json");

    public GeneralArmorRenderer() {
        super(new Model());
        addRenderLayer(new OptionalGlowLayer<>(this));
    }

    /** What the armour item hands GeckoLib: this renderer, made the first time it is needed and only once the model exists. */
    public static GeoRenderProvider provider() {
        return new GeoRenderProvider() {
            private GeneralArmorRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(T entity, ItemStack stack, EquipmentSlot slot,
                                                                              HumanoidModel<T> original) {
                if (!GeoGuard.ready(MODEL, ANIMATION)) return null;
                if (renderer == null) renderer = new GeneralArmorRenderer();
                return renderer;
            }
        };
    }

    static class Model extends GeoModel<GeneralArmorItem> {
        @Override
        public ResourceLocation getModelResource(GeneralArmorItem animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(GeneralArmorItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(GeneralArmorItem animatable) {
            return ANIMATION;
        }
    }
}
