package org.papiricoh.supernaturalcraft.client.witchboss;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaEntity;
import org.papiricoh.supernaturalcraft.network.RowenaFxPayload;
import org.papiricoh.supernaturalcraft.util.ClientPresence;

/**
 * Rowena's moments on the client (v0.21, P6): each {@link RowenaFxPayload} kind played out — the title cards
 * ({@link WitchBossOverlay}) and the fight in the world ({@link RowenaWorldFx}: the fling's gust, Ignis and Glacies, her familiar,
 * the curses down a stolen taglock, her hexed victims rising and their fate, the recitation's letters and threads, her bowls
 * breaking, the recitation's end, her smoke gathering).
 *
 * <p>What lasts is read off her synced flags: while she floats (phase III, not stunned) the updraft and her pages; while she
 * recites, the recitation (started here too, so a late joiner sees it); when she is stunned, her fall.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientRowenaFx {

    private ClientRowenaFx() {
    }

    public static void handle(RowenaFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        switch (p.kind()) {
            case RowenaFxPayload.TITLE -> WitchBossOverlay.title(WitchBossOverlay.Who.ROWENA, p.arg(), p.arg2() == 1, p.duration());
            case RowenaFxPayload.FLING -> RowenaWorldFx.fling(p.entity(), p.point(), p.arg(), p.arg2(), p.duration());
            case RowenaFxPayload.IGNIS -> RowenaWorldFx.ignis(p.point(), p.arg() / 10f, p.duration());
            case RowenaFxPayload.GLACIES -> RowenaWorldFx.glacies(p.point(), p.arg() / 10f, p.duration());
            case RowenaFxPayload.FAMILIAR -> RowenaWorldFx.familiar(p.arg(), p.point(), p.duration());
            case RowenaFxPayload.HOMING -> RowenaWorldFx.homing(p.entity(), p.arg(), p.arg2(), p.point(), p.duration());
            case RowenaFxPayload.VICTIMS -> RowenaWorldFx.victim(p.arg(), p.point(), p.duration());
            case RowenaFxPayload.VICTIM_FATE -> RowenaWorldFx.fate(p.entity(), p.arg(), p.arg2() == 1, p.point(), p.duration());
            case RowenaFxPayload.RECITE -> RowenaWorldFx.recite(p.entity(), p.arg() / 1000f, p.arg2(), p.duration());
            case RowenaFxPayload.BOWL_BROKEN -> RowenaWorldFx.bowlBroken(p.entity(), p.arg(), p.arg2(), p.point(), p.duration());
            case RowenaFxPayload.RECITE_END -> RowenaWorldFx.reciteEnd(p.entity(), p.arg() == 1, p.arg2(), p.point(), p.duration());
            case RowenaFxPayload.SMOKE -> RowenaWorldFx.smoke(p.point(), p.duration());
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        WitchBossOverlay.tick();
        if (mc.isPaused() || mc.player == null) return;
        for (RowenaEntity r : ClientPresence.live(RowenaEntity.class, level)) {
            if (r.distanceToSqr(mc.player) > 96 * 96) continue;
            if (RowenaWorldFx.floating(r)) RowenaWorldFx.levitate(r);
            if (r.reciting()) RowenaWorldFx.recitation(r.getId());
            if (r.stunned()) RowenaWorldFx.fall(r.getId());
        }
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    /** Forget everything (left the world, or a preview between scenes). The world effects live in the magic's manager. */
    public static void clear() {
        WitchBossOverlay.clear();
        RowenaWorldFx.clear();
    }
}
