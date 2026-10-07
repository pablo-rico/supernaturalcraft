package org.papiricoh.supernaturalcraft.reward.colt;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * Where a round goes and what it does there. A boss (or any part of one) takes exact damage that
 * skips its per-hit cap but never a phase threshold; a demon or a boss's summon is executed
 * outright; anything else takes a heavy wound.
 */
public final class ColtShot {

    public enum Outcome { MISS, BLOCK, EXECUTED, BOSS, OTHER, DEFLECTED }

    private ColtShot() {
    }

    /** The first block or pickable entity along the look, up to {@code range}. */
    public static HitResult trace(Player shooter, double range) {
        Vec3 from = shooter.getEyePosition(), dir = shooter.getLookAngle(), to = from.add(dir.scale(range));
        BlockHitResult block = shooter.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(shooter, from, end,
                shooter.getBoundingBox().expandTowards(dir.scale(range)).inflate(1),
                e -> e.isPickable() && !e.isSpectator() && e != shooter && BossDamage.root(e) != shooter, range * range);
        return hit != null ? hit : block;
    }

    /** One round into {@code target}. */
    public static Outcome strike(ServerLevel level, @Nullable Entity shooter, Entity target) {
        var colt = AllDamageTypes.source(level, AllDamageTypes.COLT, shooter);
        Entity root = BossDamage.root(target);
        // Bosses first: tagging one into colt_executes must never let a single round kill it.
        if (BossDamage.isBoss(target)) {
            // A melee blow a moment ago would otherwise eat into the shot through the i-frames.
            if (root instanceof LivingEntity l) l.invulnerableTime = 0;
            return target.hurt(colt, SNConfig.COLT_BOSS_DAMAGE.get().floatValue()) ? Outcome.BOSS : Outcome.DEFLECTED;
        }
        if (target.getType().is(AllTags.Entities.COLT_EXECUTES)) {
            if (target instanceof LivingEntity l) l.invulnerableTime = 0;
            target.hurt(colt, Float.MAX_VALUE);
            // Nothing it can't kill: if something turned the blow aside, it dies anyway.
            if (target.isAlive() && !target.isRemoved()) target.kill();
            return Outcome.EXECUTED;
        }
        return target.hurt(colt, SNConfig.COLT_OTHER_DAMAGE.get().floatValue()) ? Outcome.OTHER : Outcome.DEFLECTED;
    }
}
