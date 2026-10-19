package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * The camera shaking under great rites and spells (v0.21): only with the client's {@code magicFxShake} on, and weaker the further
 * the viewer stands from the source. The strongest shake running wins; it dies away over its ticks.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class MagicShake {

    private static float strength;
    private static int life, age;

    private MagicShake() {
    }

    /**
     * Shakes the view for {@code ticks} by up to {@code power} degrees when the viewer is at {@code at}, fading to nothing at
     * {@code range} blocks.
     */
    public static void kick(Vec3 at, float power, int ticks, double range) {
        if (!enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) return;
        double d = mc.gameRenderer.getMainCamera().getPosition().distanceTo(at);
        float k = (float) Mth.clamp(1 - d / range, 0, 1);
        float s = power * k * k;
        if (s < 0.05f) return;
        if (s < strength * (1 - age / (float) Math.max(1, life))) return;
        strength = s;
        life = Math.max(1, ticks);
        age = 0;
    }

    static void tick() {
        if (age < life) age++;
    }

    static void reset() {
        strength = 0;
        life = age = 0;
    }

    private static boolean enabled() {
        try {
            return SNClientConfig.MAGIC_FX_SHAKE.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (age >= life || strength <= 0) return;
        float partial = (float) event.getPartialTick();
        float p = (age + partial) / life, s = strength * (1 - p) * (1 - p);
        float t = (age + partial) * 2.3f;
        event.setYaw(event.getYaw() + (Mth.sin(t * 1.3f) + 0.5f * Mth.sin(t * 3.7f)) * s);
        event.setPitch(event.getPitch() + (Mth.cos(t * 1.1f) + 0.5f * Mth.sin(t * 4.3f)) * s * 0.8f);
        event.setRoll(event.getRoll() + Mth.sin(t * 0.9f) * s * 0.35f);
    }
}
