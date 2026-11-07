package org.papiricoh.supernaturalcraft.pact.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.magic.mastery.Mastery;
import org.papiricoh.supernaturalcraft.pact.Pacts;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;

/**
 * A Shard of Castiel's Grace (v0.39): what he gave back, forgiven, before he left. Taken in, it widens the hunter's well for good
 * ({@link Pacts#GRACE_MANA} max mana, once per hunter).
 */
public class GraceShardItem extends Item {

    public GraceShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.consume(stack);
        if (Pacts.get(sp).grace()) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.grace_shard.already").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        Pacts.update(sp, p -> p.withGrace(true));
        Mastery.refreshMana(sp);
        org.papiricoh.supernaturalcraft.network.SNNetworking.syncArcana(sp);
        sp.serverLevel().sendParticles(AllParticles.GRACE.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.4, 0.8, 0.4, 0.08);
        sp.serverLevel().playSound(null, sp.blockPosition(), AllSounds.godstiel("wings"), SoundSource.PLAYERS, 1f, 1.2f);
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.grace_shard.taken", Math.round(Pacts.GRACE_MANA))
                .withStyle(ChatFormatting.AQUA), false);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.grace_shard", Math.round(Pacts.GRACE_MANA)).withStyle(ChatFormatting.AQUA));
    }
}
