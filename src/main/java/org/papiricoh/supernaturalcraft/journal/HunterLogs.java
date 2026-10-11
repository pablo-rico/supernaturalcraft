package org.papiricoh.supernaturalcraft.journal;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal;
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.HunterLogSyncPayload;
import org.papiricoh.supernaturalcraft.network.LibrarySyncPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.papiricoh.supernaturalcraft.network.Send;

/** The hunter's log on the server: access and the two syncs to its owner. */
public final class HunterLogs {

    private HunterLogs() {
    }

    public static HunterLog get(Player player) {
        return player.getData(AllAttachments.HUNTER_LOG);
    }

    /** The mod's advancements, for the advancement set they were taken from (v0.19: not every server advancement per sync). */
    private static net.minecraft.server.ServerAdvancementManager oursFrom;
    private static List<AdvancementHolder> ours = List.of();

    private static synchronized List<AdvancementHolder> ours(net.minecraft.server.ServerAdvancementManager manager) {
        if (manager != oursFrom) {
            List<AdvancementHolder> l = new ArrayList<>();
            for (AdvancementHolder adv : manager.getAllAdvancements()) if (adv.id().getNamespace().equals(SupernaturalCraft.MODID)) l.add(adv);
            ours = List.copyOf(l);
            oursFrom = manager;
        }
        return ours;
    }

    /** The mod's advancements this player has done, hidden ones included. */
    public static List<ResourceLocation> doneAdvancements(ServerPlayer player) {
        List<ResourceLocation> done = new ArrayList<>();
        if (player.getServer() == null) return done;
        for (AdvancementHolder adv : ours(player.getServer().getAdvancements())) {
            if (player.getAdvancements().getOrStartProgress(adv).isDone()) done.add(adv.id());
        }
        return done;
    }

    public static void sync(ServerPlayer player) {
        HunterLog log = get(player);
        log.dirty = false;
        log.killsDirty = false;
        CrossroadsDeal deal = Debts.get(player);
        Optional<HunterLogSyncPayload.DealSummary> summary = deal.state() == CrossroadsDeal.State.NONE ? Optional.empty()
                : Optional.of(new HunterLogSyncPayload.DealSummary(deal.wish(), deal.arg(), deal.state().name().toLowerCase(java.util.Locale.ROOT),
                deal.dueAt() - Debts.now(player)));
        ArcanaData arcana = ManaManager.get(player);
        Send.toPlayer(player, new HunterLogSyncPayload(doneAdvancements(player), List.copyOf(log.seen()),
                log.kills(), List.copyOf(log.items()), List.copyOf(log.read()), log.bookmarks(), summary,
                arcana.bonusHearts(), arcana.bonusMana()));
    }

    public static void syncLibrary(ServerPlayer player) {
        HunterLog log = get(player);
        log.libraryDirty = false;
        Send.toPlayer(player, new LibrarySyncPayload(log.designs()));
    }
}
