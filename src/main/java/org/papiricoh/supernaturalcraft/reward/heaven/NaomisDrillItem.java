package org.papiricoh.supernaturalcraft.reward.heaven;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;
import org.papiricoh.supernaturalcraft.weapon.melee.GeoSwordItem;

import java.util.List;
import java.util.UUID;

/**
 * Naomi's drill (v0.18): holy, and {@link #HITS} blows in a row on the same creature (never a boss, never a player) reprogram it to
 * fight for its wielder for {@link #TICKS} ticks: it turns on whatever threatens them and never on them; its own blows are worth
 * the drill's Ascension ({@link Ascension#multiplier}). A GeckoLib sword ({@code geo/item/naomis_drill.geo.json}, idle loop
 * only). The behaviour of a reprogrammed creature is {@link NaomisRewardEvents}.
 */
public class NaomisDrillItem extends GeoSwordItem {

    /** Blows in a row, at most {@link #CHAIN_GAP} ticks apart, that reprogram a creature; how long it serves. */
    public static final int HITS = 3, CHAIN_GAP = 60, TICKS = 200;
    /** The creature's persistent data while it serves. */
    public static final String ALLY = "sn_reprogrammed_by", UNTIL = "sn_reprogrammed_until", POWER = "sn_reprogrammed_power";
    private static final String HIT_BY = "sn_drill_by", HIT_COUNT = "sn_drill_hits", HIT_AT = "sn_drill_at";

    public NaomisDrillItem(Tier tier, Properties properties) {
        super("naomis_drill", tier, properties, List.of(), List.of());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player p && target.level() instanceof ServerLevel) countHit(stack, target, p);
        return super.hurtEnemy(stack, target, attacker);
    }

    /** One blow of the drill on {@code target}: the third in a row reprograms it. @return whether it now serves {@code by} */
    public static boolean countHit(ItemStack stack, LivingEntity target, Player by) {
        if (!(target instanceof Mob mob) || BossDamage.isBoss(target) || !target.isAlive()) return false;
        CompoundTag data = target.getPersistentData();
        long now = target.level().getGameTime();
        boolean chain = data.hasUUID(HIT_BY) && data.getUUID(HIT_BY).equals(by.getUUID()) && now - data.getLong(HIT_AT) <= CHAIN_GAP;
        int hits = chain ? data.getInt(HIT_COUNT) + 1 : 1;
        data.putUUID(HIT_BY, by.getUUID());
        data.putLong(HIT_AT, now);
        data.putInt(HIT_COUNT, hits);
        if (hits < HITS) return false;
        data.remove(HIT_COUNT);
        reprogram(mob, by, Ascension.multiplier(stack), now + TICKS);
        return true;
    }

    /** {@code mob} serves {@code owner} until {@code until}, its blows worth {@code power} times their own. */
    public static void reprogram(Mob mob, Player owner, float power, long until) {
        CompoundTag data = mob.getPersistentData();
        data.putUUID(ALLY, owner.getUUID());
        data.putLong(UNTIL, until);
        data.putFloat(POWER, power);
        if (mob.getTarget() == owner) mob.setTarget(null);
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + mob.getBbHeight() * 0.7, mob.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
            level.playSound(null, mob.blockPosition(), AllSounds.heaven("naomi.drill"), SoundSource.PLAYERS, 1f, 1.6f);
        }
        owner.displayClientMessage(Component.translatable("message.supernaturalcraft.naomis_drill.reprogrammed", mob.getDisplayName())
                .withStyle(ChatFormatting.AQUA), true);
    }

    /** Whom {@code e} serves now, if it was reprogrammed and its time has not run out. */
    public static @Nullable UUID servant(LivingEntity e) {
        CompoundTag data = e.getPersistentData();
        if (!data.hasUUID(ALLY)) return null;
        if (e.level().getGameTime() > data.getLong(UNTIL)) return null;
        return data.getUUID(ALLY);
    }

    /** It is itself again. */
    public static void forget(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        data.remove(ALLY);
        data.remove(UNTIL);
        data.remove(POWER);
        mob.setTarget(null);
    }
}
