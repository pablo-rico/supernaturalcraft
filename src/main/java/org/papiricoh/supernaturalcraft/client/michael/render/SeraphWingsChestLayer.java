package org.papiricoh.supernaturalcraft.client.michael.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/** The Seraph Wings worn in the chest slot (with or without Curios), drawn as on the back slot. */
public class SeraphWingsChestLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public SeraphWingsChestLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partial, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(AllItems.SERAPH_WINGS.get()) || player.isInvisible()) return;
        SeraphWingsDraw.draw(player, getParentModel(), pose, buffers, partial, ageInTicks);
    }
}
