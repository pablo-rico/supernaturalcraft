package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.weapon.melee.AngelBladeItem;
import org.papiricoh.supernaturalcraft.weapon.melee.AngelicBladeAssets;
import org.papiricoh.supernaturalcraft.weapon.melee.AngelicBladeItem;

/**
 * An angelic blade in first person: the bare arm across the view from the lower corner, as vanilla draws an empty hand (and
 * its punch is the stab), crossing the view, with the blade out of the fist toward the crosshair. While the Angel Blade
 * gathers light the arm draws back.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class AngelicBladeHands {

    /** How far the arm crosses toward the middle of the view, beyond vanilla's empty hand (degrees about the view's Y). */
    static final float CROSS = 35f;
    /**
     * The blade's roll in the fist (as {@code geoDagger}'s: model +Y goes to (0, cos, -sin) of the hand's frame, Y the thumb's
     * side, Z up the forearm): out of the top of the fist and across the view toward the crosshair.
     */
    static final float ROLL = 45f;
    /** How far below the grip's middle the fist closes (model pixels): the guard and the top of the grip show above it. */
    static final float GRIP_SHOW = 2.5f;
    /** Vanilla's third-person handle point in the hand's frame (pixels), where the grip goes. */
    static final float HANDLE_Y = -3.1f, HANDLE_Z = 1.76f;

    private AngelicBladeHands() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (!(stack.getItem() instanceof AngelicBladeItem) || player == null || player.isInvisible()) return;
        event.setCanceled(true);
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float charge = player.isUsingItem() && player.getUsedItemHand() == event.getHand() && stack.getItem() instanceof AngelBladeItem
                ? Math.min(1f, (player.getTicksUsingItem() + event.getPartialTick()) / AngelBladeItem.FULL_CHARGE) : 0f;
        draw(mc, player, stack, arm, pose, event.getMultiBufferSource(), event.getPackedLight(),
                event.getEquipProgress(), event.getSwingProgress(), charge);
        pose.popPose();
    }

    private static void draw(Minecraft mc, AbstractClientPlayer player, ItemStack stack, HumanoidArm arm, PoseStack pose,
                             MultiBufferSource buffers, int light, float equip, float swing, float charge) {
        boolean right = arm == HumanoidArm.RIGHT;
        float f = right ? 1f : -1f;
        // Vanilla's empty hand (ItemInHandRenderer.renderPlayerArm), its punch included...
        float s = Mth.sqrt(swing);
        float ease = charge * charge * (3 - 2 * charge);
        pose.translate(f * (-0.3f * Mth.sin(s * Mth.PI) + 0.64f), 0.4f * Mth.sin(s * Mth.TWO_PI) - 0.6f + equip * -0.6f - ease * 0.08f,
                -0.4f * Mth.sin(swing * Mth.PI) - 0.72f + ease * 0.18f);
        pose.mulPose(Axis.YP.rotationDegrees(f * (45f + CROSS)));
        pose.mulPose(Axis.YP.rotationDegrees(f * Mth.sin(s * Mth.PI) * 70f));
        pose.mulPose(Axis.ZP.rotationDegrees(f * Mth.sin(swing * swing * Mth.PI) * -20f));
        // ...drawn back and raised while it charges.
        pose.mulPose(Axis.XP.rotationDegrees(ease * 18f));
        pose.translate(f * -1f, 3.6f, 3.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(f * 120f));
        pose.mulPose(Axis.XP.rotationDegrees(200f));
        pose.mulPose(Axis.YP.rotationDegrees(f * -135f));
        pose.translate(f * 5.6f, 0f, 0f);
        PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        if (right) renderer.renderRightHand(pose, buffers, light, player);
        else renderer.renderLeftHand(pose, buffers, light, player);
        // The blade in that fist: the hand's frame as ItemInHandLayer makes it, then the grip on vanilla's handle point.
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        model.translateToHand(arm, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90f));
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        pose.translate(f / 16f, 0.125f, -0.625f);
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        float scale = 0.7f * Math.min(1f, 20f / AngelicBladeAssets.height(id)), along = (AngelicBladeAssets.grip(id) - GRIP_SHOW) * scale;
        double r = Math.toRadians(ROLL);
        pose.translate(0f, (float) (HANDLE_Y - along * Math.cos(r)) / 16f, (float) (HANDLE_Z + along * Math.sin(r)) / 16f);
        pose.mulPose(Axis.YP.rotationDegrees(f * -90f));
        pose.mulPose(Axis.ZP.rotationDegrees(f * ROLL));
        pose.scale(scale, scale, scale);
        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack, ItemDisplayContext.NONE, !right, pose, buffers, light);
    }
}
