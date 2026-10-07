package org.papiricoh.supernaturalcraft.client;

import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.network.ArcanaSyncPayload;

import java.util.Set;

/** The local player's magic, as last told by the server. */
public final class ClientArcana {

    private static float mana = 100;
    private static float maxMana = 100;
    private static long cooldownUntil;
    private static Set<ResourceLocation> known = Set.of();
    private static boolean grace, voidMark;
    private static float sanity = 100;

    private ClientArcana() {
    }

    public static void update(ArcanaSyncPayload p) {
        mana = p.mana();
        maxMana = p.maxMana();
        cooldownUntil = p.cooldownUntil();
        known = Set.copyOf(p.known());
        grace = p.grace();
        voidMark = p.voidMark();
        sanity = p.sanity();
    }

    /** Whether this player drank the Eclipse Sight: the dark no longer blinds them. */
    public static boolean voidMark() {
        return voidMark;
    }

    public static float sanity() {
        return sanity;
    }

    public static float mana() {
        return mana;
    }

    public static float maxMana() {
        return maxMana;
    }

    public static long cooldownUntil() {
        return cooldownUntil;
    }

    public static Set<ResourceLocation> known() {
        return known;
    }

    public static boolean hasGrace() {
        return grace;
    }
}
