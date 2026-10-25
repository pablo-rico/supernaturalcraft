package org.papiricoh.supernaturalcraft.client.magic;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * The grimoire (a plain GeckoLib item, no glowmask, open only while held: {@link GrimoireItem}'s controller). In the caster's
 * own first-person hand it also carries their cast circle, drawn right after the book so it sits on the open pages.
 */
public class GrimoireRenderer extends GeoItemRenderer<GrimoireItem> {

    /** The open spread's centre in the model (GeckoLib pixels): the gutter sits on the origin, the pages rise to ~1.25. */
    private static final float SPREAD_Y = 0.6f;

    public GrimoireRenderer() {
        super(new DefaultedItemGeoModel<>(AllItems.GRIMOIRE.getId()));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light,
                             int overlay) {
        super.renderByItem(stack, context, pose, buffers, light, overlay);
        if (context.firstPerson()) {
            // GeoItemRenderer draws the model from (0.5, 0.51, 0.5) of the item cube, in pixels: this pose is the hand's.
            Vector3f spread = pose.last().pose().transformPosition(0.5f, 0.51f + SPREAD_Y / 16f, 0.5f, new Vector3f());
            MagicFxManager.renderAtBook(buffers, spread,
                    context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
        }
    }
}
