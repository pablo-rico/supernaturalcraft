package org.papiricoh.supernaturalcraft.client.colt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.reward.ColtItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * In first person, the shooter's own arms (skin, slim or wide, sleeves) holding the gun: the gun
 * hand at the {@code hand_r} anchor always, the other hand at {@code hand_l} while it reloads or
 * turns the gun over. Each arm runs from its anchor back toward the shoulder, at true size.
 */
public class ColtArmsLayer extends GeoRenderLayer<ColtItem> {

    /** Where the shoulders sit in camera space (x right, y up, -z ahead), for the gun arm and the other. */
    /** Turn of each arm about its own length (degrees), back of the hand outward. */
    public static final float ARM_TWIST = 70, SUPPORT_TWIST = 60;
    public static final Vector3f GUN_SHOULDER = new Vector3f(0.5f, -1.35f, -0.3f), SUPPORT_SHOULDER = new Vector3f(-0.25f, -1.35f, -0.3f);

    public ColtArmsLayer(ColtRenderer renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, ColtItem animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        ColtRenderer renderer = (ColtRenderer) getRenderer();
        boolean gunHand = bone.getName().equals("hand_r");
        if (!gunHand && !bone.getName().equals("hand_l")) return;
        if (!renderer.firstPerson() || (SNClientConfig.SPEC.isLoaded() && !SNClientConfig.COLT_FIRST_PERSON_ARMS.get())) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getCameraEntity() instanceof AbstractClientPlayer player) || player.isInvisible()) return;
        if (!gunHand && !supportHandShown(renderer, partialTick)) return;
        HumanoidArm arm = gunHand ? player.getMainArm() : player.getMainArm().getOpposite();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;

        poseStack.pushPose();
        poseStack.translate(bone.getPivotX() / 16f, bone.getPivotY() / 16f, bone.getPivotZ() / 16f);
        // The hand pose stack is not in camera space: vanilla starts it with the camera's rotation
        // (GameRenderer.renderItemInHand), so it is camera-centred but world-aligned. Shoulders are
        // given in camera space, so turn them by the camera's rotation to keep them with the view.
        Quaternionf view = mc.gameRenderer.getMainCamera().rotation();
        Vector3f hand = poseStack.last().pose().transformPosition(new Vector3f());
        // Shoulders are given for a right-handed shooter; a left-handed one mirrors them.
        int mirror = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        Vector3f shoulder = view.transform(new Vector3f(gunHand ? GUN_SHOULDER : SUPPORT_SHOULDER).mul(mirror, 1, 1));
        Vector3f toShoulder = shoulder.sub(hand).normalize();
        poseStack.last().pose().translation(hand);
        poseStack.last().normal().identity();
        // The arm model runs from its shoulder down +Y to the hand. Build a fixed frame: -Y toward the
        // shoulder, +X kept as close to the view's right as that allows, so the arm never rolls
        // about its own length as the hand sways (a shortest-arc rotation would).
        Vector3f yAxis = new Vector3f(toShoulder).negate();
        Vector3f xAxis = view.transform(new Vector3f(1, 0, 0));
        xAxis.sub(new Vector3f(yAxis).mul(xAxis.dot(yAxis)));
        if (xAxis.lengthSquared() < 1e-6f) xAxis.set(0, 0, 1);
        xAxis.normalize();
        Vector3f zAxis = new Vector3f(xAxis).cross(yAxis);
        poseStack.mulPose(new Quaternionf().setFromNormalized(new Matrix3f(xAxis, yAxis, zAxis)));
        // The back of the hand turned outward.
        poseStack.mulPose(Axis.YP.rotationDegrees(side * (gunHand ? ARM_TWIST : -SUPPORT_TWIST)));
        poseStack.translate(side * 6 / 16f, -12 / 16f, 0);
        PlayerRenderer pr = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        if (arm == HumanoidArm.RIGHT) pr.renderRightHand(poseStack, bufferSource, packedLight, player);
        else pr.renderLeftHand(poseStack, bufferSource, packedLight, player);
        poseStack.popPose();
    }

    private static boolean supportHandShown(ColtRenderer renderer, float partialTick) {
        return ColtItem.reloading(renderer.stack()) || ColtClient.sinceInspect(renderer.gunId(), partialTick) < 48;
    }
}
