package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.network.AllegianceSyncPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * The allegiance API. {@link #get} works on both sides (the client copy of every player it sees is kept by
 * {@link AllegianceSyncPayload}, sent to the player and to everyone tracking them: wings and eyes show to others).
 * Everything that changes a server player's allegiance goes through {@link #set}, which resyncs.
 */
public final class Allegiances {

    /** Transient display flags (not saved; {@link AllegianceSyncPayload#flags}). */
    public static final int EYES = 1, TRUE_FORM = 2, SUPPRESSED = 4, SMOKE = 8, POSSESSING = 16;
    /**
     * v0.21: the player bears the Mark of Cain ({@code mark.Marks}), and is in its bloodlust (Thirst ≥ ½): others see the Mark on
     * their arm. Kept up to date by {@code Marks} (on change and each second).
     */
    public static final int MARKED = 32, MARK_BLOODLUST = 64;

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
        Allegiance before = get(player);
        player.setData(AllAttachments.ALLEGIANCE, a);
        // What others see (side, rank) goes out to everyone at once; essence alone only to its owner, throttled (v0.19).
        if (before.faction() != a.faction() || before.rank() != a.rank()) sync(player);
        else DIRTY.add(player.getUUID());
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
        Send.trackingEntityAndSelf(player, payload(player));
    }

    /** Only to the player: their essence moved, nothing others see changed. */
    public static void syncSelf(ServerPlayer player) {
        DIRTY.remove(player.getUUID());
        Send.toPlayer(player, payload(player));
    }

    /**
     * Ritual mana for this player, with no school: {@code magic.ArcanaCost} (human, amulet; capped).
     *
     * @deprecated the altar and the bowl price through {@link org.papiricoh.supernaturalcraft.magic.ArcanaCost} with the rite's
     * school (v0.21)
     */
    @Deprecated
    public static float ritualCost(Player player, float cost) {
        return org.papiricoh.supernaturalcraft.magic.ArcanaCost.apply(player, org.papiricoh.supernaturalcraft.magic.ArcanaCost.Tool.RITUAL,
                org.papiricoh.supernaturalcraft.magic.school.School.NONE, cost);
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
