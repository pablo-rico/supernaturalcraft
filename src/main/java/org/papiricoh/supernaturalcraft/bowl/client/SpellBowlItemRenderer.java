package org.papiricoh.supernaturalcraft.bowl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlItem;
import org.papiricoh.supernaturalcraft.client.render.FirstPersonArms;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

/**
 * The bowl as an item, in every context: the block model, what is in it (from the item's
 * {@code bowl_contents}), and in first person both of the bearer's arms holding its rim. Held, the
 * liquid sways with the bearer's steps and turns.
 */
public class SpellBowlItemRenderer extends BlockEntityWithoutLevelRenderer {

    /** Where the hands grip the bowl (block space): just outside the wall, below the lip. */
    public static final Vector3f RIGHT_GRIP = new Vector3f(15.6f / 16f, 3.2f / 16f, 8f / 16f),
            LEFT_GRIP = new Vector3f(0.4f / 16f, 3.2f / 16f, 8f / 16f);
    /** Shoulders in camera space (x right, y up, -z ahead), and the arms' twist (degrees). */
    public static final Vector3f RIGHT_SHOULDER = new Vector3f(0.42f, -1.35f, -0.25f), LEFT_SHOULDER = new Vector3f(-0.42f, -1.35f, -0.25f);
    public static final float RIGHT_TWIST = 70, LEFT_TWIST = 60;

    public SpellBowlItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer().renderSingleBlock(AllBlocks.SPELL_BOWL.get().defaultBlockState(), pose, buffers, light, overlay);
        boolean firstPerson = ctx.firstPerson();
        boolean held = firstPerson || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        float time = mc.level == null ? 0 : mc.level.getGameTime() + partial;
        float tiltX = 0, tiltZ = 0;
        if (firstPerson && mc.player != null) {
            float[] sway = sway(mc.player, partial);
            tiltX = sway[0];
            tiltZ = sway[1];
        } else if (held) {
            tiltX = 0.04f * Mth.sin(time * 0.15f);
            tiltZ = 0.04f * Mth.cos(time * 0.11f);
        }
        BowlContents contents = SpellBowlItem.contents(stack);
        BowlContentsRenderer.render(contents, pose, buffers, light, overlay, time, tiltX, tiltZ, mc.level, 31);
        if (firstPerson && mc.getCameraEntity() instanceof AbstractClientPlayer player && !player.isInvisible()
                && mc.options.getCameraType().isFirstPerson()) {
            arm(pose, buffers, light, player, HumanoidArm.RIGHT, RIGHT_GRIP, RIGHT_SHOULDER, RIGHT_TWIST);
            arm(pose, buffers, light, player, HumanoidArm.LEFT, LEFT_GRIP, LEFT_SHOULDER, LEFT_TWIST);
        }
    }

    private static void arm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, HumanoidArm arm,
                            Vector3f grip, Vector3f shoulder, float twist) {
        pose.pushPose();
        pose.translate(grip.x, grip.y, grip.z);
        FirstPersonArms.draw(pose, buffers, light, player, arm, shoulder, twist);
        pose.popPose();
    }

    /**
     * How far the liquid tips (radians about X and Z): the steps' bob from vanilla's view bobbing,
     * plus the lag of the hands behind the view as the player turns.
     */
    static float[] sway(LocalPlayer player, float partial) {
        float walk = -(player.walkDist + (player.walkDist - player.walkDistO) * partial);
        float bob = Mth.lerp(partial, player.oBob, player.bob);
        float yawLag = Mth.wrapDegrees(Mth.lerp(partial, player.yBobO, player.yBob) - player.getViewYRot(partial));
        float pitchLag = Mth.lerp(partial, player.xBobO, player.xBob) - player.getViewXRot(partial);
        float x = Mth.clamp(Math.abs(Mth.cos(walk * Mth.PI)) * bob * 0.6f + pitchLag * 0.004f, -0.25f, 0.25f);
        float z = Mth.clamp(Mth.sin(walk * Mth.PI) * bob * 0.9f + yawLag * 0.004f, -0.25f, 0.25f);
        return new float[]{x, z};
    }
}
