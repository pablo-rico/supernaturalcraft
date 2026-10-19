package org.papiricoh.supernaturalcraft.magic.spell;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/** Posted on the game bus after a spell has been paid for and delivered. */
public class SpellCastEvent extends Event {

    private final ServerPlayer caster;
    private final ResolvedSpell spell;
    private final SpellContext context;
    private final float manaSpent;

    public SpellCastEvent(ServerPlayer caster, ResolvedSpell spell, SpellContext context) {
        this(caster, spell, context, 0f);
    }

    /** @param manaSpent what the cast cost, after every discount (v0.21: trains mastery) */
    public SpellCastEvent(ServerPlayer caster, ResolvedSpell spell, SpellContext context, float manaSpent) {
        this.caster = caster;
        this.spell = spell;
        this.context = context;
        this.manaSpent = manaSpent;
    }

    /** What the cast cost (mana, or a catalyst's equivalent), after every discount. */
    public float getManaSpent() {
        return manaSpent;
    }

    public ServerPlayer getCaster() {
        return caster;
    }

    public ResolvedSpell getSpell() {
        return spell;
    }

    public SpellContext getContext() {
        return context;
    }
}
