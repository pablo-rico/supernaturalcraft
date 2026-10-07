package org.papiricoh.supernaturalcraft.magic.spell;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/** Posted on the game bus after a spell has been paid for and delivered. */
public class SpellCastEvent extends Event {

    private final ServerPlayer caster;
    private final ResolvedSpell spell;
    private final SpellContext context;

    public SpellCastEvent(ServerPlayer caster, ResolvedSpell spell, SpellContext context) {
        this.caster = caster;
        this.spell = spell;
        this.context = context;
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
