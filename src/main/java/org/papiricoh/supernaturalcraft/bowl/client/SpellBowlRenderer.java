package org.papiricoh.supernaturalcraft.bowl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlockEntity;

/** A placed bowl's contents (the bowl itself is the block model). */
public class SpellBowlRenderer implements BlockEntityRenderer<SpellBowlBlockEntity> {

    public SpellBowlRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(SpellBowlBlockEntity bowl, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (bowl.getLevel() == null) return;
        float time = bowl.getLevel().getGameTime() + partial;
        BowlContentsRenderer.render(bowl.contents(), pose, buffers, light, overlay, time, 0, 0, bowl.getLevel(),
                (int) bowl.getBlockPos().asLong());
    }
}
