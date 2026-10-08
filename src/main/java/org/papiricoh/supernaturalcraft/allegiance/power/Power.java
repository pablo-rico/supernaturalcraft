package org.papiricoh.supernaturalcraft.allegiance.power;

import org.papiricoh.supernaturalcraft.allegiance.Faction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Every power of every side (pure contract). Actives go on the wheel and are cast with a {@code CastPowerPayload}; passives
 * apply on their own. {@code cost} is Grace/Corruption, {@code cooldown} ticks, {@code range} blocks (0 = self).
 * Name and description: {@code power.supernaturalcraft.<id>} / {@code .desc}; icon {@code textures/gui/allegiance/power/<id>.png}.
 */
public enum Power {
    // --- Angel (Grace) --------------------------------------------------------------------------------------------------
    /** Blink up to {@code range} blocks where you look (a flutter of unseen wings). */
    TELEPORT(Faction.ANGEL, 1, 15, 60, 16, false),
    /** Two fingers to the brow: heals the creature you look at (or yourself) and cures its poisons. */
    HEALING_TOUCH(Faction.ANGEL, 1, 25, 200, 4, false),
    /** An Angel Blade drops from your sleeve for 60 s (bound, it vanishes); casting it again with the blade out dashes. */
    ANGEL_BLADE(Faction.ANGEL, 1, 10, 100, 0, false),
    /** Angel radio: for 20 s, demons and bosses within {@code range} whisper where they are (marks on the HUD). */
    ANGEL_RADIO(Faction.ANGEL, 1, 5, 200, 128, false),
    /** Wings: flight with stamina, as Michael's Grace with the Seraph Wings gives (no wings needed). */
    WINGS(Faction.ANGEL, 2, 0, 0, 0, true),
    /** Smite: a palm to the face of what you look at; holy fire through the eyes (deadly to lesser demons). */
    SMITE(Faction.ANGEL, 2, 40, 300, 6, false),
    /** True form, 10 s: light pours out; everything that sees it is blinded, demons burn. */
    TRUE_FORM(Faction.ANGEL, 3, 80, 1800, 10, false),
    /** A squad of the Host (a captain and four) answers you for 60 s. */
    HOST_SQUAD(Faction.ANGEL, 4, 120, 2400, 0, false),
    /** A lance of light, thrown. */
    LIGHT_LANCE(Faction.ANGEL, 4, 40, 160, 32, false),

    // --- Demon (Corruption) ---------------------------------------------------------------------------------------------
    /** Black smoke: pour out and rush up to {@code range} blocks, untouchable on the way. */
    SMOKE(Faction.DEMON, 1, 15, 80, 12, false),
    /** Fire and lava do not burn you. */
    FIRE_IMMUNITY(Faction.DEMON, 1, 0, 0, 0, true),
    /** A bound hellhound fights at your side for 60 s. */
    SUMMON_HOUND(Faction.DEMON, 1, 30, 600, 0, false),
    /** Black eyes: you see in the dark (and they show when you use your powers). */
    BLACK_EYES(Faction.DEMON, 1, 0, 0, 0, true),
    /** Telekinesis: seize what you look at and hurl it where you look next. */
    TELEKINESIS(Faction.DEMON, 2, 25, 120, 16, false),
    /** Ride a mob for 20 s: it fights for you while you wait in smoke. */
    POSSESS(Faction.DEMON, 2, 50, 900, 8, false),
    /** Yellow eyes: a Prince's eyes (no hunger, no sleep, and mobs flinch). */
    YELLOW_EYES(Faction.DEMON, 2, 0, 0, 0, true),
    /** The First Blade answers its Knight: more damage, and its curse no longer climbs. */
    FIRST_BLADE(Faction.DEMON, 3, 0, 0, 0, true),
    /** Each kill heals you. */
    KILL_REGEN(Faction.DEMON, 3, 0, 0, 0, true),
    /** The Mark's thirst: without a kill, Corruption drains and then your health (a drawback). */
    BLOODLUST(Faction.DEMON, 3, 0, 0, 0, true),
    /** Demons and hellhounds obey the King: they never turn on you and they guard you. */
    DOMINION(Faction.DEMON, 4, 0, 0, 0, true),
    /** The throne: an aura that brings every mob within {@code range} to its knees for 8 s. */
    THRONE(Faction.DEMON, 4, 100, 1800, 12, false),

    // --- Human (hunter's ranks; passives only) ---------------------------------------------------------------------------
    /** Hunter: more mana. */
    HUNTER_MANA(Faction.HUMAN, 1, 0, 0, 0, true),
    /** Veteran: supernatural creatures within {@code range} glow faintly to you. */
    HUNTER_SENSE(Faction.HUMAN, 2, 0, 0, 24, true),
    /** Legend: hunter's weapons +20% against the supernatural; possession slides off you. */
    HUNTER_EDGE(Faction.HUMAN, 3, 0, 0, 0, true);

    public final Faction faction;
    public final int minRank;
    public final int cost;
    public final int cooldown;
    public final int range;
    public final boolean passive;

    Power(Faction faction, int minRank, int cost, int cooldown, int range, boolean passive) {
        this.faction = faction;
        this.minRank = minRank;
        this.cost = cost;
        this.cooldown = cooldown;
        this.range = range;
        this.passive = passive;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String nameKey() {
        return "power.supernaturalcraft." + id();
    }

    public String descKey() {
        return nameKey() + ".desc";
    }

    /** The powers a side has at a rank (passives included), in wheel order. */
    public static List<Power> of(Faction faction, int rank) {
        List<Power> out = new ArrayList<>();
        for (Power p : values()) if (p.faction == faction && p.minRank <= rank) out.add(p);
        return out;
    }

    /** The actives a side has at a rank: what the wheel shows. */
    public static List<Power> wheel(Faction faction, int rank) {
        List<Power> out = new ArrayList<>();
        for (Power p : of(faction, rank)) if (!p.passive) out.add(p);
        return out;
    }

    public static Power byOrdinal(int i) {
        Power[] all = values();
        return i >= 0 && i < all.length ? all[i] : null;
    }
}
