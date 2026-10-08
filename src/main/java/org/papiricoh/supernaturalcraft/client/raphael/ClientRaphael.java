package org.papiricoh.supernaturalcraft.client.raphael;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.RaphaelFxPayload;

/**
 * Plays out each {@link RaphaelFxPayload} on the client (v0.16): title cards ({@link RaphaelOverlay}), the flashes (the
 * screen whitens, the nearer the brighter) and the shadow of his wings, the threads of grace, the ring of holy fire that
 * holds him and the snap ({@link RaphaelWorldFx}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientRaphael {

    private ClientRaphael() {
    }

    public static void handle(RaphaelFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        switch (p.kind()) {
            case RaphaelFxPayload.TITLE -> RaphaelOverlay.add(new RaphaelOverlay.TitleCard(p.arg(), p.arg2() == 1, p.duration()));
            case RaphaelFxPayload.FLASH -> flash(p.entity(), p.point(), p.arg() == 1, p.duration());
            case RaphaelFxPayload.TETHER -> RaphaelWorldFx.tether(p.entity(), p.arg(), p.duration());
            case RaphaelFxPayload.TRAP -> RaphaelWorldFx.trap(p.arg() >= 0 ? p.arg() : p.entity(), p.point(), p.duration());
            case RaphaelFxPayload.SNAP -> RaphaelWorldFx.snap(p.point(), p.arg(), p.arg2(), p.duration());
            default -> {
            }
        }
    }

    /** A flash of his lightning at {@code point}: the screen whitens (by how near it fell) and, if asked, his wings' shadow. */
    public static void flash(int raphael, Vec3 point, boolean wings, int duration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double d = mc.player.position().distanceTo(point);
        float strength = (float) Mth.clamp(0.4 * (1 - d / 64), 0.06, 0.4);
        RaphaelOverlay.add(new RaphaelOverlay.Flash(Math.max(5, duration), strength));
        if (wings) RaphaelWorldFx.shadow(raphael, point, Math.max(6, duration));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) return;
        RaphaelWorldFx.tick();
        RaphaelOverlay.tick();
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    /** Forget everything (left the world, or a preview between scenes). */
    public static void clear() {
        RaphaelWorldFx.clear();
        RaphaelOverlay.clear();
    }
}
