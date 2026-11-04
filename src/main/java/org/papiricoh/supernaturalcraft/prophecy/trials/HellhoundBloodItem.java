package org.papiricoh.supernaturalcraft.prophecy.trials;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.prophecy.Prophecies;
import org.papiricoh.supernaturalcraft.reward.LoreItem;

/** The blood of a hellhound killed on the first trial (v0.36): poured over oneself, the trial is done. */
public class HellhoundBloodItem extends LoreItem {

    public HellhoundBloodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (Prophecies.get(sp).trial() != TrialRules.HOUND) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.trials.not_now").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        sp.serverLevel().sendParticles(new DustParticleOptions(new Vector3f(0.35f, 0.02f, 0.02f), 1.6f),
                sp.getX(), sp.getY() + 1.2, sp.getZ(), 60, 0.3, 0.7, 0.3, 0.02);
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 1f, 0.5f);
        stack.shrink(1);
        Trials.advance(sp, TrialRules.HOUND);
        return InteractionResultHolder.consume(stack);
    }
}
