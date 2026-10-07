package org.papiricoh.supernaturalcraft.entity.boss.horsemen;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.Locale;

/**
 * The four Horsemen: what each one is called, how many phases his fight has, his arena and colour, the advancement for
 * beating him, and (resolved only when asked, so tests can use the enum without the registries) his ring, trophy and type.
 * The ordinal is the variant of the horse he leaves behind.
 */
public enum HorsemanKind {
    WAR(3, ArenaTheme.WAR, 0xB3121A, "main/war"),
    FAMINE(3, ArenaTheme.FAMINE, 0x6B5A3A, "main/famine"),
    PESTILENCE(3, ArenaTheme.PLAGUE, 0x9DB86A, "main/pestilence"),
    DEATH(4, ArenaTheme.DEATH, 0xD8D8D0, "main/pale_rider");

    /** Phases of the fight; he mounts his horse for the last. */
    public final int phases;
    public final int theme;
    public final int color;
    /** Path of the advancement for beating him. */
    public final String advancement;

    HorsemanKind(int phases, int theme, int color, String advancement) {
        this.phases = phases;
        this.theme = theme;
        this.color = color;
        this.advancement = advancement;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The horse's texture: {@code textures/entity/horseman_steed_<id>.png}. */
    public String steedTexture() {
        return "horseman_steed_" + id();
    }

    public static HorsemanKind byVariant(int variant) {
        HorsemanKind[] all = values();
        return all[Math.floorMod(variant, all.length)];
    }

    public Item ring() {
        return switch (this) {
            case WAR -> AllItems.RING_OF_WAR.get();
            case FAMINE -> AllItems.RING_OF_FAMINE.get();
            case PESTILENCE -> AllItems.RING_OF_PESTILENCE.get();
            case DEATH -> AllItems.RING_OF_DEATH.get();
        };
    }

    public Item trophy() {
        return switch (this) {
            case WAR -> AllItems.WAR_TROPHY.get();
            case FAMINE -> AllItems.FAMINE_TROPHY.get();
            case PESTILENCE -> AllItems.PESTILENCE_TROPHY.get();
            case DEATH -> AllItems.DEATH_TROPHY.get();
        };
    }

    public EntityType<? extends HorsemanEntity> type() {
        return switch (this) {
            case WAR -> AllEntities.WAR.get();
            case FAMINE -> AllEntities.FAMINE.get();
            case PESTILENCE -> AllEntities.PESTILENCE.get();
            case DEATH -> AllEntities.DEATH.get();
        };
    }
}
