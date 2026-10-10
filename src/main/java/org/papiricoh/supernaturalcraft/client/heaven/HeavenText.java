package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.memory.Memory;
import org.papiricoh.supernaturalcraft.memory.MemorySets;
import org.papiricoh.supernaturalcraft.memory.MemoryText;

import java.util.Locale;

/**
 * Heaven's words on the client (v0.18): an attack's name on the docket, a memory's title and line for toasts and the book.
 * Each asks the lang first and falls back to a readable form of its id, so text the server's work has not named yet still reads.
 */
public final class HeavenText {

    private HeavenText() {
    }

    /** {@code paper_storm} → "Paper Storm". */
    public static String pretty(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        if (path.contains("/")) path = path.substring(path.lastIndexOf('/') + 1);
        StringBuilder out = new StringBuilder();
        for (String word : path.split("[_\\s]+")) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    /** A docket entry: one of Zachariah's attacks. */
    public static Component attack(String id) {
        return Component.translatableWithFallback("docket.supernaturalcraft.zachariah." + id.toLowerCase(Locale.ROOT), pretty(id));
    }

    /** An entity type id's name ({@code minecraft:wolf} → "Wolf"), or the id made readable. */
    public static Component creature(String typeId) {
        ResourceLocation id = ResourceLocation.tryParse(typeId);
        if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return BuiltInRegistries.ENTITY_TYPE.get(id).getDescription();
        return Component.literal(pretty(typeId));
    }

    /** What a memory's {@link Memory#subject} names, by its kind (a boss, a creature, a faction, a wish). */
    public static Component subject(Memory m) {
        return switch (m.kind()) {
            case BOSS_VICTORY -> Component.translatableWithFallback("entity.supernaturalcraft." + m.subject(), pretty(m.subject()));
            case CASE_SOLVED, CASE_LOST, PET_LOST, FIRST_SIGHTING, FAVOURITE_PREY -> creature(m.subject());
            case ASCENSION -> Component.translatableWithFallback("hud.supernaturalcraft.heaven.faction." + m.subject(), pretty(m.subject()));
            default -> Component.literal(pretty(m.subject()));
        };
    }

    /** A memory's title (the memory work's lang: per great enemy, per side, per kind). */
    public static Component title(Memory m) {
        return Component.translatableWithFallback(MemoryText.titleKey(m), pretty(m.kind().name().toLowerCase(Locale.ROOT)));
    }

    /** A memory's line in the book: {@code %1$s} who or what (a pet's name if it had one), {@code %2$s} the variant. */
    public static Component line(Memory m) {
        Component who = m.kind() == org.papiricoh.supernaturalcraft.memory.MemoryKind.PET_LOST && !m.detail().isEmpty()
                ? Component.literal(m.detail()) : subject(m);
        return Component.translatableWithFallback(MemoryText.lineKey(m), "", who, m.variant());
    }

    /** A collection's name. */
    public static Component set(MemorySets.Set set) {
        return Component.translatableWithFallback(set.key(), pretty(set.id));
    }

    /** A collection's name by its {@link org.papiricoh.supernaturalcraft.memory.MemoryKind#set} id. */
    public static Component set(String id) {
        for (MemorySets.Set s : MemorySets.Set.values()) if (s.id.equals(id)) return set(s);
        return Component.literal(pretty(id));
    }
}
