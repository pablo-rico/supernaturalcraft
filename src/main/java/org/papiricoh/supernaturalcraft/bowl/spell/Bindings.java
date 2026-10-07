package org.papiricoh.supernaturalcraft.bowl.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.demon.DemonEntity;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.Optional;

/**
 * Creatures held by the Binding spell ({@link AllAttachments#BINDING}): they may not stray from the
 * bowl. Checked every {@link #INTERVAL} ticks: once the bowl is gone the binding drops; outside the
 * radius the creature is pulled back to the edge (ghosts too, walls or not); demons stay trapped.
 */
public final class Bindings {

    public static final int INTERVAL = 10;
    /** How long each refresh of a bound demon's TRAPPED lasts (outlasts the interval). */
    public static final int TRAP_TICKS = INTERVAL * 3;

    private Bindings() {
    }

    /** Whether the Binding spell may take {@code e}: anything alive but players and bosses. */
    public static boolean bindable(Entity e) {
        return e instanceof LivingEntity living && living.isAlive() && !(e instanceof Player) && !(e instanceof ArmorStand)
                && !isBoss(e);
    }

    public static boolean isBoss(Entity e) {
        return e.getType().is(AllTags.Entities.BOSSES) || e instanceof LuciferEntity;
    }

    /** Demons and hounds (not bosses): trapped while bound, banished by the Banishing spell. */
    public static boolean isUnclean(Entity e) {
        return !isBoss(e) && (e.getType().is(AllTags.Entities.DEMONS) || e instanceof DemonEntity || e instanceof HellhoundEntity);
    }

    public static Optional<Binding> of(Entity e) {
        return e.hasData(AllAttachments.BINDING) ? e.getData(AllAttachments.BINDING) : Optional.empty();
    }

    /** Binds {@code e} to the bowl at {@code anchor}. */
    public static void bind(LivingEntity e, BlockPos anchor, int radius) {
        e.setData(AllAttachments.BINDING, Optional.of(new Binding(anchor.immutable(), radius, e.level().dimension())));
        if (e instanceof Mob mob) mob.restrictTo(anchor, radius);
        if (isUnclean(e)) trap(e);
        if (e instanceof Mob mob && mob.getTarget() != null) mob.getNavigation().stop();
        if (e.level() instanceof ServerLevel level) chains(level, Vec3.atBottomCenterOf(anchor).add(0, 0.5, 0), e.position().add(0, e.getBbHeight() * 0.5, 0));
    }

    public static void release(LivingEntity e) {
        e.setData(AllAttachments.BINDING, Optional.empty());
        if (e instanceof Mob mob) mob.clearRestriction();
        if (e.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
        }
    }

    /** One check of a bound creature (server side). */
    public static void tick(LivingEntity e) {
        Optional<Binding> held = of(e);
        if (held.isEmpty() || !(e.level() instanceof ServerLevel level)) return;
        Binding b = held.get();
        if (!level.dimension().equals(b.dimension()) || !e.isAlive()) {
            release(e);
            return;
        }
        if (level.isLoaded(b.anchor()) && !level.getBlockState(b.anchor()).is(AllBlocks.SPELL_BOWL.get())) {
            release(e);
            return;
        }
        if (e instanceof Mob mob && !mob.hasRestriction()) mob.restrictTo(b.anchor(), b.radius());
        if (isUnclean(e)) trap(e);
        pullBack(e, b);
    }

    /** Puts {@code e} back inside its binding if it strayed. @return whether it had to be moved */
    public static boolean pullBack(LivingEntity e, Binding b) {
        Vec3 a = Vec3.atBottomCenterOf(b.anchor());
        boolean spherical = e.isNoGravity() || e instanceof GhostEntity;
        double[] back = BindingMath.pullBack(a.x, a.y, a.z, e.getX(), e.getY(), e.getZ(), b.radius(), spherical);
        if (back == null) return false;
        if (e instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.getMoveControl().setWantedPosition(a.x, a.y, a.z, 1.0);
        }
        e.setDeltaMovement(Vec3.ZERO);
        Vec3 from = e.position();
        e.teleportTo(back[0], back[1], back[2]);
        if (e.level() instanceof ServerLevel level) chains(level, from.add(0, e.getBbHeight() * 0.5, 0), a.add(0, 0.5, 0));
        return true;
    }

    private static void trap(LivingEntity e) {
        MobEffectInstance cur = e.getEffect(AllMobEffects.TRAPPED);
        if (cur == null || cur.getDuration() < TRAP_TICKS - INTERVAL) {
            e.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, TRAP_TICKS, 0, true, false, true));
        }
    }

    /** A line of sooty sparks from {@code a} to {@code b}: the binding's chain. */
    static void chains(ServerLevel level, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        int n = Math.max(2, (int) (d.length() * 2));
        for (int i = 0; i <= n; i++) {
            Vec3 p = a.add(d.scale(i / (double) n));
            level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
            if (i % 2 == 0) level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }
}
