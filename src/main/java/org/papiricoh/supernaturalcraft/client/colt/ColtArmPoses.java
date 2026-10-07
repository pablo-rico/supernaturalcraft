package org.papiricoh.supernaturalcraft.client.colt;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * Third-person arm poses for the Colt, added to vanilla's ArmPose enum through
 * META-INF/enumextensions.json: the gun arm levelled along the gaze, or both hands low over an
 * open cylinder while reloading.
 */
public final class ColtArmPoses {

    private static final IArmPoseTransformer AIM_TRANSFORM = new IArmPoseTransformer() {
        @Override
        public void applyTransform(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
            var gun = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            // Levelled along the gaze, thrown up by the kick of the last shot and settling back.
            float kick = entity instanceof net.minecraft.world.entity.player.Player p ? kick(ColtClient.sincePlayerShot(p)) : 0;
            gun.xRot = Mth.clamp(model.head.xRot, -1.2f, 1.2f) - Mth.HALF_PI - kick;
            gun.yRot = model.head.yRot - 0.08f * side;
            gun.zRot = 0;
        }
    };

    private static final IArmPoseTransformer RELOAD_TRANSFORM = new IArmPoseTransformer() {
        @Override
        public void applyTransform(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
            var gun = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            var other = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            gun.xRot = -0.95f;
            gun.yRot = -0.35f * side;
            other.xRot = -1.1f;
            other.yRot = 0.55f * side;
        }
    };

    /** The arm's upward throw (radians) {@code ticks} after a shot: sharp, then eased back down. */
    static float kick(double ticks) {
        if (ticks < 0 || ticks > 8) return 0;
        double t = ticks + net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        return (float) (0.5 * Math.exp(-t / 2.2) * Math.min(1, t * 2));
    }

    public static final EnumProxy<HumanoidModel.ArmPose> AIM = new EnumProxy<>(HumanoidModel.ArmPose.class, false, AIM_TRANSFORM);
    public static final EnumProxy<HumanoidModel.ArmPose> RELOAD = new EnumProxy<>(HumanoidModel.ArmPose.class, true, RELOAD_TRANSFORM);

    private ColtArmPoses() {
    }
}
