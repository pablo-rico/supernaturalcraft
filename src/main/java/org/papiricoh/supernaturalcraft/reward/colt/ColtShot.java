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
import org.papiricoh.supernaturalcraft.balance.Balance;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.entity.boss.CappedBoss;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorTargetEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * Where a round goes and what it does there (v0.15). A great enemy (or any part of one, or the Author's pages) takes
 * exact damage ({@link #bossDamage}): 5% of its true health (the Author: his 1.5% hard cap), no multiplier, no soft cap,
 * never past a phase threshold. Archangels
 * and above that are not the mod's great enemies ({@code #colt_immune}, other mods' bosses) take their hard cap, at least
 * {@code colt.otherDamage}. A player takes {@code colt.otherDamage} (unless {@code colt.executePlayers}); every other
 * living thing, from any mod, dies to one round.
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

    /** What one round takes from {@code boss}: {@code colt.bossHealthShare} of its true health (the Author: his hard cap). */
    public static float bossDamage(CappedBoss boss) {
        return boss.exactCap();
    }

    /** What one round takes from a {@code #colt_immune} creature with {@code maxHealth}. */
    public static float immuneDamage(float maxHealth) {
        return Math.max(SNConfig.COLT_OTHER_DAMAGE.get().floatValue(), Balance.hardCap(maxHealth));
    }

    /** One round into {@code target}. */
    public static Outcome strike(ServerLevel level, @Nullable Entity shooter, Entity target) {
        var colt = AllDamageTypes.source(level, AllDamageTypes.COLT, shooter);
        Entity root = BossDamage.root(target);
        // Great enemies first: no tag may ever let a single round kill one.
        if (root instanceof CappedBoss boss) {
            // A melee blow a moment ago would otherwise eat into the shot through the i-frames.
            if (root instanceof LivingEntity l) l.invulnerableTime = 0;
            return target.hurt(colt, bossDamage(boss)) ? Outcome.BOSS : Outcome.DEFLECTED;
        }
        // The Author's pages and weak points are his: his hard cap tears one.
        if (target instanceof AuthorTargetEntity t) {
            ChuckEntity owner = t.owner();
            float dmg = owner != null ? bossDamage(owner) : SNConfig.COLT_OTHER_DAMAGE.get().floatValue();
            return target.hurt(colt, dmg) ? Outcome.BOSS : Outcome.DEFLECTED;
        }
        if (!(target instanceof LivingEntity living)) {
            return target.hurt(colt, SNConfig.COLT_OTHER_DAMAGE.get().floatValue()) ? Outcome.OTHER : Outcome.DEFLECTED;
        }
        // Other mods' bosses (also in #bosses through #c:bosses) and archangels and above: hurt, never executed.
        if (BossDamage.isBoss(target) || target.getType().is(AllTags.Entities.COLT_IMMUNE)) {
            living.invulnerableTime = 0;
            return target.hurt(colt, immuneDamage(living.getMaxHealth())) ? Outcome.BOSS : Outcome.DEFLECTED;
        }
        if (target instanceof Player && !SNConfig.COLT_EXECUTE_PLAYERS.get()) {
            return target.hurt(colt, SNConfig.COLT_OTHER_DAMAGE.get().floatValue()) ? Outcome.OTHER : Outcome.DEFLECTED;
        }
        return execute(colt, living) ? Outcome.EXECUTED : Outcome.DEFLECTED;
    }

    /** Kills {@code target} outright: nothing it can't kill, short of a player it may not hurt at all (creative). */
    private static boolean execute(net.minecraft.world.damagesource.DamageSource colt, LivingEntity target) {
        if (target instanceof Player p && (p.isCreative() || p.isSpectator() || p.isInvulnerableTo(colt))) return false;
        target.invulnerableTime = 0;
        target.hurt(colt, Float.MAX_VALUE);
        // If something turned the blow aside (a totem, a shield, a mod), it dies anyway.
        if (target.isAlive() && !target.isRemoved()) target.kill();
        return true;
    }
}
