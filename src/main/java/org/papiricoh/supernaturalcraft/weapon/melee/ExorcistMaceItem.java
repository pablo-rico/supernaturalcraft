package org.papiricoh.supernaturalcraft.weapon.melee;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.hunter.DemonBane;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * A blessed mace: slow, heavy, it may stun what it strikes. Brought down from a height (a vanilla
 * smash attack) it also sends out a ring of holy force that scales with the fall.
 */
public class ExorcistMaceItem extends MaceItem implements DemonBane {

    public static final float STUN_CHANCE = 0.15f;
    public static final int STUN_TICKS = 20;

    public ExorcistMaceItem(Properties properties) {
        super(properties);
    }

    public static float shockwaveRadius(float fall) {
        return 3f + fall / 2f;
    }

    public static float shockwaveDamage(float fall) {
        return 4f + fall;
    }

    @Override
    public float demonDamageMultiplier() {
        return 1.5f;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean smash = canSmashAttack(attacker);
        float fall = attacker.fallDistance;
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel level) {
            if (smash) {
                shockwave(level, attacker, target, fall, stack);
            } else if (attacker.getRandom().nextFloat() < STUN_CHANCE) {
                target.addEffect(new MobEffectInstance(AllMobEffects.STUNNED, STUN_TICKS, 0), attacker);
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY() + 0.3, target.getZ(), 10, 0.3, 0.1, 0.3, 0.1);
            }
        }
        return result;
    }

    private static void shockwave(ServerLevel level, LivingEntity attacker, LivingEntity struck, float fall, ItemStack mace) {
        float damage = org.papiricoh.supernaturalcraft.weapon.ascension.Ascension.scale(mace, shockwaveDamage(fall));
        float r = shockwaveRadius(fall);
        Vec3 c = struck.position();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, struck.getBoundingBox().inflate(r, 2, r))) {
            if (e == attacker || e == struck || ResolvedSpell.isFriend(attacker, e) || e.position().distanceTo(c) > r) continue;
            e.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, attacker), damage);
            Vec3 away = e.position().subtract(c).multiply(1, 0, 1).normalize();
            e.push(away.x * 0.6, 0.35, away.z * 0.6);
            e.hurtMarked = true;
        }
        for (int i = 0; i < 32; i++) {
            double a = Math.PI * 2 * i / 32;
            level.sendParticles(AllParticles.GRACE.get(), c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
        }
        TelegraphMarker.ring(level, c, r, TelegraphMarker.GOLD, 8);
    }
}
