package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The camera player's own arm (skin, slim or wide, sleeve) drawn in first person from a hand
 * position on an item toward a fixed shoulder, at true size. The same maths as the Colt's arms
 * ({@code ColtArmsLayer}), for any item renderer to use.
 *
 * <p>The first-person pose stack starts with the camera's rotation (GameRenderer.renderItemInHand):
 * it is camera-centred but world-aligned. Shoulders are given in camera space (x right, y up, -z
 * ahead) and turned by the camera's rotation, so the arm stays with the view when looking up or down.
 */
public final class FirstPersonArms {

    private FirstPersonArms() {
    }

    /**
     * Draws {@code arm} with its hand at the current origin of {@code pose}, running back to
     * {@code shoulder} (camera space).
     *
     * @param twist turn of the arm about its own length (degrees)
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                            HumanoidArm arm, Vector3f shoulder, float twist) {
        Minecraft mc = Minecraft.getInstance();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.pushPose();
        Quaternionf view = mc.gameRenderer.getMainCamera().rotation();
        Vector3f hand = pose.last().pose().transformPosition(new Vector3f());
        Vector3f toShoulder = view.transform(new Vector3f(shoulder)).sub(hand).normalize();
        pose.last().pose().translation(hand);
        pose.last().normal().identity();
        // The arm model runs from its shoulder down +Y to the hand. A fixed frame: -Y toward the
        // shoulder, +X as close to the view's right as that allows, so the arm never rolls about
        // its own length as the hand sways.
        Vector3f yAxis = new Vector3f(toShoulder).negate();
        Vector3f xAxis = view.transform(new Vector3f(1, 0, 0));
        xAxis.sub(new Vector3f(yAxis).mul(xAxis.dot(yAxis)));
        if (xAxis.lengthSquared() < 1e-6f) xAxis.set(0, 0, 1);
        xAxis.normalize();
        Vector3f zAxis = new Vector3f(xAxis).cross(yAxis);
        pose.mulPose(new Quaternionf().setFromNormalized(new Matrix3f(xAxis, yAxis, zAxis)));
        pose.mulPose(Axis.YP.rotationDegrees(twist));
        pose.translate(side * 6 / 16f, -12 / 16f, 0);
        PlayerRenderer pr = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        if (arm == HumanoidArm.RIGHT) pr.renderRightHand(pose, buffers, light, player);
        else pr.renderLeftHand(pose, buffers, light, player);
        pose.popPose();
    }
}
