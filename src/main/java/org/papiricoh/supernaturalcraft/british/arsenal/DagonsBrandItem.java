package org.papiricoh.supernaturalcraft.british.arsenal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import org.papiricoh.supernaturalcraft.balance.WeaponDamage;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;

import java.util.List;

/**
 * Dagon's brand (v0.41): a charm of a Prince of Hell's fire. Raised, it throws a ring of hellfire round its bearer: every foe in
 * it burns and is hurt ({@link ArsenalRules#BRAND_DAMAGE} at the magic scale; a boss through its own pipeline), and the bearer
 * is kept from the flames for a while. It wears out.
 */
public class DagonsBrandItem extends Item {

    public DagonsBrandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            ring(server, sp);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        player.getCooldowns().addCooldown(this, ArsenalRules.BRAND_COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** The ring. @return how many foes it caught */
    public static int ring(ServerLevel level, ServerPlayer bearer) {
        float r = ArsenalRules.BRAND_RADIUS;
        int n = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, bearer.getBoundingBox().inflate(r, 2, r))) {
            if (!Hex.target(bearer, e) || e.distanceTo(bearer) > r + 0.5) continue;
            float dmg = Ascension.vsBoss(bearer, e, WeaponDamage.magic(ArsenalRules.BRAND_DAMAGE));
            e.hurt(AllDamageTypes.source(level, AllDamageTypes.HELLFIRE, bearer), dmg);
            e.igniteForSeconds(ArsenalRules.BRAND_BURN_SECONDS);
            n++;
        }
        bearer.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ArsenalRules.BRAND_SHIELD_TICKS, 0));
        for (int i = 0; i < 48; i++) {
            double a = Mth.TWO_PI * i / 48;
            level.sendParticles(AllParticles.HELLFIRE.get(), bearer.getX() + Math.cos(a) * r, bearer.getY() + 0.2, bearer.getZ() + Math.sin(a) * r,
                    2, 0.05, 0.1, 0.05, 0.02);
        }
        level.sendParticles(ParticleTypes.FLAME, bearer.getX(), bearer.getY() + 0.5, bearer.getZ(), 30, r / 2, 0.2, r / 2, 0.05);
        level.playSound(null, bearer.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1f, 0.6f);
        return n;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.dagons_brand").withStyle(ChatFormatting.GRAY));
    }
}
