package org.papiricoh.supernaturalcraft.entity.boss.horsemen.death;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanKind;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenLayouts;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.LimboPalette;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.HorsemenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Death, the Pale Rider: older than God, thin, in a black suit, with a cane. Called up in the Overworld with the other three
 * rings, which he gives back whether he wins or loses. Four phases over a living world he lays over the ground, and every
 * hunter carries a {@link DeathClock} (hit him or kill a reaper to wind it back; at zero, limbo):
 *
 * <pre>P1 the duel (cane and scythe: reaps at range, throws crescents) → P2 the reapers (seen only when your time is
 *   nearly up) → P3 the world of the dead (the arena turns grey and back, clocks run double, he steps through shadows)
 *   → P4 on the pale horse (charges, waves of the scythe: the end is the end) → DYING</pre>
 */
public class DeathEntity extends HorsemanEntity {

    /** Ticks between the world turning grey and back again. */
    public static final int FLIP_EVERY = 600;

    private final Map<UUID, DeathClock> clocks = new LinkedHashMap<>();
    private final Map<UUID, ServerPlayer> clocked = new HashMap<>();
    private final Map<UUID, UUID> exits = new HashMap<>();
    private boolean ringsOffered, deadWorld;
    private long nextFlip;
    private @Nullable HorsemenGround flipping;

