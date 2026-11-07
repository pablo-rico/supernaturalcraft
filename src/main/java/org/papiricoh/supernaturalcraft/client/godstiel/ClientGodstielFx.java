package org.papiricoh.supernaturalcraft.client.godstiel;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.GodstielFxPayload;

/**
 * Godstiel's moments on the client (v0.39): each {@link GodstielFxPayload} kind played out — the title cards and the voices
 * ({@link GodstielOverlay}), his bar's struggle ({@link GodstielHud}) and the fight in the world ({@link GodstielWorldFx}).
 * Particles only here, never from the server.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientGodstielFx {

    private ClientGodstielFx() {
    }

    public static void handle(GodstielFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Vec3 at = p.point();
        switch (p.kind()) {
            case GodstielFxPayload.TITLE -> GodstielOverlay.title(p.arg(), p.arg2(), p.duration());
            case GodstielFxPayload.SMITE -> GodstielWorldFx.smite(at, p.arg() / 10f);
            case GodstielFxPayload.PALM -> GodstielWorldFx.palm(at, p.arg(), p.arg2());
            case GodstielFxPayload.RADIANCE -> GodstielWorldFx.radiance(at, p.arg() / 10f);
            case GodstielFxPayload.JUDGED -> GodstielWorldFx.judged(at);
            case GodstielFxPayload.BOLT -> GodstielWorldFx.bolt(at, at.add(p.arg() / 10.0, 0, p.arg2() / 10.0), p.duration() < 0);
            case GodstielFxPayload.CRACK -> {
                Entity e = mc.level.getEntity(p.entity());
                if (e != null) GodstielWorldFx.crack(e, p.arg());
                GodstielHud.crack();
            }
            case GodstielFxPayload.GOO -> GodstielWorldFx.goo(at, p.arg(), p.arg2());
            case GodstielFxPayload.SWEEP -> GodstielWorldFx.sweep(at, p.arg() / 10f);
            case GodstielFxPayload.SURGE -> GodstielWorldFx.surge(at, p.arg() / 10f);
            case GodstielFxPayload.STRUGGLE -> {
                GodstielHud.struggle(p.arg() == 1, p.duration());
                if (p.arg() == 1 && p.arg2() == 0) GodstielWorldFx.struggle(p.entity(), p.duration());
            }
            case GodstielFxPayload.VOICES -> GodstielOverlay.voices(p.arg(), p.duration());
            case GodstielFxPayload.DOOR -> GodstielWorldFx.door(at, p.arg() == 1, p.duration());
            case GodstielFxPayload.SOULS -> GodstielWorldFx.souls(at, p.entity(), p.duration());
            case GodstielFxPayload.BLACK_WATER -> GodstielWorldFx.blackWater(at, p.duration());
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        GodstielOverlay.tick();
        GodstielWorldFx.tick();
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    /** Forget everything (left the world, or a preview between scenes). */
    public static void clear() {
        GodstielOverlay.clear();
        GodstielWorldFx.clear();
    }
}
