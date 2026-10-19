package org.papiricoh.supernaturalcraft.magic.spell;

import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.Map;

/**
 * Sigils that were renamed (v0.21), old id → new id. Every lookup of a sigil goes through {@link #resolve}
 * ({@link SigilLookup#get}, names, glyphs), and a player's known sigils are rewritten when their arcana loads, so pages, scrolls,
 * grimoire designs and Men of Letters formulas that still name the old id keep working. Only add: an id once renamed stays
 * renamed.
 */
public final class SigilAliases {

    /** Old id → new id. */
    public static final Map<ResourceLocation, ResourceLocation> ALIASES = Map.of(
            // v0.21: the Bind sigil became the hunters' Snare (the devil's trap is the real trapping).
            SupernaturalCraft.asResource("bind"), SupernaturalCraft.asResource("snare"));

    private SigilAliases() {
    }

    /** The current id for {@code id} ({@code id} itself if it was never renamed). */
    public static ResourceLocation resolve(ResourceLocation id) {
        ResourceLocation to = ALIASES.get(id);
        return to == null ? id : to;
    }

    public static boolean isAlias(ResourceLocation id) {
        return ALIASES.containsKey(id);
    }
}
