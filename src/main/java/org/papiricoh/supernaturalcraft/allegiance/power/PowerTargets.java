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
