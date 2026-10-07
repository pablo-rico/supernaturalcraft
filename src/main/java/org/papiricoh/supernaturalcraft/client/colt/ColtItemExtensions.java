package org.papiricoh.supernaturalcraft.client.colt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.papiricoh.supernaturalcraft.reward.ColtItem;

/**
 * How the Colt is held. In first person: where vanilla holds an item, plus the recoil spring's
 * kick and a dip while sprinting (the swing is never played: the gun does not wave about). In
 * third person: the gun arm raised along the gaze for a moment after a shot, both hands low over
 * the cylinder while it reloads.
 */
public class ColtItemExtensions implements IClientItemExtensions {

    private ColtRenderer renderer;
    private float sprint, sprintO;
    private long sprintTick = Long.MIN_VALUE;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) renderer = new ColtRenderer();
        return renderer;
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (hand != InteractionHand.MAIN_HAND || (entity instanceof Player p && ColtPlayerAnims.get().active(p))) {
            return HumanoidModel.ArmPose.ITEM;
        }
        if (ColtItem.reloading(stack)) return ColtArmPoses.RELOAD.getValue();
        if (entity instanceof Player p && ColtClient.sincePlayerShot(p) < ColtClient.AIM_HOLD) return ColtArmPoses.AIM.getValue();
        return HumanoidModel.ArmPose.ITEM;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                           float partialTick, float equipProcess, float swingProcess) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        long tick = player.level().getGameTime();
        if (tick != sprintTick) {
            sprintTick = tick;
            sprintO = sprint;
            sprint = Mth.approach(sprint, player.isSprinting() ? 1 : 0, 0.2f);
        }
        float run = Mth.lerp(partialTick, sprintO, sprint);
        float kick = ColtRecoil.gunKick();
        poseStack.translate(side * 0.40f, -0.44f + equipProcess * -0.6f - run * 0.12f, -0.78f + kick * 0.06f);
        // Turned a touch inward, so the barrel runs toward the crosshair.
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 4 + run * 22 * side));
        poseStack.mulPose(Axis.XP.rotationDegrees(kick * 7 - run * 18));
        return true;
    }
}
