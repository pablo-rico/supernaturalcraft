package org.papiricoh.supernaturalcraft.client.colt;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * The Colt's kick on the shooter's own view: the camera snaps up and rolls, the field of view
 * punches out, a short shake, then springs carry it all home. Added on top of whatever else moves
 * the camera (cinematics included); the aim itself never moves.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ColtRecoil {

    public static final float PITCH = 7f, YAW = 1.2f, ROLL = 3f, FOV = 0.08f, SHAKE = 0.6f;
    private static final RecoilSpring pitch = new RecoilSpring(260, 0.5), yaw = new RecoilSpring(260, 0.5),
            roll = new RecoilSpring(200, 0.45), fov = new RecoilSpring(300, 0.6), gun = new RecoilSpring(340, 0.42);
    private static long lastFrame;
    private static float shake;

    private ColtRecoil() {
    }

    private static float strength() {
        Minecraft mc = Minecraft.getInstance();
        double cfg = SNClientConfig.SPEC.isLoaded() ? SNClientConfig.COLT_CAMERA_KICK.get() : 1.0;
        return (float) (cfg * mc.options.screenEffectScale().get());
    }

    /** One shot of the local player's Colt. */
    public static void kick() {
        float s = strength();
        var random = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.random : null;
        float side = random == null ? 1 : (random.nextBoolean() ? 1 : -1);
        pitch.kick(-PITCH * s);
        yaw.kick(YAW * s * side * (random == null ? 1 : 0.5f + random.nextFloat()));
        roll.kick(ROLL * s * side);
        fov.kick(FOV * s);
        gun.kick(1);
        shake = SHAKE * s;
    }

    /** First-person gun offset, 0 at rest, about 1 at the peak of a kick. */
    public static float gunKick() {
        return (float) gun.value();
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : (now - lastFrame) / 1e9;
        lastFrame = now;
        pitch.step(dt);
        yaw.step(dt);
        roll.step(dt);
        fov.step(dt);
        gun.step(dt);
        shake = (float) Math.max(0, shake - dt * 4);
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (pitch.atRest() && yaw.atRest() && roll.atRest() && shake <= 0) return;
        float t = (float) (System.nanoTime() / 1e9 * 60);
        float jitter = shake * shake;
        event.setPitch(event.getPitch() + (float) pitch.value() + Mth.sin(t * 1.7f) * jitter);
        event.setYaw(event.getYaw() + (float) yaw.value() + Mth.cos(t * 2.3f) * jitter);
        event.setRoll(event.getRoll() + (float) roll.value());
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (!fov.atRest()) event.setFOV(event.getFOV() * (1 + fov.value()));
    }
}
