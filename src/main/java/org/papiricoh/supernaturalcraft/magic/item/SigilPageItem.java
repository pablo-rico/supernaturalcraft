package org.papiricoh.supernaturalcraft.magic.item;

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
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;

import java.util.List;

/**
 * A torn page bearing one sigil. Reading it teaches the sigil for good. Tier-three sigils are
 * written in a hand only those touched by grace can follow.
 */
public class SigilPageItem extends Item {

    public SigilPageItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Item item, ResourceLocation sigil) {
        ItemStack stack = new ItemStack(item);
        stack.set(AllDataComponents.SIGIL_PAGE, sigil);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation id = stack.get(AllDataComponents.SIGIL_PAGE);
        if (id == null) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp) {
            SigilComponent sigil = sp.registryAccess().registryOrThrow(SNRegistries.SIGIL).get(id);
            ArcanaData arcana = ManaManager.get(sp);
            if (sigil == null) {
                sp.displayClientMessage(Component.translatable("message.supernaturalcraft.page.illegible").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            if (sigil.tier() >= 3 && !arcana.hasGrace()) {
                sp.displayClientMessage(Component.translatable("message.supernaturalcraft.page.needs_grace").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            if (!arcana.learn(id)) {
                sp.displayClientMessage(Component.translatable("message.supernaturalcraft.page.known").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.page.learned",
                    Component.translatable(SigilComponent.translationKey(id)).withStyle(ChatFormatting.GOLD)), false);
            sp.serverLevel().playSound(null, sp.blockPosition(), AllSounds.SIGIL_LEARN.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            SNNetworking.syncArcana(sp);
            stack.consume(1, player);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = stack.get(AllDataComponents.SIGIL_PAGE);
        return id == null ? super.getName(stack)
                : Component.translatable("item.supernaturalcraft.sigil_page.named", Component.translatable(SigilComponent.translationKey(id)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = stack.get(AllDataComponents.SIGIL_PAGE);
        if (id != null) {
            tooltip.add(Component.translatable(SigilComponent.translationKey(id) + ".desc").withStyle(ChatFormatting.GRAY));
        }
    }
}
