package org.papiricoh.supernaturalcraft.chain.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.Officer;
import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;

/**
 * Where the chains hold ground and how their war moves it. Without Nations &amp; War this is the ring of sectors around
 * the Pit ({@link SectorRealm}, the {@code Front}); with it, the compat bridge installs a realm of dominions (each power's
 * seat and enclaves as Nations &amp; War towns) through {@link ChainRealms#install}. The chain code asks the realm and never
 * the front directly, so both behave the same from its side.
 */
public interface Realm {
    /** True for the ring of sectors (the client draws the dial); false for dominions. */
    boolean sectors();

    /** Where {@code o} (below court rank) roams now; {@code seat}: its faction's seat. */
    SeatSite.Site roam(MinecraftServer server, Officer o, SeatSite.Site seat);

    /** A faction woke (or stands without ground): it takes its share. @return true if it took any */
    boolean admit(MinecraftServer server, ChainFaction f);

    /** A faction is beheaded, landless or gone: its ground goes. @return how many pieces it lost */
    int release(MinecraftServer server, ChainFaction f);

    /** {@code winner} bested {@code loser} in a clash across the sides: {@code side} presses where the loser roamed. */
    void clash(MinecraftServer server, Officer winner, Officer loser, ChainFaction side, int points, long day);

    /** Whether {@code a} and {@code b} may be the two sides of the chain's own war (the dice and a hunter Lord's call). */
    default boolean warPair(ChainFaction a, ChainFaction b) {
        return true;
    }

    /** The chain's war began ({@code ChainSavedData.warSides}, {@code warUntil}). */
    default void warDeclared(MinecraftServer server, long day) {
    }

    /** {@code side}'s war score rose by {@code points}. */
    default void scored(MinecraftServer server, ChainFaction side, int points) {
    }

    /**
     * The chain's war is over, {@code winner} over {@code loser} (both null for a draw): the ground it wins. @return the
     * history detail of what changed hands ("sector:5", "enclave:Name") or ""
     */
    String warSettled(MinecraftServer server, @Nullable ChainFaction winner, @Nullable ChainFaction loser, long day);

    /** Why a guild's standard may not stand at {@code at} besides the chain's own spacing (a notice key), or null. */
    default @Nullable String standardRefusal(ServerPlayer p, BlockPos at, @Nullable ChainFaction slot) {
        return null;
    }

    /** Whether the ring's "too central" rule applies to standards (only around the sectors' ring). */
    default boolean ringRules() {
        return sectors();
    }

    /** Whether the stance between {@code a} and {@code b} is set elsewhere (two guild-nations: their alliance and wars). */
    default boolean stanceLocked(MinecraftServer server, ChainFaction a, ChainFaction b) {
        return false;
    }

    /** A guild was founded by {@code founder} (with Nations &amp; War: their nation is consecrated as it). */
    default void guildFounded(MinecraftServer server, ChainFaction slot, ServerPlayer founder) {
    }

    /**
     * {@code p} planted {@code slot}'s standard again at {@code at} (with Nations &amp; War: the Lord of a guild founded before
     * it, planting in the capital of the nation they lead, consecrates it).
     */
    default void standardPlanted(MinecraftServer server, ChainFaction slot, ServerPlayer p, BlockPos at) {
    }

    /** A guild is about to dissolve (its slot is still filled). */
    default void guildDissolving(MinecraftServer server, ChainFaction slot) {
    }

    /** Whether {@code p} may come into {@code slot} without an invitation (a citizen of the guild's nation swearing the oath). */
    default boolean oathOpen(ServerPlayer p, ChainFaction slot) {
        return false;
    }

    /** Why {@code slot} may not invite {@code target} (a notice key), or null. */
    default @Nullable String inviteRefusal(MinecraftServer server, ChainFaction slot, ServerPlayer target) {
        return null;
    }
}
