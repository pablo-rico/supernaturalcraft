package org.papiricoh.supernaturalcraft.bowl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * How the bowl is held: drawn by {@link SpellBowlItemRenderer}; in third person with both arms
 * ({@link BowlArmPoses#CARRY}); in first person centred low in front of the camera, tipped toward
 * the eye so its contents show, never swung.
 */
public class SpellBowlItemExtensions implements IClientItemExtensions {

    /** First-person placement (camera space) and tilt toward the eye (degrees). */
    public static final float FP_Y = -0.18f, FP_Z = -0.64f, FP_TILT = 28;

    private SpellBowlItemRenderer renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) renderer = new SpellBowlItemRenderer();
        return renderer;
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return hand == InteractionHand.MAIN_HAND ? BowlArmPoses.CARRY.getValue() : HumanoidModel.ArmPose.ITEM;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        // Centred whichever hand is the main one: the bowl takes both.
        pose.translate(0, FP_Y - equipProcess * 0.6f, FP_Z);
        pose.mulPose(Axis.XP.rotationDegrees(FP_TILT));
        return true;
    }
}
