package org.papiricoh.supernaturalcraft.magic.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.SNClientHooks;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBook;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCaster;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;

/**
 * The hunter's grimoire: six pages of composed spells. Use casts the open page; sneak-use opens
 * the composer; sneak + scroll wheel (or the cycle key) turns the page.
 */
public class GrimoireItem extends Item {

    /** Sigils every new grimoire owner can already draw. */
    public static final List<ResourceLocation> STARTER_SIGILS = List.of(
            SupernaturalCraft.asResource("touch"), SupernaturalCraft.asResource("bolt"),
            SupernaturalCraft.asResource("smite"), SupernaturalCraft.asResource("mend"));

    public GrimoireItem(Properties properties) {
        super(properties.stacksTo(1).component(AllDataComponents.SPELL_BOOK, SpellBook.EMPTY));
    }

    public static @Nullable InteractionHand heldHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof GrimoireItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof GrimoireItem) return InteractionHand.OFF_HAND;
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) SNClientHooks.openComposer();
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player instanceof ServerPlayer sp) {
            teachStarterSigils(sp);
            SpellBook book = stack.getOrDefault(AllDataComponents.SPELL_BOOK, SpellBook.EMPTY);
            Spell spell = book.current();
            if (spell.isEmpty()) {
                sp.displayClientMessage(Component.translatable("message.supernaturalcraft.grimoire.empty_page")
                        .withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            SpellCaster.Result result = SpellCaster.cast(sp, spell, 1.0f);
            if (result.success()) player.swing(hand, true);
            return result.success() ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    private static void teachStarterSigils(ServerPlayer player) {
        ArcanaData arcana = ManaManager.get(player);
        boolean taught = false;
        for (ResourceLocation id : STARTER_SIGILS) taught |= arcana.learn(id);
        if (taught) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.grimoire.first_open")
                    .withStyle(ChatFormatting.GOLD), false);
            player.serverLevel().playSound(null, player.blockPosition(), AllSounds.SIGIL_LEARN.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            SNNetworking.syncArcana(player);
        }
    }

    /** "Page 2: Holy Lance (Bolt · Smite · Empower)". */
    public static MutableComponent describe(Spell spell, int page) {
        MutableComponent text = Component.translatable("message.supernaturalcraft.grimoire.page", page + 1).withStyle(ChatFormatting.GOLD);
        if (spell.isEmpty()) {
            return text.append(Component.translatable("message.supernaturalcraft.grimoire.blank").withStyle(ChatFormatting.GRAY));
        }
        return text.append(spellName(spell).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                .append(sigilList(spell).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent spellName(Spell spell) {
        if (!spell.name().isBlank()) return Component.literal(spell.name());
        if (spell.form().isEmpty() || spell.effects().isEmpty()) return Component.translatable("spell.supernaturalcraft.unnamed");
        return Component.translatable("spell.supernaturalcraft.auto_name",
                Component.translatable(SigilComponent.translationKey(spell.form().get())),
                Component.translatable(SigilComponent.translationKey(spell.effects().getFirst())));
    }

    public static MutableComponent sigilList(Spell spell) {
        MutableComponent list = Component.empty();
        boolean first = true;
        for (ResourceLocation id : allSigils(spell)) {
            if (!first) list.append(" · ");
            list.append(Component.translatable(SigilComponent.translationKey(id)));
            first = false;
        }
        return list;
    }

    public static List<ResourceLocation> allSigils(Spell spell) {
        java.util.ArrayList<ResourceLocation> all = new java.util.ArrayList<>();
        spell.form().ifPresent(all::add);
        all.addAll(spell.effects());
        all.addAll(spell.modifiers());
        return all;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        SpellBook book = stack.getOrDefault(AllDataComponents.SPELL_BOOK, SpellBook.EMPTY);
        tooltip.add(describe(book.current(), book.selected()));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.grimoire.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
