package org.papiricoh.supernaturalcraft.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.hellhound.BoundHellhoundEntity;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;

/** Lilith's Whistle: calls one hellhound to its holder's side for a minute. Costs mana; one hound at a time. */
public class HoundWhistleItem extends LoreItem {

    public static final float MANA = 40f;
    public static final int COOLDOWN = 20 * 90;

    public HoundWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (houndOf(server, sp) != null) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.whistle.already").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!ManaManager.tryConsume(sp, MANA)) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.cast.no_mana").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        Vec3 at = sp.position().add(sp.getLookAngle().multiply(1, 0, 1).normalize().scale(2));
        BoundHellhoundEntity.call(server, sp, at.x, sp.getY(), at.z);
        server.playSound(null, sp.blockPosition(), AllSounds.HOUND_WHISTLE.get(), SoundSource.PLAYERS, 1.5f, 1.0f);
        sp.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }

    /** The hound already answering {@code player}, if any. */
    public static BoundHellhoundEntity houndOf(ServerLevel level, Player player) {
        for (BoundHellhoundEntity h : level.getEntitiesOfClass(BoundHellhoundEntity.class, new AABB(player.blockPosition()).inflate(64))) {
            if (player.getUUID().equals(h.ownerId()) && h.isAlive()) return h;
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.hound_whistle.use", Math.round(MANA)).withStyle(ChatFormatting.GOLD));
    }
}
