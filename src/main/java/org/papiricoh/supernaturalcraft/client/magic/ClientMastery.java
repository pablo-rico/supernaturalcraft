package org.papiricoh.supernaturalcraft.client.magic;

import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MasterySyncPayload;

/** The local player's mastery XP per school, as the server last sent it ({@link MasterySyncPayload}). */
public final class ClientMastery {

    private static MasterySyncPayload last = new MasterySyncPayload(0, 0, 0, 0);

    private ClientMastery() {
    }

    public static void handle(MasterySyncPayload payload) {
        last = payload;
    }

    /** XP in a school (0 for {@link School#NONE}). */
    public static int xp(School school) {
        return last.xp(school);
    }

    /** Forgets everything (leaving a world). */
    public static void reset() {
        last = new MasterySyncPayload(0, 0, 0, 0);
    }
}
