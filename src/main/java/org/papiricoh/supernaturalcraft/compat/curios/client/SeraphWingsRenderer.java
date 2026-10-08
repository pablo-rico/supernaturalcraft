package org.papiricoh.supernaturalcraft.compat.curios.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.client.michael.render.SeraphWingsDraw;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;


/**
 * The Seraph Wings on a player's back (Curios back slot): drawn by {@link SeraphWingsDraw}, shared with the chest-slot
 * layer used without Curios.
 */
public class SeraphWingsRenderer implements ICurioRenderer {

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack pose,
            RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount, float partial,
            float ageInTicks, float netHeadYaw, float headPitch) {
        SeraphWingsDraw.draw(slot.entity(), parent.getModel(), pose, buffers, partial, ageInTicks);
    }
}
