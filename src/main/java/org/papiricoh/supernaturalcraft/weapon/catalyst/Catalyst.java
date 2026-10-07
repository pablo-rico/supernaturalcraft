package org.papiricoh.supernaturalcraft.weapon.catalyst;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;

/**
 * An item that shapes grimoire spells when held in the other hand. Every hook has a neutral
 * default, so a catalyst only overrides what it changes.
 */
public interface Catalyst {

    default float manaMultiplier(ItemStack stack, ResolvedSpell spell) {
        return 1f;
    }

    default float cooldownMultiplier(ItemStack stack, ResolvedSpell spell) {
        return 1f;
    }

    /** True if this catalyst pays for spells itself (so the caster needs no mana). */
    default boolean paysOwnCost(ItemStack stack) {
        return false;
    }

    /** Pays for a spell that would cost {@code mana}; only called if {@link #paysOwnCost}. */
    default void pay(ServerPlayer player, ItemStack stack, float mana) {
    }

    /** Adjusts potency and form traits after the spell's modifiers have run. */
    default void shape(SpellContext ctx, ItemStack stack, ResolvedSpell spell) {
    }

    default void afterCast(ServerPlayer player, ItemStack stack, ResolvedSpell spell, SpellContext ctx) {
    }

    static boolean hasEffect(ResolvedSpell spell, String path) {
        return spell.effects().stream().anyMatch(p -> p.id().getPath().equals(path));
    }
}
