package org.papiricoh.supernaturalcraft.client.crowley;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.CrowleyFxPayload;

/**
 * Crowley's moments on the client (v0.35): each {@link CrowleyFxPayload} kind played out — the title cards
 * ({@link CrowleyOverlay}) and the fight in the world ({@link CrowleyWorldFx}). A broken seal also cracks on his bar
 * ({@link CrowleyHud#sealBroken}). Particles and sounds only here, never from the server.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientCrowleyFx {

    private ClientCrowleyFx() {
    }

    public static void handle(CrowleyFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Vec3 at = p.point();
        switch (p.kind()) {
            case CrowleyFxPayload.TITLE -> CrowleyOverlay.title(p.arg(), p.arg2() == 1, p.duration());
            case CrowleyFxPayload.RED_SMOKE -> CrowleyWorldFx.smoke(at, p.arg() / 10f, p.duration(), CrowleyWorldFx.Smoke.Kind.CLOUD);
            case CrowleyFxPayload.VANISH -> CrowleyWorldFx.smoke(at, 1, p.duration(), CrowleyWorldFx.Smoke.Kind.VANISH);
            case CrowleyFxPayload.APPEAR -> CrowleyWorldFx.smoke(at, 1, p.duration(), CrowleyWorldFx.Smoke.Kind.APPEAR);
            case CrowleyFxPayload.PAPER_STORM -> CrowleyWorldFx.paperStorm(at, p.arg(), p.arg2(), p.duration());
            case CrowleyFxPayload.HELLFIRE_LINE -> CrowleyWorldFx.hellfireLine(at, at.add(p.arg() / 10.0, 0, p.arg2() / 10.0), p.duration());
            case CrowleyFxPayload.SNAP -> CrowleyWorldFx.snap(p.entity(), at, p.arg(), p.duration());
            case CrowleyFxPayload.SEAL_BREAK -> {
                CrowleyHud.sealBroken(p.arg());
                CrowleyWorldFx.sealBreak(at, p.arg(), p.duration());
            }
            case CrowleyFxPayload.HOUNDS -> CrowleyWorldFx.hounds(at, p.arg(), p.duration());
            case CrowleyFxPayload.COLLECT -> CrowleyWorldFx.collect(p.arg(), at, p.duration());
            case CrowleyFxPayload.COMPANION_POOF -> CrowleyWorldFx.smoke(at, 0.6f, p.duration(),
                    p.arg() == 1 ? CrowleyWorldFx.Smoke.Kind.POOF_IN : CrowleyWorldFx.Smoke.Kind.POOF_OUT);
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        CrowleyOverlay.tick();
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    /** Forget everything (left the world, or a preview between scenes). The world effects live in the magic's manager. */
    public static void clear() {
        CrowleyOverlay.clear();
    }
}
