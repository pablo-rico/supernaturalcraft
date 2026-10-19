package org.papiricoh.supernaturalcraft.entity.boss.rowena;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.PersonalComponent;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.magic.ArcanaCost;
import org.papiricoh.supernaturalcraft.magic.MagicAssets;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.weapon.catalyst.CatalystItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rowena's Grimoire (v0.21, P6): her first-victory spoil, a catalyst. It <b>remembers</b> the last <em>aimed</em> bowl spell its
 * bearer spoke at a spell bowl while carrying it (a curse, a binding, a banishing, a scrying: any recipe that
 * {@code requires_target}; never a brew, which would make something from nothing) — the recipe and what was in the bowl
 * ({@link GrimoireEcho}). Used, it speaks that spell again <b>without a bowl</b>, at once (no recitation), for the spell's usual
 * mana, at most once every {@link RowenaBalance#GRIMOIRE_COOLDOWN} ticks; a taglock or a vial of bound blood in the other hand
 * names a new target instead of the remembered one. Nothing in the remembered bowl is spent. If the spell finds nothing to act
 * on, no cooldown is set.
 */
public class RowenasGrimoireItem extends CatalystItem {

    public RowenasGrimoireItem(Properties properties) {
        super(properties);
    }

    @Override
    protected float ownMana() {
        return 0f;
    }

    @Override
    protected int ownCooldown() {
        return RowenaBalance.GRIMOIRE_COOLDOWN;
    }

    /** {@code player} just spoke {@code recipe} at a bowl holding {@code contents}: every grimoire they carry remembers it. */
    public static void remember(ServerPlayer player, ResourceLocation recipeId, BowlSpellRecipe recipe, BowlContents contents) {
        if (!recipe.requiresTarget()) return;
        GrimoireEcho echo = new GrimoireEcho(recipeId, contents);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof RowenasGrimoireItem) s.set(AllDataComponents.GRIMOIRE_ECHO.get(), echo);
        }
    }

    public static @Nullable GrimoireEcho echo(ItemStack stack) {
        return stack.get(AllDataComponents.GRIMOIRE_ECHO.get());
    }

    @Override
    protected boolean ownSpell(ServerPlayer player, ItemStack stack) {
        return recast(player, stack) == null;
    }

    /** Speaks the remembered spell again. @return why it could not (a translation key, already told to the player), or null */
    public static @Nullable String recast(ServerPlayer player, ItemStack stack) {
        GrimoireEcho echo = echo(stack);
        ServerLevel level = player.serverLevel();
        String why = null;
        BowlSpellRecipe recipe = null;
        if (echo == null) {
            why = "message.supernaturalcraft.rowenas_grimoire.empty";
        } else {
            Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(echo.recipe());
            if (holder.isEmpty() || !(holder.get().value() instanceof BowlSpellRecipe r)) why = "message.supernaturalcraft.rowenas_grimoire.forgotten";
            else recipe = r;
        }
        BowlCast cast = null;
        if (why == null) {
            cast = new BowlCast(level, player.blockPosition(), player, retarget(player, echo.contents()), recipe);
            why = recipe.conditions().check(level, player);
            if (why == null && recipe.requiresTarget() && cast.target().isEmpty()) why = "message.supernaturalcraft.bowl.needs_target";
            if (why == null) why = recipe.effect().precheck(cast);
        }
        if (why == null && !player.getAbilities().instabuild
                && !ManaManager.tryConsume(player, ArcanaCost.apply(player, ArcanaCost.Tool.BOWL, recipe.school(), recipe.manaCost()))) {
            why = "message.supernaturalcraft.cast.no_mana";
        }
        if (why == null && !recipe.effect().perform(cast)) why = "message.supernaturalcraft.bowl.fizzled";
        if (why != null) {
            player.displayClientMessage(Component.translatable(why).withStyle(ChatFormatting.GRAY), true);
            return why;
        }
        for (ItemStack back : cast.returned()) if (!player.getInventory().add(back)) player.drop(back, false);
        String path = echo.recipe().getPath();
        int seal = MagicAssets.sealIndex(path.substring(path.lastIndexOf('/') + 1));
        Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.6)).subtract(0, 0.4, 0);
        MagicFx.bowlSeal(level, recipe.school(), hand, player, seal, 30);
        if (cast.destination() != null) {
            MagicFx.bowlSend(level, recipe.school(), hand, cast.destination(), cast.destinationEntity(), seal,
                    (int) Math.max(20, Math.min(60, hand.distanceTo(cast.destination()) * 2)));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
        player.swing(InteractionHand.MAIN_HAND, true);
        return null;
    }

    /** The remembered bowl, its target swapped for a taglock or bound blood in the other hand, if one is held. */
    static BowlContents retarget(ServerPlayer player, BowlContents remembered) {
        ItemStack other = player.getOffhandItem().getItem() instanceof RowenasGrimoireItem ? player.getMainHandItem() : player.getOffhandItem();
        if (other.isEmpty() || PersonalComponent.resolve(other).isEmpty()) return remembered;
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack s : remembered.stacks()) if (PersonalComponent.resolve(s).isEmpty()) items.add(s.copy());
        items.add(other.copyWithCount(1));
        return BowlContents.of(items, remembered.liquids().stream().filter(d -> PersonalComponent.resolve(d).isEmpty()).toList());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.rowenas_grimoire").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        GrimoireEcho echo = echo(stack);
        if (echo == null) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.rowenas_grimoire.empty").withStyle(ChatFormatting.GRAY));
        } else {
            String path = echo.recipe().getPath();
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.rowenas_grimoire.echo",
                    Component.translatable("bowl_spell." + echo.recipe().getNamespace() + "." + path.substring(path.lastIndexOf('/') + 1)))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
