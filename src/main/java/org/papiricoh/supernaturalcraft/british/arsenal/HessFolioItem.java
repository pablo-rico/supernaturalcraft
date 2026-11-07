package org.papiricoh.supernaturalcraft.british.arsenal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;

import java.util.List;

/**
 * Hess's folio (v0.41): one ward of the British Men of Letters, read aloud once. The reader is warded (resistance II and
 * absorption for {@link ArsenalRules#FOLIO_WARD_TICKS}), and every foe near is hexed (slowed and weakened; a boss shrugs it
 * off). The folio is spent.
 */
public class HessFolioItem extends Item {

    public HessFolioItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            read(server, sp);
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** The ward and the hex. @return how many foes were hexed */
    public static int read(ServerLevel level, ServerPlayer reader) {
        reader.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ArsenalRules.FOLIO_WARD_TICKS, 1));
        reader.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ArsenalRules.FOLIO_WARD_TICKS, 0));
        int n = 0;
        float r = ArsenalRules.FOLIO_RADIUS;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, reader.getBoundingBox().inflate(r))) {
            if (!Hex.target(reader, e) || BossDamage.isBoss(e)) continue;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ArsenalRules.FOLIO_HEX_TICKS, 1));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ArsenalRules.FOLIO_HEX_TICKS, 0));
            level.sendParticles(ParticleTypes.ENCHANT, e.getX(), e.getY() + 1, e.getZ(), 12, 0.3, 0.5, 0.3, 0.5);
            n++;
        }
        level.sendParticles(ParticleTypes.ENCHANT, reader.getX(), reader.getY() + 1.2, reader.getZ(), 80, 1.2, 0.8, 1.2, 0.8);
        level.playSound(null, reader.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1f, 1.3f);
        reader.displayClientMessage(Component.translatable("message.supernaturalcraft.hess_folio.read").withStyle(ChatFormatting.DARK_AQUA), true);
        return n;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.hess_folio").withStyle(ChatFormatting.GRAY));
    }
}
