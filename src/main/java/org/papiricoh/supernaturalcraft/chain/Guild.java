package org.papiricoh.supernaturalcraft.chain;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The profile of a guild a hunter founded (pure, mutable; v0.29), kept by {@link ChainSavedData} for its slot. Its officers
 * live in the slot's {@link Hierarchy} like any chain's (the founder is the Lord's post); this is what a chain of the world
 * would take from its code: name, colours, standard, invitations and the founder's last sight.
 * <ul>
 *     <li>{@code generation}: bumped each time the slot is founded anew, so nothing left over from an older guild is taken
 *     for this one.</li>
 *     <li>{@code dye}: the vanilla dye of the founding banner (0–15); {@code emblem}: 0 to {@link GuildRules#EMBLEMS} − 1.</li>
 *     <li>{@code standard*}: where its standard stands; {@code standardLost} the day it was taken down (-1 while it stands,
 *     or before it was ever planted).</li>
 *     <li>{@code founderSeen}: the last day the Lord was online (an interregnum starts {@link GuildRules#INACTIVE_DAYS} after).</li>
 *     <li>{@code invites}: invited hunters → the last day the invitation stands.</li>
 *     <li>{@code openSince}: per rank, the day a post for an officer of flesh opened ({@link GuildRules#ripe}).</li>
 * </ul>
 */
public final class Guild {
    public final ChainFaction slot;
    public int generation;
    public String name = "";
    public int dye, emblem;
    public UUID founder;
    public String founderName = "";
    public long founded, founderSeen;
    public boolean hasStandard;
    public String standardDimension = "";
    public int standardX, standardY, standardZ;
    public long standardLost = -1;
    public final Map<UUID, Long> invites = new HashMap<>();
    public final Map<ChainRank, Long> openSince = new EnumMap<>(ChainRank.class);

    public Guild(ChainFaction slot) {
        if (!slot.guild()) throw new IllegalArgumentException(slot + " is no guild slot");
        this.slot = slot;
    }

    /** Whether the slot holds a guild (a free slot has no founder). */
    public boolean founded() {
        return founder != null;
    }

    public void plant(String dimension, int x, int y, int z) {
        hasStandard = true;
        standardDimension = dimension;
        standardX = x;
        standardY = y;
        standardZ = z;
        standardLost = -1;
    }

    /** Its standard is taken down on {@code day}: it is homeless until it stands again. */
    public void uproot(long day) {
        hasStandard = false;
        standardLost = day;
    }

    /** Whether it has been without its standard for so long it loses its ground. */
    public boolean landless(long day) {
        return !hasStandard && standardLost >= 0 && day - standardLost >= GuildRules.HOMELESS_DAYS;
    }

    public boolean invited(UUID player, long day) {
        Long until = invites.get(player);
        return until != null && day <= until;
    }

    /** Frees the slot (the next founding bumps the generation). */
    public void clear() {
        name = "";
        dye = 0;
        emblem = 0;
        founder = null;
        founderName = "";
        founded = 0;
        founderSeen = 0;
        hasStandard = false;
        standardDimension = "";
        standardX = standardY = standardZ = 0;
        standardLost = -1;
        invites.clear();
        openSince.clear();
    }
}
