package org.papiricoh.supernaturalcraft.weapon.melee;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.projectile.SoulCrescent;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;

import java.util.List;

/**
 * Tier III. Every swing reaps a 140° arc; every blow drinks a little of the life it takes. Hold use
 * to gather souls, release to throw a crescent that cuts through everything in its path.
 */
public class SoulScytheItem extends GeoSwordItem {

    public static final float ARC_DEGREES = 140, ARC_RANGE = 3.5f, ARC_SHARE = 0.6f, LIFESTEAL = 0.15f, CRESCENT_MANA = 15f;
    public static final int FULL_CHARGE = 20, COOLDOWN = 40;

    public SoulScytheItem(Tier tier, Properties properties) {
        super("soul_scythe", tier, properties, List.of("charge"), List.of("release"));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel level && attacker instanceof Player player && player.getAttackStrengthScale(0.5f) > 0.9f) {
            reap(level, attacker, target);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /** The arc: everything else within reach and inside the swing takes part of the blow. */
    public static void reap(ServerLevel level, LivingEntity attacker, LivingEntity struck) {
        float dmg = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * ARC_SHARE;
        Vec3 facing = attacker.getLookAngle().multiply(1, 0, 1).normalize();
        double cos = Math.cos(Math.toRadians(ARC_DEGREES / 2));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(ARC_RANGE))) {
            Vec3 to = e.position().subtract(attacker.position()).multiply(1, 0, 1);
            if (e == attacker || e == struck || ResolvedSpell.isFriend(attacker, e) || to.length() > ARC_RANGE
                    || to.normalize().dot(facing) < cos) continue;
            e.hurt(level.damageSources().mobAttack(attacker), dmg);
        }
        level.playSound(null, attacker.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.7f);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild && ManaManager.get(player).mana() < CRESCENT_MANA) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.cast.no_mana").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        play(player, stack, "charge");
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        stopPlaying(user, stack, "charge");
        int held = getUseDuration(stack, user) - timeLeft;
        if (held < FULL_CHARGE || !(user instanceof ServerPlayer player) || !ManaManager.tryConsume(player, CRESCENT_MANA)) return;
        throwCrescent(player);
        play(player, stack, "release");
        player.getCooldowns().addCooldown(this, COOLDOWN);
    }

    public static SoulCrescent throwCrescent(ServerPlayer player) {
        SoulCrescent c = new SoulCrescent(player.level(), player);
        Vec3 look = player.getLookAngle();
        c.setPos(player.getX() + look.x, player.getEyeY() - 0.4, player.getZ() + look.z);
        c.shoot(look.x, look.y, look.z, 1.2f, 0f);
        player.level().addFreshEntity(c);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5f, 1.2f);
        return c;
    }
}
