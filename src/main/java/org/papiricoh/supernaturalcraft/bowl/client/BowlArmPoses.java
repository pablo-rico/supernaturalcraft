package org.papiricoh.supernaturalcraft.bowl.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * Third-person pose for carrying the spell bowl, added to vanilla's ArmPose enum through
 * META-INF/enumextensions.json: two-handed, both arms forward and down, hands on either side of the
 * bowl in front of the belly. A gentle bob while walking.
 */
public final class BowlArmPoses {

    /** Arms' forward lift (radians, negative is forward) and inward turn. */
    public static final float LIFT = -0.75f, INWARD = 0.2f;

    private static final IArmPoseTransformer CARRY_TRANSFORM = new IArmPoseTransformer() {
        @Override
        public void applyTransform(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
            float bob = 0.04f * Mth.sin(entity.walkAnimation.position() * 0.6662f) * Math.min(1, entity.walkAnimation.speed());
            model.rightArm.xRot = LIFT + bob;
            model.rightArm.yRot = -INWARD;
            model.rightArm.zRot = 0;
            model.leftArm.xRot = LIFT + bob;
            model.leftArm.yRot = INWARD;
            model.leftArm.zRot = 0;
        }
    };

    public static final EnumProxy<HumanoidModel.ArmPose> CARRY = new EnumProxy<>(HumanoidModel.ArmPose.class, true, CARRY_TRANSFORM);

    private BowlArmPoses() {
    }
}
