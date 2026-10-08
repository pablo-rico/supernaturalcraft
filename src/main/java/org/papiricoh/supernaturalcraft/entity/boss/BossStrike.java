package org.papiricoh.supernaturalcraft.entity.boss;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.balance.Balance;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

import java.lang.reflect.Field;

/**
 * How a great enemy's blow lands (v0.15): {@link ProgressionScale#split} cuts it in two, the ordinary part with the
 * attack's own damage type and the rest as Divine Wrath, which no armour, enchantment, effect or shield turns aside.
 *
 * <p>To the victim it is still one blow: the Divine Wrath only lands when the ordinary part was a fresh hit (outside the
 * victim's invulnerability window), and afterwards the window remembers the ordinary part, as vanilla would, so an attack
 * that strikes every tick is no stronger than before. Minions and summons never come through here.
 */
public final class BossStrike {

    private BossStrike() {
    }

    private static @Nullable Field lastHurt;
    private static boolean lookedUp;

    /** {@code total} (the boss's multiplier already applied) from {@code attacker}, with damage type {@code type}. */
    public static boolean deal(Entity attacker, Entity victim, ResourceKey<DamageType> type, float total) {
        return deal(attacker, victim, AllDamageTypes.source(attacker.level(), type, attacker), total);
    }

    /**
     * A blow from something {@code owner} fired (a shard, a bolt): a great enemy's lands as {@link #deal}, anyone else's
     * (a hunter's catalyst, a demon's bolt) as plain damage.
     */
    public static boolean land(@Nullable Entity owner, Entity victim, DamageSource source, float amount) {
        if (owner != null && !owner.level().isClientSide && BossDamage.isBoss(owner)) return deal(owner, victim, source, amount);
        return victim.hurt(source, amount);
    }

    /** As above, with a source of the attack's own (a projectile's, say). */
    public static boolean deal(Entity attacker, Entity victim, DamageSource ordinary, float total) {
        if (total <= 0) return false;
        if (!(victim instanceof LivingEntity living)) return victim.hurt(ordinary, total);
        ProgressionScale.Split split = Balance.split(total);
        boolean fresh = living.invulnerableTime <= 10;
        boolean landed = living.hurt(ordinary, split.ordinary());
        if (!fresh || split.divine() <= 0 || !living.isAlive()) return landed;

        DamageSource divine = new DamageSource(attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(AllDamageTypes.DIVINE_WRATH), ordinary.getDirectEntity(), attacker);
        float remembered = lastHurt(living);
        boolean wrath = living.hurt(divine, split.divine());
        if (remembered >= 0) setLastHurt(living, remembered);
        if (!wrath && SNConfig.DIVINE_AS_HEALTH_LOSS.get() && living instanceof Player p && !p.isInvulnerableTo(divine)
                && !p.isDeadOrDying()) {
            // Something cancelled it (some shields do): it comes straight off their health.
            float h = p.getHealth() - split.divine();
            if (h <= 0) {
                p.setHealth(0);
                p.die(divine);
            } else {
                p.setHealth(h);
            }
            wrath = true;
        }
        return landed || wrath;
    }

    /** The victim's invulnerability-window memory ({@code LivingEntity.lastHurt}), or -1 if it can't be read. */
    private static float lastHurt(LivingEntity e) {
        Field f = field();
        try {
            return f == null ? -1 : f.getFloat(e);
        } catch (IllegalAccessException ex) {
            return -1;
        }
    }

    private static void setLastHurt(LivingEntity e, float value) {
        Field f = field();
        try {
            if (f != null) f.setFloat(e, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static @Nullable Field field() {
        if (!lookedUp) {
            lookedUp = true;
            try {
                Field f = LivingEntity.class.getDeclaredField("lastHurt");
                f.setAccessible(true);
                lastHurt = f;
            } catch (ReflectiveOperationException | RuntimeException ex) {
                lastHurt = null;
            }
        }
        return lastHurt;
    }
}
