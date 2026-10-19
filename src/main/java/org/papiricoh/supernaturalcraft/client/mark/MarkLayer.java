package org.papiricoh.supernaturalcraft.client.mark;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;

/** The Mark of Cain on a bearer's right forearm, on the player models (v0.21; {@link MarkArm}). */
public class MarkLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public MarkLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !getParentModel().rightArm.visible || !MarkArm.marked(player)) return;
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        MarkArm.draw(pose, buffers, getParentModel(), slim, MarkArm.thirst(player), ageInTicks, player.getId());
    }
}
