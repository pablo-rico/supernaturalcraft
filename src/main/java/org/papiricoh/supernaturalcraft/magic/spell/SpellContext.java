package org.papiricoh.supernaturalcraft.magic.spell;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * The mutable numbers of one cast. Forms set their defaults, modifiers scale them, effects
 * read them. Echoed casts get a fresh copy.
 */
public class SpellContext {

    public final ServerLevel level;
    public final LivingEntity caster;
    public float potency = 1.0f;
    public float bossDamageScale = 1.0f;
    public float durationScale = 1.0f;
    public float range = 3.0f;
    public float area = 0.0f;
    public int echoes = 0;
    public int color = 0xF2E6B0;
    /** Set by a catalyst: how the form behaves this cast. */
    public org.papiricoh.supernaturalcraft.weapon.catalyst.CatalystTraits traits =
            org.papiricoh.supernaturalcraft.weapon.catalyst.CatalystTraits.NONE;
    /**
     * How many more IMPACT effects this cast may still send (v0.21, {@code magic.fx.MagicFx#impact}): one per thing hit, up to
     * {@link #MAX_IMPACTS}, so a burst into a crowd stays within the network budget. A ward's pulses set it to 0 (the ward
     * shows itself).
     */
    public int impactBudget = MAX_IMPACTS;

    public static final int MAX_IMPACTS = 6;
    /**
     * The Linger modifier (v0.21, param {@code linger_ticks}): where the spell first takes hold it leaves a zone that repeats it
     * for this long ({@code ResolvedSpell#linger}). One zone per cast ({@link #lingered}); a zone's own pulses never linger.
     */
    public int lingerTicks = 0;
    public boolean lingered = false;

    public SpellContext(ServerLevel level, LivingEntity caster) {
        this.level = level;
        this.caster = caster;
    }

    public int duration(int baseTicks) {
        return Math.round(baseTicks * durationScale);
    }

    public SpellContext copy() {
        SpellContext c = new SpellContext(level, caster);
        c.potency = potency;
        c.bossDamageScale = bossDamageScale;
        c.durationScale = durationScale;
        c.range = range;
        c.area = area;
        c.echoes = echoes;
        c.color = color;
        c.traits = traits;
        c.impactBudget = MAX_IMPACTS;
        c.lingerTicks = lingerTicks;
        return c;
    }
}
