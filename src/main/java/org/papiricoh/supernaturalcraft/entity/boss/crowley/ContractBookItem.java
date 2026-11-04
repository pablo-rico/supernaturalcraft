package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.List;
import java.util.UUID;

/**
 * Crowley's Contract Book (v0.35), taken from him when he is executed: right-click a creature to bind it by contract — it
 * fights for you for {@link CrowleyBalance#BOOK_SERVE_TICKS}, then the contract lapses. Paid in experience levels by its size
 * ({@link CrowleyBalance#bookCost}). One at a time, a rest between contracts. Never a boss, a player, anything the Colt cannot
 * kill, or anything bigger than {@link CrowleyBalance#BOOK_MAX_HEALTH}. The binding itself lives on the creature
 * ({@link ContractBindings}).
 */
public class ContractBookItem extends Item {

    public ContractBookItem(Properties properties) {
        super(properties);
    }

    /** Why {@code target} cannot be bound, or null. */
    public static @Nullable String refusal(LivingEntity target) {
        if (!(target instanceof Mob) || target instanceof Player) return "not_a_mob";
        if (target.getType().is(AllTags.Entities.BOSSES) || target.getType().is(AllTags.Entities.COLT_IMMUNE)
                || target instanceof CrowleyCompanionEntity) return "too_great";
        if (target.getMaxHealth() > CrowleyBalance.BOOK_MAX_HEALTH) return "too_great";
        if (ContractBindings.owner(target) != null) return "bound";
        return null;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp) || !(player.level() instanceof ServerLevel level)) return InteractionResult.PASS;
        String why = refusal(target);
        long now = level.getGameTime();
        if (why == null && ContractBindings.resting(sp, now)) why = "resting";
        int cost = CrowleyBalance.bookCost(target.getMaxHealth());
        if (why == null && !sp.getAbilities().instabuild && sp.experienceLevel < cost) why = "cost";
        if (why != null) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.contract_book." + why, cost).withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        if (!sp.getAbilities().instabuild) sp.giveExperienceLevels(-cost);
        ContractBindings.bind((Mob) target, sp, now);
        level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight(), target.getZ(), 30, 0.4, 0.4, 0.4, 0.5);
        level.playSound(null, target.blockPosition(), AllSounds.crowley("seal_break"), SoundSource.PLAYERS, 1f, 1.3f);
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.contract_book.signed", target.getDisplayName()).withStyle(ChatFormatting.DARK_RED), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.contract_book").withStyle(ChatFormatting.GRAY));
    }

    static UUID id(Player p) {
        return p.getUUID();
    }
}
