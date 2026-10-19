package org.papiricoh.supernaturalcraft.magic.spell;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.magic.ArcanaCost;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.spell.form.SpellForms;
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
        CAST, INCOMPLETE, COOLDOWN, NO_MANA, NO_REAGENTS,
        /** v0.21: under the Curse of Silence ({@code AllMobEffects.SILENCED}). */
        SILENCED;

        public boolean success() {
            return this == CAST;
        }
    }

    public static final int ECHO_DELAY = 10;

    private SpellCaster() {
    }

    public static Result cast(ServerPlayer player, Spell spell, float discount) {
        return cast(player, spell, discount, org.papiricoh.supernaturalcraft.magic.school.School.NONE);
    }

    /** A spell written in hexed ink of a school is this much stronger when it is of that school (v0.21). */
    public static final float INK_POTENCY = 1.15f;

    /**
     * As {@link #cast(ServerPlayer, Spell, float)}, from a page written in hexed ink of {@code ink} (v0.21): a spell of that school
     * gets {@link #INK_POTENCY}.
     */
    public static Result cast(ServerPlayer player, Spell spell, float discount, org.papiricoh.supernaturalcraft.magic.school.School ink) {
        ServerLevel level = player.serverLevel();
        if (player.hasEffect(org.papiricoh.supernaturalcraft.registry.AllMobEffects.SILENCED) && !player.getAbilities().instabuild) {
            return fail(player, Result.SILENCED);
        }
        ResolvedSpell resolved = ResolvedSpell.resolve(level.registryAccess(), org.papiricoh.supernaturalcraft.legacy.Legacies.archive(player), spell);
        if (resolved == null) return fail(player, Result.INCOMPLETE);

        ArcanaData arcana = ManaManager.get(player);
        boolean creative = player.getAbilities().instabuild;
        if (!creative && level.getGameTime() < arcana.cooldownUntil()) return fail(player, Result.COOLDOWN);

        Catalysts.Held catalyst = Catalysts.held(player);
        // v0.21: one discount rule for all magic (human, amulet, mastery; capped), then the scroll's and the catalyst's own.
        float mana = resolved.manaCost(discount * ArcanaCost.discount(player, ArcanaCost.Tool.SIGIL, resolved.school())
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
        if (!ink.isNone() && ink == resolved.school()) ctx.potency *= INK_POTENCY;
        resolved.modifiers().forEach(m -> m.behavior().modify(ctx, m.sigil()));
        catalyst.shape(ctx, resolved);
        deliver(ctx, resolved);
        catalyst.afterCast(player, resolved, ctx);
        NeoForge.EVENT_BUS.post(new SpellCastEvent(player, resolved, ctx, creative ? 0f : mana));
        return Result.CAST;
    }

    /** How long a cast circle stays up. */
    public static final int CIRCLE_TICKS = 20;

    private static void deliver(SpellContext ctx, ResolvedSpell spell) {
        castCircle(ctx, spell);
        // The cast's sound plays on the client with its CAST_CIRCLE (v0.21).
        spell.formBehavior().deliver(ctx, spell);
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

    /** CAST_CIRCLE (v0.21): at the hand, or at the feet for the forms that work from there (Burst, Ward). */
    private static void castCircle(SpellContext ctx, ResolvedSpell spell) {
        boolean feet = spell.formBehavior() == SpellForms.BURST || spell.formBehavior() == SpellForms.WARD;
        Vec3 look = ctx.caster.getLookAngle();
        Vec3 at = feet ? ctx.caster.position().add(0, 0.05, 0)
                : ctx.caster.getEyePosition().add(look.scale(0.6)).add(0, -0.35, 0);
        MagicFx.cast(ctx.level, ctx.caster, spell.school(), at, ctx.caster.getEyePosition().add(look.scale(4)), spell.tier(), feet,
                CIRCLE_TICKS);
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
