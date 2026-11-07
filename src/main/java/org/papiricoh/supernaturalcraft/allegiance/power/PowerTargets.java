package org.papiricoh.supernaturalcraft.allegiance.power;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Who a power strikes and who travels with it (v0.20). A foe is never the caster, an ally ({@code isAlliedTo}: the Host's
 * squad, a bound hound), a pet, the mob the caster rides, one of the caster's own side, or a player while PvP is off. An
 * area power also leaves the innocent alone: it only strikes monsters, whatever hunts the caster and the other side.
 */
public final class PowerTargets {

    private PowerTargets() {
    }

    /** Whether {@code caster}'s power may strike {@code e}; {@code area}: a power that does not choose (no villagers, no cows). */
    public static boolean foe(ServerPlayer caster, LivingEntity e, boolean area) {
        if (e == caster || !e.isAlive() || e.isSpectator()) return false;
        if (e.isAlliedTo(caster) || caster.isAlliedTo(e)) return false;
        if (e instanceof OwnableEntity pet && caster.getUUID().equals(pet.getOwnerUUID())) return false;
        if (ActivePowers.rides(caster, e) || Kin.sameSide(caster, e)) return false;
        if (e instanceof Player) return caster.server.isPvpAllowed();
        if (!area) return true;
        return e instanceof Enemy || e instanceof Mob m && m.getTarget() == caster || Kin.opposed(caster, e);
    }

    public static boolean boss(LivingEntity e) {
        return e.getType().is(AllTags.Entities.BOSSES);
    }

    /**
     * What {@code power} deals to {@code target} (v0.38): {@code mobDamage} to anything that is not a boss; against a boss (or one
     * of its parts) {@code hits} blows of the reference weapon at the boss's own tier ({@link PowerBossDamage}), whatever the
     * caster's tier. Never {@code Ascension.vsBoss}, which is for sigils.
     */
    public static float vsBoss(ServerPlayer caster, net.minecraft.world.entity.Entity target, Power power, float hits, float mobDamage) {
        if (!org.papiricoh.supernaturalcraft.entity.boss.BossDamage.isBoss(target)) return mobDamage;
        int rank = org.papiricoh.supernaturalcraft.allegiance.Allegiances.get(caster).rank();
        double scale;
        try {
            scale = org.papiricoh.supernaturalcraft.SNConfig.POWER_BOSS_DAMAGE_MULTIPLIER.get();
        } catch (IllegalStateException notLoaded) {
            scale = 1;
        }
        return (float) (PowerBossDamage.damage(hits, bossTier(caster, target), rank, power.minRank) * scale);
    }

    /** {@link #vsBoss(ServerPlayer, net.minecraft.world.entity.Entity, Power, float, float)} with the power's main blow. */
    public static float vsBoss(ServerPlayer caster, net.minecraft.world.entity.Entity target, Power power, float mobDamage) {
        return vsBoss(caster, target, power, PowerBossDamage.hits(power), mobDamage);
    }

    /** The tier of the boss behind {@code target}; a boss outside the progression is taken at the caster's own tier. */
    public static int bossTier(ServerPlayer caster, net.minecraft.world.entity.Entity target) {
        var root = org.papiricoh.supernaturalcraft.entity.boss.BossDamage.root(target);
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(root.getType());
        var boss = org.papiricoh.supernaturalcraft.SupernaturalCraft.MODID.equals(key.getNamespace())
                ? org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss.byEntity(key.getPath()) : null;
        var stats = boss == null ? null : org.papiricoh.supernaturalcraft.balance.ProgressionScale.of(boss);
        return stats != null ? stats.tier() : org.papiricoh.supernaturalcraft.weapon.ascension.Ascension.playerTier(caster);
    }

    /** The foes within {@code radius} of {@code at} (an area power), nearest first. */
    public static List<LivingEntity> foesAround(ServerPlayer caster, Vec3 at, double radius) {
        ServerLevel level = caster.serverLevel();
        List<LivingEntity> out = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                e -> e.position().distanceToSqr(at) <= radius * radius && foe(caster, e, true)));
        out.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(at)));
        return out;
    }

    /**
     * Who crosses with {@code caster} (Ascend, Descend): the caster, and the players within {@code radius} who kneel (crouch) to
     * go along and are not of the other side.
     */
    public static List<ServerPlayer> party(ServerPlayer caster, List<? extends Player> near, double radius) {
        List<ServerPlayer> out = new ArrayList<>();
        out.add(caster);
        for (Player q : near) {
            if (q == caster || !(q instanceof ServerPlayer sp) || !sp.isAlive() || sp.isSpectator()) continue;
            if (sp.distanceTo(caster) > radius || !sp.isCrouching() || Kin.opposed(caster, sp)) continue;
            out.add(sp);
        }
        return out;
    }
}
