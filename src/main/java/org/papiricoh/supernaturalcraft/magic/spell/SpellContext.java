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
    public float durationScale = 1.0f;
    public float range = 3.0f;
    public float area = 0.0f;
    public int echoes = 0;
    public int color = 0xF2E6B0;
    /** Set by a catalyst: how the form behaves this cast. */
    public org.papiricoh.supernaturalcraft.weapon.catalyst.CatalystTraits traits =
            org.papiricoh.supernaturalcraft.weapon.catalyst.CatalystTraits.NONE;

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
        c.durationScale = durationScale;
        c.range = range;
        c.area = area;
        c.echoes = echoes;
        c.color = color;
        c.traits = traits;
        return c;
    }
}
