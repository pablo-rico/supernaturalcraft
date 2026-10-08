package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.network.AllegianceSyncPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The allegiance API. {@link #get} works on both sides (the client copy of every player it sees is kept by
 * {@link AllegianceSyncPayload}, sent to the player and to everyone tracking them: wings and eyes show to others).
 * Everything that changes a server player's allegiance goes through {@link #set}, which resyncs.
 */
public final class Allegiances {

    /** Transient display flags (not saved; {@link AllegianceSyncPayload#flags}). */
    public static final int EYES = 1, TRUE_FORM = 2, SUPPRESSED = 4, SMOKE = 8, POSSESSING = 16;

    private static final Map<UUID, Integer> FLAGS = new ConcurrentHashMap<>();
    /** Essence changes often: those resync at most every few ticks ({@link AllegianceEvents}). */
    static final java.util.Set<UUID> DIRTY = ConcurrentHashMap.newKeySet();

    private Allegiances() {
    }

    public static Allegiance get(Player player) {
        return player.getData(AllAttachments.ALLEGIANCE);
    }

    /** Sets and resyncs at once (a change of side or rank, a cure). */
    public static void set(ServerPlayer player, Allegiance a) {
        player.setData(AllAttachments.ALLEGIANCE, a);
        sync(player);
    }

    /** Sets quietly: the change goes out with the next throttled sync (essence ticking up or down). */
    public static void update(ServerPlayer player, Allegiance a) {
        player.setData(AllAttachments.ALLEGIANCE, a);
        DIRTY.add(player.getUUID());
    }

    public static void addEssence(ServerPlayer player, float delta) {
        if (delta == 0) return;
        Allegiance a = get(player);
        if (!a.committed()) return;
        update(player, a.addEssence(delta));
    }

    public static int flags(Player player) {
        return FLAGS.getOrDefault(player.getUUID(), 0);
    }

    public static boolean flag(Player player, int flag) {
        return (flags(player) & flag) != 0;
    }

    public static void setFlag(ServerPlayer player, int flag, boolean on) {
        int old = flags(player);
        int now = on ? old | flag : old & ~flag;
        if (now == old) return;
        if (now == 0) FLAGS.remove(player.getUUID());
        else FLAGS.put(player.getUUID(), now);
        sync(player);
    }

    static void forget(UUID player) {
        FLAGS.remove(player);
        DIRTY.remove(player);
    }

    public static AllegianceSyncPayload payload(ServerPlayer player) {
        Allegiance a = get(player);
        return new AllegianceSyncPayload(player.getId(), a.faction().ordinal(), a.rank(), a.essence(), flags(player));
    }

    /** To the player and to everyone who can see them. */
    public static void sync(ServerPlayer player) {
        DIRTY.remove(player.getUUID());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload(player));
    }

    /** Ritual mana for this player: a human hunter's rites cost a quarter less. */
    public static float ritualCost(Player player, float cost) {
        return get(player).isHuman() ? (float) (cost * (1 - org.papiricoh.supernaturalcraft.SNConfig.HUMAN_RITUAL_DISCOUNT.getAsDouble())) : cost;
    }

    /** How much longer a human gets to recite at the bowl. */
    public static float recitationScale(Player player) {
        return get(player).isHuman() ? 1.25f : 1f;
    }

    /** An angel of rank II or higher has wings of their own: flight as Michael's Grace and the Seraph Wings give. */
    public static boolean wingsGranted(Player player) {
        Allegiance a = get(player);
        return a.isAngel() && a.rank() >= 2 && !flag(player, SUPPRESSED);
    }
}
