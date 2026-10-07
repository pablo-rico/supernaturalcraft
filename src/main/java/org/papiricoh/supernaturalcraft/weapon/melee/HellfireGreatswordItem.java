package org.papiricoh.supernaturalcraft.weapon.melee;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.hazard.FlameTrail;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.List;

/**
 * Tier III. A slow, heavy blade split by hellfire. Hold use to heat it, release for a cleaving
 * blow across a 120° cone that leaves a line of burning ground behind.
 */
public class HellfireGreatswordItem extends GeoSwordItem {

    public static final int FULL_CHARGE = 30, COOLDOWN = 80, TRAIL_LENGTH = 8;
    public static final float SLASH_RANGE = 5f, SLASH_DAMAGE = 12f;

    public HellfireGreatswordItem(Tier tier, Properties properties) {
        super("hellfire_greatsword", tier, properties, List.of("charge"), List.of("slash"));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        play(player, player.getItemInHand(hand), "charge");
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        stopPlaying(user, stack, "charge");
        if (getUseDuration(stack, user) - timeLeft < FULL_CHARGE || !(user instanceof ServerPlayer player)) return;
        cleave(player);
        play(player, stack, "slash");
        player.getCooldowns().addCooldown(this, COOLDOWN);
    }

    public static void cleave(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 facing = player.getLookAngle().multiply(1, 0, 1).normalize();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SLASH_RANGE))) {
            Vec3 to = e.position().subtract(player.position()).multiply(1, 0, 1);
            if (e == player || ResolvedSpell.isFriend(player, e) || to.length() > SLASH_RANGE || to.normalize().dot(facing) < 0.5) continue;
            e.hurt(AllDamageTypes.source(level, AllDamageTypes.HELLFIRE, player), SLASH_DAMAGE);
            e.igniteForSeconds(4);
        }
        for (int i = 1; i <= TRAIL_LENGTH; i++) {
            Vec3 p = player.position().add(facing.scale(i));
            level.addFreshEntity(new FlameTrail(level, player, p.x, Math.floor(p.y), p.z));
        }
        for (int a = -6; a <= 6; a++) {
            double ang = Math.atan2(facing.z, facing.x) + Math.toRadians(a * 10);
            Vec3 p = player.position().add(Math.cos(ang) * 3, 1.0, Math.sin(ang) * 3);
            level.sendParticles(AllParticles.HELLFIRE.get(), p.x, p.y, p.z, 3, 0.2, 0.2, 0.2, 0.05);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + facing.x * 2, player.getY() + 1, player.getZ() + facing.z * 2, 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f, 0.5f);
    }
}
