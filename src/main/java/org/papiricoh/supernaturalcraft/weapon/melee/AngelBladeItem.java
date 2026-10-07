package org.papiricoh.supernaturalcraft.weapon.melee;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import org.papiricoh.supernaturalcraft.hunter.HunterBladeItem;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.util.ServerScheduler;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The angel blade. Hold use to gather light, release to dash: everything you pass through is
 * struck once with holy force. Still triples damage against demons.
 */
public class AngelBladeItem extends HunterBladeItem {

    public static final int FULL_CHARGE = 20, DASH_TICKS = 6, COOLDOWN = 60;

    public AngelBladeItem(Tier tier, Properties properties) {
        super(tier, 3.0f, false, properties);
    }

    public static float dashDamage(float charge) {
        return 6f + 4f * charge;
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
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        float charge = Math.min(1f, (getUseDuration(stack, user) - timeLeft) / (float) FULL_CHARGE);
        if (charge < 0.5f || !(user instanceof ServerPlayer player)) return;
        dash(player, charge);
        player.getCooldowns().addCooldown(this, COOLDOWN);
    }

    /** Launches the dash and strikes along it for a few ticks. Public for tests. */
    public static void dash(ServerPlayer player, float charge) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().multiply(1, 0.2, 1).normalize().scale(1.6 * charge);
        player.setDeltaMovement(look.x, Math.max(0.1, look.y), look.z);
        player.hurtMarked = true;
        level.playSound(null, player.blockPosition(), AllSounds.LUCIFER_WINGS.get(), SoundSource.PLAYERS, 0.8f, 1.8f);
        Set<UUID> struck = new HashSet<>();
        for (int t = 0; t <= DASH_TICKS; t++) {
            ServerScheduler.schedule(Math.max(1, t), () -> {
                if (!player.isAlive()) return;
                level.sendParticles(AllParticles.GRACE.get(), player.getX(), player.getY() + 1, player.getZ(), 4, 0.2, 0.4, 0.2, 0.01);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1.2))) {
                    if (e == player || ResolvedSpell.isFriend(player, e) || !struck.add(e.getUUID())) continue;
                    e.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, player), dashDamage(charge));
                }
            });
        }
    }
}
