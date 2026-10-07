package org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanKind;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenLayouts;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pestilence, the Pale-Green Rider: sickly, coughing, sorry for nothing. Three phases in a toxic swamp; everything he
 * does gives you his stacking {@link PlagueEffect}, and antidote vials turn up in the swamp now and then.
 *
 * <pre>P1 toxic clouds that grow across the swamp → P2 the flies (swarms that only fire disperses) → P3 on the sickly
 *   horse (a trail of plague behind him, coughs that spray cones of it) → DYING</pre>
 */
public class PestilenceEntity extends HorsemanEntity {

    /** Ticks between antidote vials, and how many may lie in the swamp at once. */
    public static final int VIAL_EVERY = 500, MAX_VIALS = 2;
    public static final int CLOUD_LIFE = 400, TRAIL_LIFE = 120;
    public static final float CLOUD_START = 1.5f, CLOUD_MAX = 4.5f;

    /** A cloud of sickness, growing where it settled. */
    static final class Cloud {
        final Vec3 at;
        int age;

        Cloud(Vec3 at) {
            this.at = at;
        }

        float radius() {
            return CLOUD_START + (CLOUD_MAX - CLOUD_START) * Math.min(1f, age / (float) (CLOUD_LIFE / 2));
        }
    }

    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Vec3> trail = new ArrayList<>();
    private final List<Integer> trailAge = new ArrayList<>();
    private final Map<UUID, Long> dosedAt = new HashMap<>();
    private final List<UUID> vials = new ArrayList<>();
    private long nextVial;

    public PestilenceEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public HorsemanKind kind() {
        return HorsemanKind.PESTILENCE;
    }

    @Override
    protected List<ArenaCell> groundPlan(int radius, long seed) {
        return HorsemenLayouts.toxicSwamp(radius, seed);
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return PestilenceAttacks.pool(phase);
    }

    public void addCloud(Vec3 at) {
        clouds.add(new Cloud(at));
    }

    public int clouds() {
        return clouds.size();
    }

    /** Doses {@code e} with the plague, at most once a second each. */
    public void dose(LivingEntity e, int doses) {
        long now = level().getGameTime();
        Long last = dosedAt.get(e.getUUID());
        if (last != null && now - last < 20) return;
        dosedAt.put(e.getUUID(), now);
        Plague.infect(e, doses);
    }

    // --- the antidote ----------------------------------------------------------------------------------

    /** Puts an antidote vial on one of the swamp's dry spots; returns it. */
    public ItemEntity dropVial(ServerLevel level, ArenaController arena) {
        List<int[]> spots = HorsemenLayouts.vialSpots(arena.radius(), groundSeed(arena));
        int[] spot = spots.isEmpty() ? new int[]{3, 3} : spots.get(random.nextInt(spots.size()));
        BlockPos c = arena.center();
        BlockPos floor = ArenaTerrain.surface(level, arena, c.getX() + spot[0], c.getZ() + spot[1]);
        double y = floor != null ? floor.getY() + 1.1 : c.getY() + 1.1;
        ItemEntity vial = new ItemEntity(level, c.getX() + spot[0] + 0.5, y, c.getZ() + spot[1] + 0.5, new ItemStack(AllItems.ANTIDOTE_VIAL.get()));
        vial.setDeltaMovement(Vec3.ZERO);
        vial.setUnlimitedLifetime();
        level.addFreshEntity(vial);
        vials.add(vial.getUUID());
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, vial.getX(), y + 0.5, vial.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, vial.blockPosition(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5f, 1.2f);
        for (ServerPlayer p : challengers()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.pestilence.vial").withStyle(ChatFormatting.GREEN), true);
        }
        return vial;
    }

    private int vialsLying(ServerLevel level) {
        vials.removeIf(id -> !(level.getEntity(id) instanceof ItemEntity i) || !i.isAlive());
        return vials.size();
    }

    // --- ticking ---------------------------------------------------------------------------------------

    @Override
    protected void tickFight(ServerLevel level, ArenaController arena) {
        if (state() == EMERGING) return;
        long now = level.getGameTime();
        if (nextVial == 0) nextVial = now + VIAL_EVERY / 2;
        if (now >= nextVial && groundLaid()) {
            nextVial = now + VIAL_EVERY;
            if (vialsLying(level) < MAX_VIALS) dropVial(level, arena);
        }
        // The clouds grow, then thin out.
        for (Iterator<Cloud> it = clouds.iterator(); it.hasNext(); ) {
            Cloud c = it.next();
            if (++c.age > CLOUD_LIFE) {
                it.remove();
                continue;
            }
            float r = c.radius();
            if (c.age % 4 == 0) {
                level.sendParticles(AllParticles.PLAGUE_SPORE.get(), c.at.x, c.at.y + 0.6, c.at.z, (int) (r * 3), r * 0.6, 0.5, r * 0.6, 0.005);
            }
            if (c.age % 10 == 0) {
                for (ServerPlayer p : challengers()) {
                    if (p.position().distanceTo(c.at) < r && Math.abs(p.getY() - c.at.y) < 3) dose(p, 1);
                }
            }
        }
        // Mounted, he leaves a trail of plague.
        if (isMounted() && state() != TRANSITION && tickCount % 6 == 0 && getDeltaMovement().horizontalDistanceSqr() > 0.002) {
            trail.add(position());
            trailAge.add(0);
        }
        for (int i = trail.size() - 1; i >= 0; i--) {
            int age = trailAge.get(i) + 1;
            if (age > TRAIL_LIFE) {
                trail.remove(i);
                trailAge.remove(i);
                continue;
            }
            trailAge.set(i, age);
            Vec3 t = trail.get(i);
            if (age % 8 == 0) level.sendParticles(AllParticles.PLAGUE_SPORE.get(), t.x, t.y + 0.2, t.z, 2, 0.4, 0.1, 0.4, 0.002);
            if (age % 10 == 0) {
                for (ServerPlayer p : challengers()) if (p.position().distanceToSqr(t) < 1.6 * 1.6) dose(p, 1);
            }
        }
        if (tickCount % 140 == 0 && random.nextBoolean()) {
            level.playSound(null, blockPosition(), AllSounds.PESTILENCE_COUGH.get(), SoundSource.HOSTILE, 1.5f, 0.9f + random.nextFloat() * 0.2f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level && reason.shouldDestroy()) {
            for (UUID id : vials) if (level.getEntity(id) instanceof ItemEntity i) i.discard();
        }
        super.remove(reason);
    }

    @Override
    protected SoundEvent ambientSound() {
        return AllSounds.PESTILENCE_AMBIENT.get();
    }

    @Override
    protected SoundEvent hurtSound() {
        return AllSounds.PESTILENCE_HURT.get();
    }

    @Override
    protected SoundEvent deathSound() {
        return AllSounds.PESTILENCE_DEATH.get();
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.PESTILENCE_COUGH.get();
    }
}
