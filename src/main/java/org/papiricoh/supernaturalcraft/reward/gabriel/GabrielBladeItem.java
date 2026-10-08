package org.papiricoh.supernaturalcraft.reward.gabriel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.hunter.HunterBladeItem;

import java.util.List;

/**
 * Gabriel's own archangel blade (v0.14), golden: holy and deadly to demons like Lucifer's, and now and then
 * ({@link #DOUBLES_CHANCE}) two fleeting doubles of its wielder strike beside them, each for {@link #DOUBLE_SHARE} of the
 * blow: afterimages in a flash of gold, no creatures of their own.
 */
public class GabrielBladeItem extends HunterBladeItem {

    /** Chance that a hit calls the doubles, and what each of them deals of the blow. */
    public static final float DOUBLES_CHANCE = 0.25f, DOUBLE_SHARE = 0.3f;

    public GabrielBladeItem(Tier tier, Properties properties) {
        super(tier, 4.0f, true, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean hit = super.hurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel && attacker.getRandom().nextFloat() < DOUBLES_CHANCE) afterimages(attacker, target);
        return hit;
    }

    /** The two afterimages strike {@code target} beside {@code attacker}. @return the damage they meant to deal, each */
    public static float afterimages(LivingEntity attacker, LivingEntity target) {
        float each = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * DOUBLE_SHARE;
        if (!(attacker.level() instanceof ServerLevel level)) return each;
        Vec3 side = target.position().subtract(attacker.position()).multiply(1, 0, 1);
        side = side.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : new Vec3(-side.z, 0, side.x).normalize();
        DamageSource source = attacker instanceof Player p ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker);
        for (int s : new int[]{-1, 1}) {
            Vec3 at = attacker.position().add(side.scale(s * 0.9));
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 10, 0.2, 0.6, 0.2, 0.01);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 1, 0, 0, 0, 0);
            target.invulnerableTime = 0;
            target.hurt(source, each);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.4f);
        return each;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.gabriel_blade").withStyle(ChatFormatting.GOLD));
    }
}
