package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.Kin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Which guild each hunter serves (v0.29), read from the guild chains' posts and cached until the chain data changes: the
 * targeting and reaction hooks ask it many times a second. The posts stay the truth ({@link Hierarchy#heldBy}).
 */
public final class GuildMembers {
    private static final Map<UUID, ChainFaction> INDEX = new HashMap<>();
    private static long revision = -1;
    private static ChainSavedData indexed;

    private GuildMembers() {
    }

    /** The guild {@code player} holds a post in, or null. */
    public static @Nullable ChainFaction of(MinecraftServer server, UUID player) {
        ChainSavedData data = ChainSavedData.get(server);
        if (data != indexed || data.revision() != revision) {
            INDEX.clear();
            for (Guild g : data.guilds())
                for (Officer o : data.hierarchy(g.slot).alive()) if (o.player != null) INDEX.put(o.player, g.slot);
            indexed = data;
            revision = data.revision();
        }
        return INDEX.get(player);
    }

    /**
     * The faction a hunter answers for when a chain weighs them: their guild, else the chain of their sworn side (a fallen
     * one counts with the Pit), else null for a free hunter.
     */
    public static @Nullable ChainFaction faction(Player p) {
        if (p.getServer() != null) {
            ChainFaction guild = of(p.getServer(), p.getUUID());
            if (guild != null) return guild;
        }
        if (Kin.isDemon(p) || Kin.isFallen(p)) return ChainFaction.DEMON;
        return Kin.isAngel(p) ? ChainFaction.ANGEL : null;
    }

    /**
     * How a wild demon or angel meets a guild's member (v0.29): by the guild's stance towards the chain of the mob's kind (a
     * demon or a fallen angel counts with the Pit, an angel with the Host). True: left alone (neutral or allied); false: fair
     * game (hostile); null: not a guild's member, or not a wild one of those kinds (the usual rules apply).
     */
    public static @Nullable Boolean wildPeace(net.minecraft.world.entity.LivingEntity mob, Player p) {
        if (p.getServer() == null || OfficerSpawner.key(mob) != null) return null;
        ChainFaction guild = of(p.getServer(), p.getUUID());
        if (guild == null) return null;
        ChainFaction kind = Kin.isDemon(mob) || Kin.isFallen(mob) ? ChainFaction.DEMON : Kin.isAngel(mob) ? ChainFaction.ANGEL : null;
        if (kind == null) return null;
        return !ChainSavedData.get(p.getServer()).hostile(guild, kind);
    }

    public static void clear() {
        INDEX.clear();
        indexed = null;
        revision = -1;
    }
}
