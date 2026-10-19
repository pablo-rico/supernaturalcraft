package org.papiricoh.supernaturalcraft.client.mark;

import org.papiricoh.supernaturalcraft.network.MarkSyncPayload;

/** Whether the local player bears the Mark of Cain and how thirsty it is, as the server last sent it ({@link MarkSyncPayload}). */
public final class ClientMark {

    private static boolean marked;
    private static float thirst;

    private ClientMark() {
    }

    public static void handle(MarkSyncPayload payload) {
        marked = payload.marked();
        thirst = Math.max(0f, Math.min(1f, payload.thirst()));
    }

    public static boolean marked() {
        return marked;
    }

    /** 0-1; 0 when not marked. */
    public static float thirst() {
        return marked ? thirst : 0f;
    }

    /** Forgets everything (leaving a world). */
    public static void reset() {
        marked = false;
        thirst = 0f;
    }
}