    public DeathEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 800;
    }

    @Override
    public HorsemanKind kind() {
        return HorsemanKind.DEATH;
    }

    @Override
    protected List<ArenaCell> groundPlan(int radius, long seed) {
        return HorsemenLayouts.livingWorld(radius, seed);
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return DeathAttacks.pool(phase);
    }

    @Override
    protected int arenaRadius() {
        return SNConfig.DEATH_ARENA_RADIUS.get();
    }

    @Override
    protected double healthMultiplier() {
        return SNConfig.DEATH_HEALTH_MULTIPLIER.get();
    }

    /** Summoned by the rite: the three rings were offered and come back, win or lose. */
    public void setRingsOffered(boolean offered) {
        ringsOffered = offered;
    }

    public boolean ringsOffered() {
        return ringsOffered;
    }

    public boolean deadWorld() {
        return deadWorld;
    }

    // --- the clocks ------------------------------------------------------------------------------------

    public static int clockTicks() {
        return SNConfig.DEATH_CLOCK_SECONDS.get() * 20;
    }

    public static int limboTicks() {
        return SNConfig.LIMBO_SECONDS.get() * 20;
    }

    /** Starts (or finds) {@code p}'s clock. Every challenger gets one; tests hand theirs in. */
    public DeathClock track(ServerPlayer p) {
        clocked.put(p.getUUID(), p);
        return clocks.computeIfAbsent(p.getUUID(), id -> new DeathClock(clockTicks(), limboTicks()));
    }

    public @Nullable DeathClock clock(ServerPlayer p) {
        return clocks.get(p.getUUID());
    }

    public boolean inLimbo(ServerPlayer p) {
        DeathClock c = clocks.get(p.getUUID());
        return c != null && c.inLimbo();
    }

    /** A blow on Death, or a reaper's end: back to full. */
    public void windBack(ServerPlayer p) {
        DeathClock c = clocks.get(p.getUUID());
        if (c == null || c.inLimbo()) return;
        boolean low = c.reapersSeen();
        c.reset();
        sync(p, c);
        if (low) p.displayClientMessage(Component.translatable("message.supernaturalcraft.death.clock_reset").withStyle(ChatFormatting.GRAY), true);
    }

    /** A reaper's touch takes time off {@code p}'s clock. */
    public void stealTime(ServerPlayer p, int ticks) {
        DeathClock c = clocks.get(p.getUUID());
        if (c == null) return;
        c.steal(ticks);
        sync(p, c);
    }

    /** {@code p} killed a reaper: their clock winds back. */
    public void reaperFell(ServerPlayer p) {
        windBack(p);
    }

    private void sync(ServerPlayer p, DeathClock c) {
        PacketDistributor.sendToPlayer(p, new HorsemenFxPayload(getId(), HorsemenFxPayload.CLOCK, c.remaining(), c.fullTicks(),
                new Vec3(deadWorld ? 1 : 0, 0, 0), c.limboLeft()));
    }

    private void tickClocks(ServerLevel level, ArenaController arena) {
        for (ServerPlayer p : challengers()) track(p);
        for (var it = clocks.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            ServerPlayer p = clocked.get(e.getKey());
            DeathClock c = e.getValue();
            if (p == null || p.isRemoved() || !p.isAlive() || p.level() != level) {
                if (p != null) endClock(p);
                discardExit(level, e.getKey());
                clocked.remove(e.getKey());
                it.remove();
                continue;
            }
            switch (c.tick(deadWorld)) {
                case ENTER_LIMBO -> enterLimbo(level, arena, p, c);
                case DIE -> reap(level, p);
                default -> {
                    if (c.inLimbo()) checkEscape(level, p, c);
                }
            }
            if (tickCount % 10 == 0) sync(p, c);
        }
    }

    private void enterLimbo(ServerLevel level, ArenaController arena, ServerPlayer p, DeathClock c) {
        Vec3 at = null;
        for (int i = 0; i < 12 && at == null; i++) {
            Vec3 cand = LuciferAttacks.randomArenaPoint(this, 0.85);
            if (cand.distanceTo(p.position()) >= 9) at = cand;
        }
        if (at == null) at = LuciferAttacks.floorAt(this, arena.centerVec().add(p.position().subtract(arena.centerVec()).multiply(-1, 0, -1)));
        LimboExitEntity exit = AllEntities.LIMBO_EXIT.get().create(level);
        if (exit != null) {
            exit.moveTo(at.x, at.y, at.z);
            exit.setOwner(p.getUUID(), c.limboLeft());
            level.addFreshEntity(exit);
            exits.put(p.getUUID(), exit.getUUID());
        }
        PacketDistributor.sendToPlayer(p, new HorsemenFxPayload(getId(), HorsemenFxPayload.LIMBO_ENTER, 0, 0, at, c.limboLeft()));
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.death.limbo").withStyle(ChatFormatting.GRAY), true);
        level.playSound(null, p.blockPosition(), AllSounds.DEATH_LIMBO_BELL.get(), SoundSource.HOSTILE, 1.5f, 0.8f);
    }

    private void checkEscape(ServerLevel level, ServerPlayer p, DeathClock c) {
        UUID id = exits.get(p.getUUID());
        if (id == null || !(level.getEntity(id) instanceof LimboExitEntity exit)) return;
        if (p.position().distanceTo(exit.position()) > LimboExitEntity.REACH) return;
        c.escape();
        discardExit(level, p.getUUID());
        PacketDistributor.sendToPlayer(p, new HorsemenFxPayload(getId(), HorsemenFxPayload.LIMBO_EXIT, 1, 0, p.position(), 0));
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.death.escaped").withStyle(ChatFormatting.WHITE), true);
        level.sendParticles(AllParticles.SOUL_WISP.get(), p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        sync(p, c);
    }

    /** Limbo ran out: Death takes what is his. */
    private void reap(ServerLevel level, ServerPlayer p) {
        discardExit(level, p.getUUID());
        PacketDistributor.sendToPlayer(p, new HorsemenFxPayload(getId(), HorsemenFxPayload.LIMBO_EXIT, 0, 0, p.position(), 0));
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.death.reaped").withStyle(ChatFormatting.DARK_GRAY), true);
        level.playSound(null, p.blockPosition(), AllSounds.DEATH_REAP.get(), SoundSource.HOSTILE, 2f, 0.7f);
        p.invulnerableTime = 0;
        p.hurt(AllDamageTypes.source(level, AllDamageTypes.REAPED, this), 10_000f);
    }

    private void discardExit(ServerLevel level, UUID player) {
        UUID id = exits.remove(player);
        if (id != null && level.getEntity(id) instanceof LimboExitEntity exit) exit.discard();
    }

    public @Nullable LimboExitEntity exitOf(ServerLevel level, ServerPlayer p) {
        UUID id = exits.get(p.getUUID());
        return id != null && level.getEntity(id) instanceof LimboExitEntity e ? e : null;
    }

    private void endClock(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new HorsemenFxPayload(getId(), HorsemenFxPayload.CLOCK, -1, 1, Vec3.ZERO, 0));
        PacketDistributor.sendToPlayer(p, new HorsemenFxPayload(getId(), HorsemenFxPayload.LIMBO_EXIT, 0, 0, p.position(), 0));
    }

    /** The fight is over, either way: every clock stops and every light goes out. */
    private void endClocks() {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : clocked.values()) endClock(p);
        for (UUID id : new ArrayList<>(exits.keySet())) discardExit(level, id);
        clocks.clear();
        clocked.clear();
    }

    /** Hunters in limbo are not his to fight. */
    @Override
    public @Nullable LivingEntity attackTarget() {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : challengers()) {
            if (inLimbo(p)) continue;
            double d = p.distanceToSqr(this);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    // --- the world of the dead -------------------------------------------------------------------------

    /** The arena turns grey ({@code dead}) or back to life, a few hundred blocks a tick. */
    public void flip(ServerLevel level, boolean dead) {
        HorsemenGround living = ground();
        deadWorld = dead;
        if (living != null) flipping = dead ? living.mapped(LimboPalette::toLimbo) : living.mapped(s -> s);
        fx(level, new HorsemenFxPayload(getId(), HorsemenFxPayload.WORLD_FLIP, dead ? 1 : 0, 0, position(), 60));
        level.playSound(null, blockPosition(), AllSounds.DEATH_WORLD_FLIP.get(), SoundSource.HOSTILE, 4f, dead ? 0.7f : 1.1f);
        triggerAnim("action", "world_flip");
        for (ServerPlayer p : clocked.values()) {
            DeathClock c = clocks.get(p.getUUID());
            if (c != null) sync(p, c);
        }
    }

    /** Test and preview hook: finishes the flip under way at once. */
    public void finishFlip() {
        if (flipping != null && level() instanceof ServerLevel level && arena() != null) {
            flipping.finish(level, arena());
            flipping = null;
        }
    }

    @Override
    protected void tickFight(ServerLevel level, ArenaController arena) {
        if (state() == EMERGING) return;
        tickClocks(level, arena);
        if (flipping != null && flipping.tick(level, arena)) flipping = null;
        long now = level.getGameTime();
        if (phase() >= 3 && groundLaid() && state() != TRANSITION && nextFlip > 0 && now >= nextFlip) {
            nextFlip = now + FLIP_EVERY;
            flip(level, !deadWorld);
        }
        if (deadWorld && tickCount % 5 == 0) {
            level.sendParticles(AllParticles.SOUL_WISP.get(), getRandomX(1), getY() + random.nextDouble() * 2, getRandomZ(1), 1, 0, 0.02, 0, 0);
        }
    }

    @Override
    protected void onTransitionStart(int to) {
        super.onTransitionStart(to);
        if (to == 3 && level() instanceof ServerLevel level) {
            flip(level, true);
            nextFlip = level.getGameTime() + FLIP_EVERY;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        boolean hurt = super.hurt(source, amount);
        if (hurt && !isInvulnerablePhase() && source.getEntity() instanceof ServerPlayer p) windBack(p);
        return hurt;
    }

    // --- the end, either way ---------------------------------------------------------------------------

    private void giveBackRings(ServerLevel level, Vec3 at) {
        if (!ringsOffered) return;
        ringsOffered = false;
        for (var ring : List.of(AllItems.RING_OF_WAR, AllItems.RING_OF_FAMINE, AllItems.RING_OF_PESTILENCE)) {
            spawnItem(level, at, new ItemStack(ring.get()));
        }
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(at) < 64 * 64) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.death.rings_returned").withStyle(ChatFormatting.GRAY), false);
            }
        }
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        super.onDefeated(level, arena);
        giveBackRings(level, position().add(0, 2.1, 0));
        endClocks();
    }

    /** He lost interest, or won: the three rings come back all the same. */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
        super.leaveBehind(level, at);
        giveBackRings(level, at);
        endClocks();
    }

    @Override
    public void remove(RemovalReason reason) {
        endClocks();
        super.remove(reason);
    }

    // --- attacks ---------------------------------------------------------------------------------------

    /** In the world of the dead he steps through the shadows rather than walking. */
    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (deadWorld && (distanceToSqr(target) > 10 * 10 || noSightTicks > 40)) {
            noSightTicks = 0;
            return DeathAttacks.ShadowStep::new;
        }
        return super.forcedAttack(target);
    }

    @Override
    protected SoundEvent ambientSound() {
        return AllSounds.DEATH_AMBIENT.get();
    }

    @Override
    protected SoundEvent hurtSound() {
        return AllSounds.DEATH_HURT.get();
    }

    @Override
    protected SoundEvent deathSound() {
        return AllSounds.DEATH_DEATH.get();
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.DEATH_REAP.get();
    }

    // --- persistence -----------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("RingsOffered", ringsOffered);
        tag.putBoolean("DeadWorld", deadWorld);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ringsOffered = tag.getBoolean("RingsOffered");
        deadWorld = tag.getBoolean("DeadWorld");
        if (phase() >= 3) nextFlip = 1;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && nextFlip == 1) nextFlip = level().getGameTime() + FLIP_EVERY;
    }

    /** Particles at {@code at}: the reaper's scythe passing. */
    static void reapFx(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(AllParticles.SOUL_WISP.get(), at.x, at.y + 1, at.z, 4, 0.3, 0.4, 0.3, 0.02);
    }
}
