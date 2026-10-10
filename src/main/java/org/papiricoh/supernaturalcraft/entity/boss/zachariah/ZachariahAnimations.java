package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import org.papiricoh.supernaturalcraft.heaven.HeavenAssets;

import java.util.List;

/**
 * Zachariah's clip names (v0.18, pure): {@code animation.zachariah.<clip>} from {@link HeavenAssets#ZACHARIAH_CLIPS}. The loops
 * play on the base controller; the rest are triggered once on {@code action}. Every name the code triggers is a constant here, and
 * {@code ZachariahBalanceTest} checks each one is in the art's list.
 */
public final class ZachariahAnimations {

    private ZachariahAnimations() {
    }

    public static final String PREFIX = "animation.zachariah.";

    public static final String IDLE = "idle", WALK = "walk", PAPER_STORM = "paper_storm", STAMP = "stamp", SUMMON_CLERKS = "summon_clerks",
            PRECEDENT = "precedent", SHUFFLE = "shuffle", TERMINATION = "termination", REASSIGN = "reassign", WING_BUFFET = "wing_buffet",
            SMITE = "smite", WINGS_REVEAL = "wings_reveal", STAGGER = "stagger", DEATH = "death", EMERGE = "emerge";

    /** Every clip the code plays. */
    public static final List<String> USED = List.of(IDLE, WALK, PAPER_STORM, STAMP, SUMMON_CLERKS, PRECEDENT, SHUFFLE, TERMINATION, REASSIGN,
            WING_BUFFET, SMITE, WINGS_REVEAL, STAGGER, DEATH, EMERGE);

    /** Triggered on the {@code action} controller (everything that does not loop). */
    public static final List<String> TRIGGERED = HeavenAssets.ZACHARIAH_CLIPS.stream()
            .filter(c -> !HeavenAssets.ZACHARIAH_LOOPS.contains(c)).toList();

    /** Clips that hold their last frame. */
    public static final List<String> HOLDS = List.of(EMERGE, DEATH, WINGS_REVEAL);
}
