package org.papiricoh.supernaturalcraft.client.heaven.render.figure;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.jetbrains.annotations.Nullable;

/**
 * A player's shape in a skin (v0.18): the viewer themselves in a memory or a training copy ({@code @owner}), or, in a pale wash,
 * the stand-in silhouette for a figure nothing else can draw. Vanilla's player model, posed by {@link FigurePose} over its own
 * animation (walking, a swing) when it stands in for a living thing.
 */
public final class SkinFigure {

    private final PlayerModel<LivingEntity> wide, slim;
    private @Nullable ArmorStand stand;

    public SkinFigure(EntityRendererProvider.Context ctx) {
        wide = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
        slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    /** The viewer's own skin (Steve's while they have none). */
    public static PlayerSkin viewerSkin() {
        var p = Minecraft.getInstance().player;
        return p != null ? p.getSkin() : DefaultPlayerSkin.get(net.minecraft.Util.NIL_UUID);
    }

    /** A hunter's skin by their UUID (as the connection knows it), or their default one. */
    public static PlayerSkin skinOf(java.util.UUID id) {
        var connection = Minecraft.getInstance().getConnection();
        var info = connection == null ? null : connection.getPlayerInfo(id);
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(id);
    }

    /**
     * Draws the player model with its feet at the pose's origin.
     *
     * @param living    what it stands in for if that is alive (its walk and swing drive the limbs), else null
     * @param headYaw   head turn from the body, degrees (vanilla's sense)
     * @param headPitch head tilt, degrees (vanilla's sense: positive down)
     */
    public void draw(PoseStack ps, MultiBufferSource buffers, PlayerSkin skin, FigurePose pose, @Nullable LivingEntity living,
                     float bodyYaw, float headYaw, float headPitch, int argb, int light, float partial) {
        PlayerModel<LivingEntity> model = skin.model() == PlayerSkin.Model.SLIM ? slim : wide;
        LivingEntity driver = living != null ? living : stand();
        if (driver == null) return;
        float walkPos = 0, walkSpeed = 0, age = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime() + partial;
        if (living != null) {
            walkSpeed = Math.min(1, living.walkAnimation.speed(partial));
            walkPos = living.walkAnimation.position(partial);
            model.attackTime = living.getAttackAnim(partial);
        } else {
            model.attackTime = 0;
        }
        model.crouching = false;
        model.riding = false;
        model.young = false;
        model.setAllVisible(true);
        model.prepareMobModel(driver, walkPos, walkSpeed, partial);
        model.setupAnim(driver, walkPos, walkSpeed, age, pose == FigurePose.STAND ? headYaw : 0, pose == FigurePose.STAND ? headPitch : 0);
        apply(model.body, pose.body());
        apply(model.head, pose.head());
        apply(model.rightArm, pose.rightArm());
        apply(model.leftArm, pose.leftArm());
        apply(model.rightLeg, pose.rightLeg(false));
        apply(model.leftLeg, pose.leftLeg(false));
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);

        ps.pushPose();
        ps.translate(0, -pose.sink(), 0);
        ps.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
        if (pose.lying()) {
            ps.translate(0, 0.15, 0);
            ps.mulPose(Axis.XP.rotationDegrees(90f));
            ps.translate(0, -0.95, 0);
        }
        ps.scale(-1, -1, 1);
        ps.scale(0.9375f, 0.9375f, 0.9375f);
        ps.translate(0, -1.501f, 0);
        RenderType type = (argb >>> 24) < 250 ? RenderType.entityTranslucent(skin.texture()) : RenderType.entityCutoutNoCull(skin.texture());
        model.renderToBuffer(ps, buffers.getBuffer(type), light, OverlayTexture.NO_OVERLAY, argb);
        ps.popPose();
    }

    /** {@code part} turned further by a pose's rotation (vanilla's x is the other way round). */
    private static void apply(ModelPart part, FigurePose.Rot r) {
        if (r == null || r.none()) return;
        part.xRot += FigurePose.vanillaX(r);
        part.yRot += r.y();
        part.zRot += r.z();
    }

    /** A still body for the model's animation when the figure is not a living thing. */
    private @Nullable ArmorStand stand() {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        if (stand == null || stand.level() != level) stand = new ArmorStand(level, 0, 0, 0);
        return stand;
    }
}
