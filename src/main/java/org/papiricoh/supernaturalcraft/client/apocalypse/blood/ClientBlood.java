package org.papiricoh.supernaturalcraft.client.apocalypse.blood;

import org.papiricoh.supernaturalcraft.network.BloodSyncPayload;

/** This client's own demon blood (v0.40): the contract the wheel, the HUD and the hallucinations read. */
public final class ClientBlood {

    private static BloodSyncPayload state = BloodSyncPayload.NONE;

    private ClientBlood() {
    }

    public static void handle(BloodSyncPayload payload) {
        state = payload;
    }

    public static BloodSyncPayload state() {
        return state;
    }

    /** Blood in the body, 0-1. */
    public static float blood() {
        return state.blood();
    }

    public static float dependency() {
        return state.dependency();
    }

    /** The blood powers' tier (0 = none: no blood powers on the wheel). */
    public static int tier() {
        return state.blood() > 0 ? state.tier() : 0;
    }

    public static boolean withdrawal() {
        return state.withdrawal();
    }

    public static void reset() {
        state = BloodSyncPayload.NONE;
    }
}
