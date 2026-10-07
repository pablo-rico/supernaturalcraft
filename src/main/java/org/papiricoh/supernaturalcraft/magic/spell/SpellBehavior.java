package org.papiricoh.supernaturalcraft.magic.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

/** Java side of a sigil. Each kind has its own sub-interface. */
public sealed interface SpellBehavior permits SpellBehavior.Form, SpellBehavior.Effect, SpellBehavior.Modifier {

    SigilKind kind();

    non-sealed interface Form extends SpellBehavior {
        /** Default range/area before modifiers. */
        void setup(SpellContext ctx, SigilComponent sigil);

        /** Carries the spell to its targets and calls {@link ResolvedSpell#applyTo}. */
        void deliver(SpellContext ctx, ResolvedSpell spell);

        @Override
        default SigilKind kind() {
            return SigilKind.FORM;
        }
    }

    non-sealed interface Effect extends SpellBehavior {
        /** Helps (applied to the caster and allies) or harms (applied to everyone else). */
        boolean beneficial();

        boolean applyToEntity(SpellContext ctx, SigilComponent sigil, Entity target);

        default boolean applyToBlock(SpellContext ctx, SigilComponent sigil, BlockPos pos, Direction face) {
            return false;
        }

        @Override
        default SigilKind kind() {
            return SigilKind.EFFECT;
        }
    }

    non-sealed interface Modifier extends SpellBehavior {
        void modify(SpellContext ctx, SigilComponent sigil);

        @Override
        default SigilKind kind() {
            return SigilKind.MODIFIER;
        }
    }
}
