package org.papiricoh.supernaturalcraft.client.leviathan;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.DickFxPayload;

/**
 * Plays out the moments of Dick Roman's fight and the Leviathans' (v0.25, {@link DickFxPayload}): the title cards
 * ({@link DickOverlay}) and the effects in the world ({@link DickWorldFx}: the downsizing shockwave, the contract's signature,
 * the bite, the swallow, the goo, the copies' shimmer, the tentacles, the surge, borax, a severed neck and a maw opening).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientDickFx {

    private ClientDickFx() {
    }

    public static void handle(DickFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        switch (p.kind()) {
            case DickFxPayload.TITLE -> DickOverlay.title(p.arg(), p.arg2() == 1, p.duration());
            case DickFxPayload.DOWNSIZE -> DickWorldFx.downsize(p.point(), p.arg() / 10f, p.duration());
            case DickFxPayload.CONTRACT -> DickWorldFx.contract(p.arg(), p.point(), p.duration());
            case DickFxPayload.BITE -> DickWorldFx.bite(p.point(), p.arg(), p.arg2() / 10f, p.duration());
            case DickFxPayload.SWALLOW -> DickWorldFx.swallow(p.entity(), p.arg(), p.arg2() == 1, p.point(), p.duration());
            case DickFxPayload.GOO_SPLASH -> DickWorldFx.gooSplash(p.entity(), p.point(), p.arg() / 10f, p.duration());
            case DickFxPayload.COPY -> DickWorldFx.copy(p.entity(), p.arg2() == 1, p.point(), p.duration());
            case DickFxPayload.EXPOSED -> DickWorldFx.exposed(p.entity(), p.arg(), p.point(), p.duration());
            case DickFxPayload.TENTACLE -> DickWorldFx.tentacle(p.point(), p.arg(), p.arg2() / 10f, p.duration());
            case DickFxPayload.SURGE -> DickWorldFx.surge(p.point(), p.arg(), p.duration());
            case DickFxPayload.BORAX -> DickWorldFx.borax(p.point(), p.arg() / 10f, p.arg2(), p.duration());
            case DickFxPayload.DECAPITATE -> DickWorldFx.decapitate(p.entity(), p.point(), p.duration());
            case DickFxPayload.REVEAL -> DickWorldFx.reveal(p.entity(), p.point(), p.duration());
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) return;
        DickOverlay.tick();
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    /** Forget everything (left the world, or a preview between scenes). The world effects live in the magic's manager. */
    public static void clear() {
        DickOverlay.clear();
    }
}
