package org.papiricoh.supernaturalcraft.client.mark;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * The Mark of Cain on the client (v0.21): its layer on the player models ({@link MarkLayer}), the Mark on the bare right arm in
 * first person, and the bearer's Thirst by the hotbar ({@link MarkHud}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class MarkClientEvents {

    private MarkClientEvents() {
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            if (event.getSkin(model) instanceof PlayerRenderer renderer) renderer.addLayer(new MarkLayer(renderer));
        }
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, SupernaturalCraft.asResource("mark_thirst"), new MarkHud());
    }

    /**
     * The bare right arm in first person: drawn just before vanilla draws it, posed as vanilla is about to pose it (its first-person
     * set-up, {@code PlayerRenderer.renderHand}), in the same pose stack.
     */
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (event.getArm() != HumanoidArm.RIGHT) return;
        AbstractClientPlayer player = event.getPlayer();
        if (!MarkArm.marked(player)) return;
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        model.attackTime = 0;
        model.crouching = false;
        model.swimAmount = 0;
        model.setupAnim(player, 0, 0, 0, 0, 0);
        model.rightArm.xRot = 0;
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        MarkArm.draw(event.getPoseStack(), event.getMultiBufferSource(), model, slim, MarkArm.thirst(player), player.tickCount
                + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false), player.getId());
    }
}
