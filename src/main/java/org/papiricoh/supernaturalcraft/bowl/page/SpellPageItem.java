package org.papiricoh.supernaturalcraft.bowl.page;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import org.papiricoh.supernaturalcraft.bowl.BowlSpells;
import org.papiricoh.supernaturalcraft.client.ClientArcana;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;

/**
 * A loose page with one bowl spell written out, Latin and all ({@link AllDataComponents#BOWL_SPELL}).
 * Reading it teaches the spell, and with it every recipe for the spell; the page is used up. A page
 * for a spell already known is kept.
 */
public class SpellPageItem extends Item {

    public SpellPageItem(Properties props) {
        super(props);
    }

    public static ItemStack of(ResourceLocation spell) {
        ItemStack stack = new ItemStack(AllItems.SPELL_PAGE.get());
        stack.set(AllDataComponents.BOWL_SPELL.get(), spell);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation spell = stack.get(AllDataComponents.BOWL_SPELL.get());
        if (spell == null) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.consume(stack);
        if (BowlSpells.recipesFor(sp.serverLevel().getRecipeManager(), spell).isEmpty()) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.spell_page.illegible").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        ArcanaData arcana = ManaManager.get(sp);
        Component name = Component.translatable(BowlSpells.nameKey(spell)).withStyle(ChatFormatting.GOLD);
        if (!arcana.learnRite(spell)) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.spell_page.known", name).withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        SNNetworking.syncArcana(sp);
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.spell_page.learned", name), false);
        sp.serverLevel().playSound(null, sp.blockPosition(), AllSounds.SPELL_PAGE_LEARN.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        stack.consume(1, player);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation spell = stack.get(AllDataComponents.BOWL_SPELL.get());
        return spell == null ? super.getName(stack)
                : Component.translatable("item.supernaturalcraft.spell_page.named", Component.translatable(BowlSpells.nameKey(spell)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation spell = stack.get(AllDataComponents.BOWL_SPELL.get());
        if (spell == null) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.spell_page.blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable(BowlSpells.nameKey(spell) + ".desc").withStyle(ChatFormatting.GRAY));
        Level level = context.level();
        if (FMLEnvironment.dist.isClient() && level != null && level.isClientSide() && ClientArcana.rites().contains(spell)) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.spell_page.known").withStyle(ChatFormatting.DARK_GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.spell_page.read").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
    }
}
