package org.papiricoh.supernaturalcraft.client.chain;

import org.papiricoh.supernaturalcraft.network.ChainSyncPayload;
import org.papiricoh.supernaturalcraft.network.ChainNoticePayload;
import java.util.List;

/** Client presentation may consume these snapshots without keeping server objects. */
public final class ClientChain {
    private ClientChain() {}
    public static List<ChainSyncPayload.Entry> officers = List.of();
    public static List<ChainSyncPayload.History> history = List.of();
    public static ChainNoticePayload lastTitle;
    public static void clear() { officers = List.of(); history = List.of(); lastTitle = null; }
}
