package org.papiricoh.supernaturalcraft.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.weapon.catalyst.CenserOfGraceItem;

/** World-space effects of held weapons: the Censer's beam. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class WeaponFx {

    private WeaponFx() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = (mc.level.getGameTime() + partial) / 20f;
        boolean drew = false;
        for (Player p : mc.level.players()) {
            if (!p.isUsingItem() || !(p.getUseItem().getItem() instanceof CenserOfGraceItem)) continue;
            Vec3 hand = p.getEyePosition(partial).add(p.getViewVector(partial).scale(0.6)).add(0, -0.35, 0);
            Vec3 end = CenserOfGraceItem.trace(p).getLocation();
            float grow = Math.min(1f, p.getTicksUsingItem() / 10f);
            BeamFx.draw(pose, buffers, hand.subtract(cam), end.subtract(cam), 0.18f * grow + 0.04f, 0xFFE38A, 0.9f, time);
            drew = true;
        }
        if (drew) buffers.endBatch();
    }
}
