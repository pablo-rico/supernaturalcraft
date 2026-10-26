package org.papiricoh.supernaturalcraft.client.chain;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload;
import org.papiricoh.supernaturalcraft.network.ChainNoticePayload;
import java.util.List;

/** Client presentation may consume these snapshots without keeping server objects. */
public final class ClientChain {
    private ClientChain() {}
    public static List<ChainSyncPayload.Entry> officers = List.of();
    public static List<ChainSyncPayload.History> history = List.of();
    public static List<ChainSyncPayload.Court> courts = List.of();
    public static ChainNoticePayload lastTitle;
    public static void clear() { officers = List.of(); history = List.of(); courts = List.of(); lastTitle = null; }

    /** The officer a materialized mob carries ({@code officerId} synced on the entity), or null for an ordinary mob. */
    public static @Nullable ChainSyncPayload.Entry entryFor(Entity e) {
        int id = e instanceof BlackEyedDemon d ? d.officerId() : e instanceof AngelEntity a ? a.officerId() : -1;
        if (id < 0) return null;
        String faction = e instanceof AngelEntity ? "angel" : "demon";
        for (var o : officers) if (o.id() == id && o.faction().equals(faction)) return o;
        return null;
    }

    /** {@code "demon"} or {@code "angel"}: its stronghold and interregnum, or null before the first sync. */
    public static @Nullable ChainSyncPayload.Court court(String faction) {
        for (var c : courts) if (c.faction().equals(faction)) return c;
        return null;
    }
}
