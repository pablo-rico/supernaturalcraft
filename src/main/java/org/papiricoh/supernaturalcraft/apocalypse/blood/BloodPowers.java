package org.papiricoh.supernaturalcraft.apocalypse.blood;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.allegiance.power.ActivePowers;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerTargets;
import org.papiricoh.supernaturalcraft.balance.WeaponDamage;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.magic.spell.SpellHooks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;

/**
 * The demon blood's three powers (v0.40), cast from the wheel by anyone with the blood in them (the checks, the payment and the
 * cooldown are {@code PowerCaster}'s). Damage goes through {@link WeaponDamage#magic} for mobs and {@link PowerTargets#vsBoss}
 * for bosses; the raised hand strikes Alastair, Lilith and Ruby {@link BloodRules#GREATER_DEMON_BONUS} times harder, as Sam's did.
 */
public final class BloodPowers {

    /** The shove's cone (blocks ahead), its push and its blow. */
    public static final double PUSH_REACH = 10, PUSH_STRENGTH = 2.4;
    public static final float PUSH_DAMAGE = 2f;
    /** What the exorcism does to what it cannot tear out, and the raised hand to something that is not a demon. */
    public static final float EXORCISM_WOUND = 8f, SMITE_OTHER = 6f;
    /** The raised hand's blow on a demon that is a boss's escort or a player (a common demon just dies). */
    public static final float SMITE_DEMON = 24f;

    private BloodPowers() {
    }

    /** @return what the power did, or null if it found nothing to do */
    public static @Nullable ActivePowers.Result perform(ServerPlayer p, Power power, @Nullable LivingEntity target) {
        return switch (power) {
            case BLOOD_PUSH -> push(p);
            case BLOOD_EXORCISM -> exorcism(p, target);
            case BLOOD_SMITE -> smite(p, target);
            default -> null;
        };
    }

    static ActivePowers.Result push(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        List<LivingEntity> foes = PowerTargets.foesAround(p, p.position(), PUSH_REACH);
        int n = 0;
        for (LivingEntity e : foes) {
            Vec3 to = e.position().subtract(p.position());
            if (to.lengthSqr() > PUSH_REACH * PUSH_REACH || to.normalize().dot(look) < 0.35) continue;
            Vec3 shove = to.normalize().scale(BossDamage.isBoss(e) ? PUSH_STRENGTH * 0.2 : PUSH_STRENGTH).add(0, 0.35, 0);
            e.push(shove.x, shove.y, shove.z);
            e.hurtMarked = true;
            e.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), PowerTargets.vsBoss(p, e, Power.BLOOD_PUSH, WeaponDamage.magic(PUSH_DAMAGE)));
            n++;
        }
        level.playSound(null, p.blockPosition(), AllSounds.apocalypse("blood.push"), SoundSource.PLAYERS, 1.2f, 1f);
        level.sendParticles(AllParticles.BLOOD_MOTE.get(), p.getX() + look.x * 2, p.getEyeY(), p.getZ() + look.z * 2, 30, 1.2, 0.6, 1.2, 0.05);
        return new ActivePowers.Result(null, p.getEyePosition().add(look.scale(3)), n > 0 ? 10 : 6);
    }

    static @Nullable ActivePowers.Result exorcism(ServerPlayer p, @Nullable LivingEntity t) {
        if (t == null || !Kin.isDemon(t) || !PowerTargets.foe(p, t, false)) return null;
        ServerLevel level = p.serverLevel();
        boolean done = t instanceof SpellHooks.Expellable x && x.onExpelled(p, SpellHooks.Expellable.EXORCISE);
        if (!done) done = t instanceof SpellHooks.Exorcisable x && x.onExorcised(1f);
        if (!done) {
            boolean common = !BossDamage.isBoss(t) && !(t instanceof net.minecraft.world.entity.player.Player) && !t.getType().is(
                    org.papiricoh.supernaturalcraft.registry.AllTags.Entities.COLT_IMMUNE);
            float dmg = common ? t.getMaxHealth() * 4 : WeaponDamage.magic(EXORCISM_WOUND);
            t.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), PowerTargets.vsBoss(p, t, Power.BLOOD_EXORCISM, dmg));
        }
        level.sendParticles(org.papiricoh.supernaturalcraft.registry.AllParticles.DEMON_SMOKE.get(), t.getX(), t.getEyeY(), t.getZ(), 40, 0.3, 0.6, 0.3, 0.08);
        level.playSound(null, t.blockPosition(), AllSounds.apocalypse("blood.exorcism"), SoundSource.PLAYERS, 1.2f, 1f);
        return new ActivePowers.Result(t, t.getEyePosition(), 20);
    }

    static @Nullable ActivePowers.Result smite(ServerPlayer p, @Nullable LivingEntity t) {
        if (t == null || !PowerTargets.foe(p, t, false)) return null;
        ServerLevel level = p.serverLevel();
        float dmg;
        if (Kin.isDemon(t)) {
            boolean common = !BossDamage.isBoss(t) && !(t instanceof net.minecraft.world.entity.player.Player)
                    && !t.getType().is(org.papiricoh.supernaturalcraft.registry.AllTags.Entities.COLT_IMMUNE);
            dmg = common ? t.getMaxHealth() * 4 : WeaponDamage.magic(SMITE_DEMON);
        } else {
            dmg = WeaponDamage.magic(SMITE_OTHER);
        }
        float hits = org.papiricoh.supernaturalcraft.allegiance.power.PowerBossDamage.hits(Power.BLOOD_SMITE) * (greater(t) ? BloodRules.GREATER_DEMON_BONUS : 1f);
        float blow = PowerTargets.vsBoss(p, t, Power.BLOOD_SMITE, hits, dmg);
        if (greater(t) && !BossDamage.isBoss(t)) blow *= BloodRules.GREATER_DEMON_BONUS;
        t.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), blow);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getEyeY(), t.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
        level.sendParticles(AllParticles.BLOOD_MOTE.get(), t.getX(), t.getEyeY(), t.getZ(), 30, 0.3, 0.5, 0.3, 0.06);
        level.playSound(null, t.blockPosition(), AllSounds.apocalypse("blood.smite"), SoundSource.PLAYERS, 1.5f, 0.8f);
        return new ActivePowers.Result(t, t.getEyePosition(), 20);
    }

    /** The demons Sam's raised hand was made for: Alastair, Lilith and Ruby. */
    static boolean greater(LivingEntity t) {
        var root = BossDamage.root(t);
        var type = root.getType();
        return type == AllEntities.ALASTAIR.get() || type == AllEntities.LILITH.get() || type == AllEntities.RUBY.get();
    }
}
