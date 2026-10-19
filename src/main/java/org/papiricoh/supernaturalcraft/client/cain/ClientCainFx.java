package org.papiricoh.supernaturalcraft.client.cain;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.CainFxPayload;

/**
 * Cain's moments on the client (v0.21): each {@link CainFxPayload} kind played out — the title cards ({@link CainOverlay}) and the
 * fight's effects in the world ({@link CainWorldFx}: the swarm, the lunge's trail, the furrow, Abel's blood, the thrown tools,
 * the execution's blink, the Mark's flare, the offer of the Mark).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientCainFx {

    private ClientCainFx() {
    }

    public static void handle(CainFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        switch (p.kind()) {
            case CainFxPayload.TITLE -> CainOverlay.title(p.arg(), p.arg2() == 1, p.duration());
            case CainFxPayload.SWARM -> CainWorldFx.swarm(p.point(), p.arg(), p.duration());
            case CainFxPayload.LUNGE -> CainWorldFx.lunge(p.entity(), p.arg(), p.point(), p.duration());
            case CainFxPayload.FURROW -> CainWorldFx.furrow(p.point(), p.arg(), p.arg2(), p.duration());
            case CainFxPayload.SPIKES -> CainWorldFx.spikes(p.point(), p.arg() / 10f, p.arg2() / 10f, p.duration());
            case CainFxPayload.TOOLS -> CainWorldFx.tools(p.entity(), p.arg(), p.point(), p.duration());
            case CainFxPayload.EXECUTION -> CainWorldFx.execution(p.entity(), p.arg(), p.point(), p.duration());
            case CainFxPayload.MARK_FLARE -> CainWorldFx.flare(p.entity(), p.point(), p.arg() / 10f, p.duration());
            case CainFxPayload.FLARE_SHELTER -> CainWorldFx.shelter(p.entity(), p.point(), p.arg2() / 10f);
            case CainFxPayload.OFFER -> CainWorldFx.offer(p.entity(), p.arg(), p.arg2() == 1, p.point(), p.duration());
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) return;
        CainOverlay.tick();
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    /** Forget everything (left the world, or a preview between scenes). The world effects live in the magic's manager. */
    public static void clear() {
        CainOverlay.clear();
        CainWorldFx.clear();
    }
}
