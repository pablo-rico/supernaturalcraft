package org.papiricoh.supernaturalcraft.compat.pal.client;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.allegiance.ClientAllegiance;
import org.papiricoh.supernaturalcraft.client.flight.FlightPose;
import org.papiricoh.supernaturalcraft.client.flight.Flyers;
import org.papiricoh.supernaturalcraft.reward.michael.WingFlight;

/**
 * The flight layer (v0.20): one per player, it poses whoever flies on wings (an angel's own, or the Seraph Wings) through
 * {@link FlightPose}. Its own player flies when their abilities say so; anyone else's abilities never reach us, so they fly
 * when they have been off the ground a while on wings without simply falling. The whole body turns on PlayerAnimationLib's
 * root bone ({@code body}); the limbs are set over the vanilla pose, which the layer eases back to on landing.
 */
final class PalFlight implements IAnimation {

    /** Ticks in the air before someone else on wings counts as flying. */
    static final int REMOTE_AIR_TICKS = 10;

    private final AbstractClientPlayer player;
    private final FlightPose pose = new FlightPose();
    private FlightPose.Frame frame = FlightPose.Frame.REST;
    private double lastX, lastY, lastZ;
    private float lastYaw;
    private boolean seeded;
    private int airTicks;

    PalFlight(AbstractClientPlayer player) {
        this.player = player;
    }

    @Override
    public void tick(AnimationData data) {
        double dx = player.getX() - lastX, dy = player.getY() - lastY, dz = player.getZ() - lastZ;
        float yaw = player.getYRot() - lastYaw;
        if (!seeded || dx * dx + dy * dy + dz * dz > 64) {
            // First tick, or a teleport: no speed to read.
            dx = dy = dz = 0;
            yaw = 0;
            seeded = true;
        }
        lastX = player.getX();
        lastY = player.getY();
        lastZ = player.getZ();
        lastYaw = player.getYRot();
        airTicks = player.onGround() || player.isInWater() ? 0 : airTicks + 1;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        boolean flying = flying(horizontal, dy);
        Flyers.set(player, flying);
        pose.tick(flying, horizontal, dy, yaw);
    }

    private boolean flying(double horizontal, double vertical) {
        if (player.isSpectator() || player.isPassenger() || player.isFallFlying() || player.isSwimming() || player.isSleeping()) return false;
        if (!(WingFlight.wearingWings(player) || ClientAllegiance.wingsGranted(player))) return false;
        if (player == Minecraft.getInstance().player) return player.getAbilities().flying;
        return airTicks >= REMOTE_AIR_TICKS && (vertical > -0.15 || horizontal > 0.25);
    }

    @Override
    public void setupAnim(AnimationData data) {
        frame = pose.frame(data.getPartialTick());
    }

    @Override
    public boolean isActive() {
        return pose.active();
    }

    @Override
    public PlayerAnimBone get3DTransform(PlayerAnimBone bone) {
        FlightPose.Frame f = frame;
        float b = f.blend();
        if (b <= 0) return bone;
        switch (bone.getName()) {
            case "body" -> {
                // The root turns about the waist; the library negates X there, so forward is positive (seen in game).
                bone.setRotX(f.bodyPitch() * Mth.DEG_TO_RAD);
                bone.setRotZ(f.bodyRoll() * Mth.DEG_TO_RAD);
                bone.setPosY(f.rise());
            }
            case "head" -> bone.setRotX(bone.getRotX() + f.headPitch() * Mth.DEG_TO_RAD * b);
            case "right_arm" -> limb(bone, f.armBack(), f.armOut(), b);
            case "left_arm" -> limb(bone, f.armBack(), -f.armOut(), b);
            case "right_leg" -> limb(bone, f.rightLegBack(), f.legOut(), b);
            case "left_leg" -> limb(bone, f.leftLegBack(), -f.legOut(), b);
            default -> {
            }
        }
        return bone;
    }

    /** A hanging limb: positive X swings it back, positive Z takes a right limb out (a left one in, hence the sign). */
    private static void limb(PlayerAnimBone bone, float back, float out, float blend) {
        bone.setRotX(Mth.lerp(blend, bone.getRotX(), back * Mth.DEG_TO_RAD));
        bone.setRotY(Mth.lerp(blend, bone.getRotY(), 0));
        bone.setRotZ(Mth.lerp(blend, bone.getRotZ(), out * Mth.DEG_TO_RAD));
    }

    @Override
    public FirstPersonMode getFirstPersonMode() {
        return FirstPersonMode.DISABLED;
    }
}
