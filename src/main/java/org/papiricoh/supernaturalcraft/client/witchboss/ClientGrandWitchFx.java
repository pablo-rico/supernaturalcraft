package org.papiricoh.supernaturalcraft.client.witchboss;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchEntity;
import org.papiricoh.supernaturalcraft.network.GrandWitchFxPayload;
import org.papiricoh.supernaturalcraft.util.ClientPresence;

/**
 * The Grand Witch's moments on the client (v0.21, P6): each {@link GrandWitchFxPayload} kind played out — the title cards
 * ({@link WitchBossOverlay}) and the fight in the world ({@link GrandWitchWorldFx}: the coven's beams and her shield, the frost
 * hex, the inferno, the crush, the mass taglock with Rowena's green circles and the curse landing, the smoke veil, Rowena arriving
 * and leaving, the Coven Staff's hex).
 *
 * <p>Her coven's channel and her shield are drawn from the synced flags ({@code CovenWitchEntity.channeling()},
 * {@link GrandWitchEntity#shielded()}), so a late joiner sees them; the payloads add their moments.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientGrandWitchFx {

    private ClientGrandWitchFx() {
    }

    public static void handle(GrandWitchFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        switch (p.kind()) {
            case GrandWitchFxPayload.TITLE -> WitchBossOverlay.title(WitchBossOverlay.Who.GRAND_WITCH, p.arg(), p.arg2() == 1, p.duration());
            case GrandWitchFxPayload.CHANNEL -> GrandWitchWorldFx.channel(p.entity(), p.arg(), p.arg2() == 1, p.point());
            case GrandWitchFxPayload.SHIELD -> GrandWitchWorldFx.shield(p.entity(), p.arg() == 1, p.point());
            case GrandWitchFxPayload.FROST_HEX -> GrandWitchWorldFx.frost(p.point(), p.arg() / 10f, p.duration());
            case GrandWitchFxPayload.INFERNO -> GrandWitchWorldFx.inferno(p.entity(), p.point(), p.arg() / 10f, p.duration());
            case GrandWitchFxPayload.CRUSH -> GrandWitchWorldFx.crush(p.entity(), p.arg(), p.point(), p.duration());
            case GrandWitchFxPayload.MASS_TAGLOCK -> GrandWitchWorldFx.taglock(p.entity(), p.arg(), p.arg2() / 10f, p.duration());
            case GrandWitchFxPayload.SAFE_CIRCLE -> GrandWitchWorldFx.safe(p.entity(), p.point(), p.arg() / 10f, p.duration());
            case GrandWitchFxPayload.MASS_CURSE -> GrandWitchWorldFx.massCurse(p.arg(), p.arg2() == 1, p.point());
            case GrandWitchFxPayload.VEIL -> GrandWitchWorldFx.veil(p.point(), p.duration());
            case GrandWitchFxPayload.ALLY -> GrandWitchWorldFx.ally(p.entity(), p.arg() == 1, p.point(), p.duration());
            case GrandWitchFxPayload.STAFF -> GrandWitchWorldFx.staff(p.entity(), p.arg(), p.point(), p.duration());
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || mc.player == null) return;
        for (GrandWitchEntity w : ClientPresence.live(GrandWitchEntity.class, level)) {
            if (w.distanceToSqr(mc.player) > 96 * 96) continue;
            // Her coven's channel and her shield (the effect ends by itself when she is gone).
            if (w.shielded() || w.phase() == 1) GrandWitchWorldFx.coven(w);
        }
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    /** Forget everything (left the world, or a preview between scenes). */
    public static void clear() {
        WitchBossOverlay.clear();
        GrandWitchWorldFx.clear();
    }
}
