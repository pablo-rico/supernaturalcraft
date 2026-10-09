package org.papiricoh.supernaturalcraft.magic.spell;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.papiricoh.supernaturalcraft.hunter.AmuletHelper;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.util.ServerScheduler;
import org.papiricoh.supernaturalcraft.weapon.catalyst.Catalysts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The cast pipeline, all on the server: resolve → cost → check cooldown, mana and reagents →
 * pay → let modifiers shape the context → deliver (and echo).
 */
public final class SpellCaster {

    public enum Result {
        CAST, INCOMPLETE, COOLDOWN, NO_MANA, NO_REAGENTS;

        public boolean success() {
            return this == CAST;
        }
    }

    public static final int ECHO_DELAY = 10;

    private SpellCaster() {
    }

    public static Result cast(ServerPlayer player, Spell spell, float discount) {
        ServerLevel level = player.serverLevel();
        ResolvedSpell resolved = ResolvedSpell.resolve(level.registryAccess(), org.papiricoh.supernaturalcraft.legacy.Legacies.archive(player), spell);
        if (resolved == null) return fail(player, Result.INCOMPLETE);

        ArcanaData arcana = ManaManager.get(player);
        boolean creative = player.getAbilities().instabuild;
        if (!creative && level.getGameTime() < arcana.cooldownUntil()) return fail(player, Result.COOLDOWN);

        Catalysts.Held catalyst = Catalysts.held(player);
        float mana = resolved.manaCost(discount * (AmuletHelper.isWearing(player) ? SpellCost.AMULET_DISCOUNT : 1.0f)
                * catalyst.manaMultiplier(resolved));
        if (!creative && !catalyst.paysOwnCost() && arcana.mana() < mana) return fail(player, Result.NO_MANA);

        List<SigilComponent.Reagent> reagents = resolved.reagents();
        if (!creative && !hasReagents(player.getInventory(), reagents)) return fail(player, Result.NO_REAGENTS);

        if (!creative) {
            if (catalyst.paysOwnCost()) catalyst.pay(player, mana);
            else ManaManager.tryConsume(player, mana);
            takeReagents(player.getInventory(), reagents);
            arcana.setCooldownUntil(level.getGameTime() + Math.round(resolved.cooldown() * catalyst.cooldownMultiplier(resolved)));
        }

        SpellContext ctx = new SpellContext(level, player);
        ctx.color = resolved.effects().getFirst().sigil().color();
        resolved.formBehavior().setup(ctx, resolved.form());
        resolved.modifiers().forEach(m -> m.behavior().modify(ctx, m.sigil()));
        catalyst.shape(ctx, resolved);
        deliver(ctx, resolved);
        catalyst.afterCast(player, resolved, ctx);
        NeoForge.EVENT_BUS.post(new SpellCastEvent(player, resolved, ctx));
        return Result.CAST;
    }

    private static void deliver(SpellContext ctx, ResolvedSpell spell) {
        spell.formBehavior().deliver(ctx, spell);
        ctx.level.playSound(null, ctx.caster.blockPosition(), AllSounds.SPELL_CAST.get(), SoundSource.PLAYERS, 0.8f,
                0.9f + ctx.level.random.nextFloat() * 0.2f);
        for (int i = 1; i <= ctx.echoes; i++) {
            SpellContext echo = ctx.copy();
            echo.echoes = 0;
            ServerScheduler.schedule(ECHO_DELAY * i, () -> {
                if (echo.caster.isAlive()) {
                    spell.formBehavior().deliver(echo, spell);
                }
            });
        }
    }

    private static Result fail(ServerPlayer player, Result result) {
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.cast." + result.name().toLowerCase())
                .withStyle(ChatFormatting.GRAY), true);
        player.serverLevel().playSound(null, player.blockPosition(), AllSounds.SPELL_FIZZLE.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
        return result;
    }

    static Map<net.minecraft.world.item.Item, Integer> tally(List<SigilComponent.Reagent> reagents) {
        Map<net.minecraft.world.item.Item, Integer> need = new HashMap<>();
        for (SigilComponent.Reagent r : reagents) need.merge(r.item().value(), r.count(), Integer::sum);
        return need;
    }

    public static boolean hasReagents(Inventory inv, List<SigilComponent.Reagent> reagents) {
        for (Map.Entry<net.minecraft.world.item.Item, Integer> e : tally(reagents).entrySet()) {
            if (inv.countItem(e.getKey()) < e.getValue()) return false;
        }
        return true;
    }

    private static void takeReagents(Inventory inv, List<SigilComponent.Reagent> reagents) {
        for (Map.Entry<net.minecraft.world.item.Item, Integer> e : tally(reagents).entrySet()) {
            int left = e.getValue();
            for (int slot = 0; slot < inv.getContainerSize() && left > 0; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (stack.is(e.getKey())) {
                    int take = Math.min(left, stack.getCount());
                    stack.shrink(take);
                    left -= take;
                }
            }
        }
    }

}
