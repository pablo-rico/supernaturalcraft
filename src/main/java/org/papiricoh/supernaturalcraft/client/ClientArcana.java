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
    private static Set<ResourceLocation> rites = Set.of();

    private ClientArcana() {
    }

    public static void update(ArcanaSyncPayload p) {
        mana = p.mana();
        maxMana = p.maxMana();
        cooldownUntil = p.cooldownUntil();
        if (p.hasLists()) known = Set.copyOf(p.known());
        grace = p.grace();
        voidMark = p.voidMark();
        sanity = p.sanity();
        if (p.hasLists()) rites = Set.copyOf(p.rites());
    }

    /** The bowl spells this player has learned. */
    public static Set<ResourceLocation> rites() {
        return rites;
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

    /** Whether this player knows the sigil {@code id}, under its old name too (v0.21: {@code bind} is now {@code snare}). */
    public static boolean knowsSigil(ResourceLocation id) {
        return known.contains(id) || known.contains(org.papiricoh.supernaturalcraft.magic.spell.SigilAliases.resolve(id));
    }

    public static Set<ResourceLocation> known() {
        return known;
    }

    public static boolean hasGrace() {
        return grace;
    }
}
