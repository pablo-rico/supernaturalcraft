package org.papiricoh.supernaturalcraft.client.chuck.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * The local hunter's view while the Author rewrites gravity: inverted, the camera rolls smoothly over to stand on the
 * ceiling of pages (and left and right swap, so walking still feels like walking); low, loose pages and motes drift up
 * around them. Also the shake when his script cracks. The server owns the real gravity (an attribute); this is only
 * how it looks and steers.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ChuckCamera {

    private static byte mode = ChuckEntity.GRAVITY_NORMAL;
    private static int left, noAuthor;
    private static float roll, rollO;
    private static float shake;
    private static int shakeAge, shakeLife;

    private ChuckCamera() {
    }

    /** {@code GRAVITY} payload: {@code ChuckEntity.GRAVITY_*} for {@code duration} ticks (0 or less: until told otherwise). */
    public static void gravity(int newMode, int duration) {
        mode = (byte) Mth.clamp(newMode, ChuckEntity.GRAVITY_NORMAL, ChuckEntity.GRAVITY_INVERTED);
        left = duration > 0 ? duration : -1;
        noAuthor = 0;
    }

    public static void shake(float strength, int ticks) {
        if (strength < shake * (1 - (float) shakeAge / Math.max(1, shakeLife))) return;
        shake = strength;
        shakeLife = Math.max(1, ticks);
        shakeAge = 0;
    }

    public static byte mode() {
        return mode;
    }

    static void reset() {
        mode = ChuckEntity.GRAVITY_NORMAL;
        roll = rollO = 0;
        shake = 0;
        left = 0;
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }
        if (left > 0 && --left == 0) mode = ChuckEntity.GRAVITY_NORMAL;
        // Never leave a hunter upside down: no Author near for five seconds, or dead, and gravity comes back.
        if (mode != ChuckEntity.GRAVITY_NORMAL && mc.level.getGameTime() % 10 == 0) {
            boolean author = !mc.level.getEntitiesOfClass(ChuckEntity.class, mc.player.getBoundingBox().inflate(128)).isEmpty();
            noAuthor = author ? 0 : noAuthor + 10;
        }
        if (noAuthor > 100 || !mc.player.isAlive()) mode = ChuckEntity.GRAVITY_NORMAL;
        rollO = roll;
        float target = mode == ChuckEntity.GRAVITY_INVERTED ? 180 : 0;
        roll += Mth.clamp((target - roll) * 0.12f, -9, 9);
        if (Math.abs(target - roll) < 0.05f) roll = target;
        if (shakeAge < shakeLife) shakeAge++;
        if (mode == ChuckEntity.GRAVITY_LOW && mc.level.random.nextInt(2) == 0) {
            Vec3 p = mc.player.position();
            var r = mc.level.random;
            mc.level.addParticle(r.nextBoolean() ? AllParticles.PAGE_SCRAP.get() : AllParticles.GOLDEN_MOTE.get(),
                    p.x + (r.nextDouble() - 0.5) * 6, p.y + r.nextDouble() * 2.5, p.z + (r.nextDouble() - 0.5) * 6,
                    0, 0.02 + r.nextDouble() * 0.03, 0);
        }
    }

    private static float roll(float partial) {
        return Mth.lerp(partial, rollO, roll);
    }

    /** True while the view is more than half way over (left and right are swapped on screen). */
    public static boolean upsideDown() {
        return roll > 90;
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        float partial = (float) event.getPartialTick();
        float r = roll(partial);
        if (r != 0) event.setRoll(event.getRoll() + r);
        if (shakeAge < shakeLife && shake > 0) {
            float p = (shakeAge + partial) / shakeLife, s = shake * (1 - p);
            float t = (shakeAge + partial) * 1.9f;
            event.setYaw(event.getYaw() + (Mth.sin(t * 1.3f) + 0.5f * Mth.sin(t * 3.1f)) * s * 1.6f);
            event.setPitch(event.getPitch() + (Mth.cos(t * 1.7f) + 0.5f * Mth.sin(t * 3.9f)) * s * 1.2f);
        }
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        if (!upsideDown()) return;
        Input in = event.getInput();
        in.leftImpulse = -in.leftImpulse;
        boolean l = in.left;
        in.left = in.right;
        in.right = l;
    }
}
