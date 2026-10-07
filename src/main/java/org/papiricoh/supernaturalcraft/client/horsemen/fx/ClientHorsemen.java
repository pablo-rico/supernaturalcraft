package org.papiricoh.supernaturalcraft.client.horsemen.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.HorsemenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * What the Horsemen's fights have told this client: War's illusion on the local hunter, Death's clock (counted down
 * locally between the server's updates), limbo and the light out of it, and whether the world of the dead holds the
 * arena. Everything else on the client reads it from here.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientHorsemen {

    private static int illusionLeft;
    private static boolean clockShown;
    private static int clockLeft, clockFull, limboLeft;
    private static boolean doubled, deadWorld;
    private static Vec3 exit = Vec3.ZERO;
    /** 0..1 fade of the grey (limbo full, the world of the dead a little). */
    private static float grey, lastGrey;

    private ClientHorsemen() {
    }

    public static void handle(HorsemenFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        switch (p.kind()) {
            case HorsemenFxPayload.ILLUSION -> illusionLeft = p.duration();
            case HorsemenFxPayload.CLOCK -> {
                clockShown = p.arg() >= 0;
                clockLeft = Math.max(0, p.arg());
                clockFull = Math.max(1, p.arg2());
                limboLeft = p.duration();
                doubled = p.point().x > 0.5;
                if (!clockShown) {
                    limboLeft = 0;
                    deadWorld = false;
                }
            }
            case HorsemenFxPayload.LIMBO_ENTER -> {
                limboLeft = p.duration();
                exit = p.point();
                if (mc.player != null) mc.player.playSound(AllSounds.DEATH_LIMBO_BELL.get(), 1.0f, 0.8f);
            }
            case HorsemenFxPayload.LIMBO_EXIT -> {
                limboLeft = 0;
                if (p.arg() == 1 && mc.player != null) mc.player.playSound(SoundEvents.BEACON_ACTIVATE, 0.8f, 1.4f);
            }
            case HorsemenFxPayload.WORLD_FLIP -> {
                deadWorld = p.arg() == 1;
                if (mc.player != null) mc.player.playSound(AllSounds.DEATH_WORLD_FLIP.get(), 1.0f, deadWorld ? 0.8f : 1.2f);
            }
            case HorsemenFxPayload.MOUNT -> {
                if (mc.level == null) return;
                Entity e = mc.level.getEntity(p.entity());
                Vec3 at = e != null ? e.position() : p.point();
                for (int i = 0; i < 40; i++) {
                    mc.level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x + (Math.random() - 0.5) * 3, at.y + 0.1,
                            at.z + (Math.random() - 0.5) * 3, 0, 0.02, 0);
                }
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        if (illusionLeft > 0) illusionLeft--;
        if (limboLeft > 0) limboLeft--;
        else if (clockShown && clockLeft > 0) clockLeft = Math.max(0, clockLeft - (doubled ? 2 : 1));
        lastGrey = grey;
        float want = inLimbo() ? 1f : deadWorld ? 0.55f : 0f;
        grey += (want - grey) * 0.08f;
        if (Math.abs(grey - want) < 0.002f) grey = want;
        if (clockShown && mc.player != null && !inLimbo() && clockLeft > 0 && clockLeft < 200 && clockLeft % 20 == 0) {
            mc.player.playSound(AllSounds.DEATH_CLOCK_TICK.get(), 0.7f, 1.0f + (200 - clockLeft) / 400f);
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    public static void reset() {
        illusionLeft = 0;
        clockShown = false;
        limboLeft = 0;
        deadWorld = false;
        grey = lastGrey = 0;
    }

    public static boolean illusion() {
        return illusionLeft > 0;
    }

    public static boolean clockShown() {
        return clockShown;
    }

    public static int clockLeft() {
        return clockLeft;
    }

    public static int clockFull() {
        return clockFull;
    }

    public static boolean doubled() {
        return doubled;
    }

    public static boolean inLimbo() {
        return limboLeft > 0;
    }

    public static int limboLeft() {
        return limboLeft;
    }

    public static Vec3 exit() {
        return exit;
    }

    public static boolean deadWorld() {
        return deadWorld;
    }

    public static float grey(float partial) {
        return lastGrey + (grey - lastGrey) * partial;
    }

    /** Preview hook: show a state without a fight. */
    public static void preview(int clock, int full, int limbo, boolean dead, int illusion) {
        clockShown = clock >= 0;
        clockLeft = Math.max(0, clock);
        clockFull = Math.max(1, full);
        limboLeft = limbo;
        deadWorld = dead;
        illusionLeft = illusion;
    }
}
