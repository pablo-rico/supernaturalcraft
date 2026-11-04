package org.papiricoh.supernaturalcraft.prophecy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
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
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.List;

/**
 * A tablet of the Word of God (v0.36): used, it opens its next unread chapter ({@link TabletReading}). The first one to touch
 * a tablet is chosen by it ({@link AllDataComponents#TABLET_CHOSEN}) and becomes a prophet for good ({@link #touch}).
 */
public class TabletItem extends Item {

    private final Tablet tablet;

    public TabletItem(Tablet tablet, Properties properties) {
        super(properties);
        this.tablet = tablet;
    }

    public Tablet tablet() {
        return tablet;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            touch(sp, stack, tablet);
            TabletReading.open(sp, tablet);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp && level.getGameTime() % 20 == 0) touch(sp, stack, tablet);
    }

    /** A hunter's hand on a tablet: if no one has been chosen by it yet, it is them, and they are a prophet from now on. */
    public static void touch(ServerPlayer sp, ItemStack stack, Tablet tablet) {
        if (stack.has(AllDataComponents.TABLET_CHOSEN.get())) return;
        stack.set(AllDataComponents.TABLET_CHOSEN.get(), sp.getUUID());
        if (tablet == Tablet.LEVIATHAN && ManaManager.get(sp).learn(SupernaturalCraft.asResource("leviathans_bane"))) {
            // The first chapter of the Leviathan Tablet was always plain to read: Leviathan's Bane.
            SNNetworking.syncArcana(sp);
        }
        if (Prophecies.prophet(sp)) return;
        Prophecies.update(sp, p -> p.withProphet(true));
        sp.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.supernaturalcraft.prophet").withStyle(ChatFormatting.GOLD)));
        sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.supernaturalcraft.prophet.sub")));
        sp.serverLevel().playSound(null, sp.blockPosition(), AllSounds.TABLET_WHISPER.get(), SoundSource.PLAYERS, 1.2f, 0.9f);
        ChorusRewards.award(sp, "main/prophet");
    }

    /** Where to look up the reward lines of a chapter in the tooltip. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.tablet." + tablet.id()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(stack.has(AllDataComponents.TABLET_CHOSEN.get())
                ? "tooltip.supernaturalcraft.tablet.chosen" : "tooltip.supernaturalcraft.tablet.unclaimed").withStyle(ChatFormatting.DARK_GRAY));
    }

    static ResourceLocation id(String path) {
        return SupernaturalCraft.asResource(path);
    }
}
