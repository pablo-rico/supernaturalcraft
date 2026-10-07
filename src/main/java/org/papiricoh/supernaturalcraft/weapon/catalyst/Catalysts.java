package org.papiricoh.supernaturalcraft.weapon.catalyst;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.weapon.Rune;
import org.papiricoh.supernaturalcraft.weapon.RuneSet;

/**
 * The catalyst in play for one cast: the item held opposite the grimoire, plus whatever
 * catalyst runes are graved into it. {@link #NONE} when there is none, so callers never branch.
 */
public final class Catalysts {

    public static final float RESONANCE_MANA = 0.10f, RESONANCE_POTENCY = 0.10f, FOCUS_COOLDOWN = 0.15f, ECHO_CHANCE = 0.15f;

    public record Held(ItemStack stack, Catalyst catalyst, RuneSet runes) {

        public float manaMultiplier(ResolvedSpell spell) {
            return catalyst.manaMultiplier(stack, spell) * (1f - RESONANCE_MANA * runes.count(Rune.RESONANCE));
        }

        public float cooldownMultiplier(ResolvedSpell spell) {
            return catalyst.cooldownMultiplier(stack, spell) * (1f - FOCUS_COOLDOWN * runes.count(Rune.FOCUS));
        }

        public boolean paysOwnCost() {
            return catalyst.paysOwnCost(stack);
        }

        public void pay(ServerPlayer player, float mana) {
            catalyst.pay(player, stack, mana);
        }

        public void shape(SpellContext ctx, ResolvedSpell spell) {
            catalyst.shape(ctx, stack, spell);
            ctx.potency *= 1f + RESONANCE_POTENCY * runes.count(Rune.RESONANCE);
            for (int i = 0; i < runes.count(Rune.ECHO); i++) {
                if (ctx.level.random.nextFloat() < ECHO_CHANCE) ctx.echoes++;
            }
        }

        public void afterCast(ServerPlayer player, ResolvedSpell spell, SpellContext ctx) {
            catalyst.afterCast(player, stack, spell, ctx);
        }
    }

    public static final Held NONE = new Held(ItemStack.EMPTY, new Catalyst() {
    }, RuneSet.EMPTY);

    private Catalysts() {
    }

    /** The catalyst in the hand opposite a grimoire (or the off hand when casting from a scroll). */
    public static Held held(Player player) {
        InteractionHand grimoire = GrimoireItem.heldHand(player);
        InteractionHand other = grimoire == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND
                : grimoire == InteractionHand.OFF_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(other);
        if (stack.getItem() instanceof Catalyst c) {
            return new Held(stack, c, stack.getOrDefault(AllDataComponents.RUNES, RuneSet.EMPTY));
        }
        return NONE;
    }
}
