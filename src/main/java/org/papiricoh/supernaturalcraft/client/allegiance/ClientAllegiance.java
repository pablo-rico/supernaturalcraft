package org.papiricoh.supernaturalcraft.client.allegiance;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.network.AllegianceDialoguePayload;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.network.AllegianceSyncPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

import java.util.HashMap;
import java.util.Map;

/** Client side of the allegiance payloads: every seen player's side and display flags. */
public final class ClientAllegiance {

    /** Display flags by entity id ({@code Allegiances.EYES} …). */
    private static final Map<Integer, Integer> FLAGS = new HashMap<>();

    private ClientAllegiance() {
    }

    public static int flags(Entity e) {
        return FLAGS.getOrDefault(e.getId(), 0);
    }

    public static boolean flag(Entity e, int flag) {
        return (flags(e) & flag) != 0;
    }

    public static void handleSync(AllegianceSyncPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!(mc.level.getEntity(p.entity()) instanceof Player player)) return;
        Allegiance old = player.getData(AllAttachments.ALLEGIANCE);
        Allegiance now = new Allegiance(Faction.byOrdinal(p.faction()), p.rank(), p.essence(), old.cooldownUntil(), old.tollHearts(),
                old.cureStage(), old.lastCureNight(), old.messenger(), old.messengerDay(), old.pendingDemon());
        player.setData(AllAttachments.ALLEGIANCE, now);
        if (p.flags() == 0) FLAGS.remove(p.entity());
        else FLAGS.put(p.entity(), p.flags());
    }

    /** The local player's powers are nullified (Chuck's fight). */
    public static boolean suppressed() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && flag(mc.player, org.papiricoh.supernaturalcraft.allegiance.Allegiances.SUPPRESSED);
    }

    /** As {@code Allegiances.wingsGranted}, with the flags this client was told. */
    public static boolean wingsGranted(Player player) {
        Allegiance a = player.getData(AllAttachments.ALLEGIANCE);
        return a.isAngel() && a.rank() >= 2 && !flag(player, org.papiricoh.supernaturalcraft.allegiance.Allegiances.SUPPRESSED);
    }

    /** Preview hook: as if the server had sent these flags for {@code e}. */
    public static void forceFlags(Entity e, int flags) {
        if (flags == 0) FLAGS.remove(e.getId());
        else FLAGS.put(e.getId(), flags);
    }

    static void clear() {
        FLAGS.clear();
    }

    public static void handleDialogue(AllegianceDialoguePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (p.options().isEmpty() && p.node().isEmpty()) {
            // An empty page: the talk is over.
            if (mc.screen instanceof AllegianceDialogueScreen) mc.setScreen(null);
            return;
        }
        if (mc.screen instanceof AllegianceDialogueScreen open && open.page().dialogue().equals(p.dialogue())) open.next(p);
        else mc.setScreen(new AllegianceDialogueScreen(p));
    }

    public static void handleFx(AllegianceFxPayload p) {
        AllegianceFx.handle(p);
    }
}
