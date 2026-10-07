package org.papiricoh.supernaturalcraft.entity.boss.horsemen.war;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanKind;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenLayouts;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.HorsemenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * War, the Red Rider. Three phases on a battlefield he lays over the arena:
 *
 * <pre>P1 the duel (sword combos; a shield raised just as a blow lands staggers him: ×1.5 for three seconds)
 *   → P2 fury (four standards; {@link WarFury}; his illusion makes friends look like demons and walks mirages
 *   among them: hit an innocent or a friend and it comes back on you) → P3 on the red horse (charges, sweeps,
 *   burning trenches) → DYING</pre>
 */
public class WarEntity extends HorsemanEntity {

    public static final int STAGGER_TICKS = 60, ILLUSION_TICKS = 200, STANDARDS = 4;
    public static final float STAGGER_VULNERABILITY = 1.5f;
    private static final ResourceLocation FURY_SPEED = SupernaturalCraft.asResource("war_fury");

    private final WarFury fury = new WarFury();
    private final List<UUID> standards = new ArrayList<>();
    private final Map<UUID, Long> marked = new HashMap<>();
    private boolean standardsRaised;
    private long staggeredUntil;

    public WarEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public HorsemanKind kind() {
        return HorsemanKind.WAR;
    }

    @Override
    protected List<ArenaCell> groundPlan(int radius, long seed) {
        return HorsemenLayouts.battlefield(radius, seed);
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return WarAttacks.pool(phase);
    }

    public WarFury fury() {
        return fury;
    }

    // --- parry -----------------------------------------------------------------------------------------

    public boolean isStaggered() {
        return level().getGameTime() < staggeredUntil;
    }

    /** A blade turned aside at the last moment: he reels, open, for three seconds. */
    public void parried(ServerPlayer by) {
        staggeredUntil = level().getGameTime() + STAGGER_TICKS;
        scheduler().cancel();
        scheduler().delay(STAGGER_TICKS);
        triggerAnim("action", "parried");
        level().playSound(null, blockPosition(), AllSounds.WAR_PARRY.get(), SoundSource.HOSTILE, 2.5f, 1.0f);
        by.displayClientMessage(Component.translatable("message.supernaturalcraft.war.parry").withStyle(ChatFormatting.GOLD), true);
        if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.5, getZ(), 30, 0.4, 0.5, 0.4, 0.3);
    }

    @Override
    protected float vulnerability(DamageSource source) {
        return super.vulnerability(source) * (isStaggered() ? STAGGER_VULNERABILITY : 1f);
    }

    @Override
    public float attackDamageMultiplier() {
        return super.attackDamageMultiplier() * fury.damageMultiplier();
    }

    // --- standards -------------------------------------------------------------------------------------

    public void raiseStandards(ServerLevel level, ArenaController arena) {
        standardsRaised = true;
        BlockPos c = arena.center();
        for (int[] spot : HorsemenLayouts.standardSpots(arena.radius(), groundSeed(arena))) {
            BlockPos floor = ArenaTerrain.surface(level, arena, c.getX() + spot[0], c.getZ() + spot[1]);
            WarStandardEntity standard = AllEntities.WAR_STANDARD.get().create(level);
            if (standard == null) continue;
            double y = floor != null ? floor.getY() + 1 : c.getY() + 1;
            standard.moveTo(c.getX() + spot[0] + 0.5, y, c.getZ() + spot[1] + 0.5, random.nextFloat() * 360, 0);
            standard.setOwner(getUUID());
            standard.finalizeSpawn(level, level.getCurrentDifficultyAt(standard.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            level.addFreshEntity(standard);
            standards.add(standard.getUUID());
            level.sendParticles(ParticleTypes.LARGE_SMOKE, standard.getX(), y + 1, standard.getZ(), 12, 0.3, 1, 0.3, 0.02);
        }
        level.playSound(null, blockPosition(), AllSounds.WAR_RAGE.get(), SoundSource.HOSTILE, 3f, 0.9f);
    }

    public int standing(ServerLevel level) {
        standards.removeIf(id -> !(level.getEntity(id) instanceof WarStandardEntity s) || !s.isAlive());
        return standards.size();
    }

    public List<UUID> standards() {
        return standards;
    }

    /** A hunter broke one of his standards. */
    public void standardBroken(WarStandardEntity standard) {
        standards.remove(standard.getUUID());
        fury.standardBroken();
        if (level() instanceof ServerLevel level) {
            for (ServerPlayer p : challengers()) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.war.standard").withStyle(ChatFormatting.GOLD), true);
            }
            level.playSound(null, standard.blockPosition(), AllSounds.WAR_RAGE.get(), SoundSource.HOSTILE, 1.5f, 1.4f);
        }
    }

    // --- illusion --------------------------------------------------------------------------------------

    /** Marks {@code p}: for {@code ticks} everyone else looks like a demon to them, and striking a friend hurts them instead. */
    public void mark(ServerPlayer p, int ticks) {
        marked.put(p.getUUID(), level().getGameTime() + ticks);
        PacketDistributor.sendToPlayer(p, new HorsemenFxPayload(getId(), HorsemenFxPayload.ILLUSION, 0, 0, position(), ticks));
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.war.marked").withStyle(ChatFormatting.DARK_RED), true);
    }

    public boolean isMarked(Player p) {
        Long until = marked.get(p.getUUID());
        return until != null && until > level().getGameTime();
    }

    // --- ticking ---------------------------------------------------------------------------------------

    @Override
    protected void tickFight(ServerLevel level, ArenaController arena) {
        if (phase() >= 2 && !standardsRaised && state() != TRANSITION) raiseStandards(level, arena);
        if (tickCount % 20 == 0) {
            if (phase() >= 2) fury.tickSecond(standing(level));
            marked.values().removeIf(t -> t <= level.getGameTime());
            AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(FURY_SPEED);
                if (fury.value() > 0) speed.addTransientModifier(new AttributeModifier(FURY_SPEED, fury.speedMultiplier() - 1,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            updateBossBar();
        }
        if (isStaggered() && tickCount % 4 == 0) level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 2.1, getZ(), 2, 0.3, 0.1, 0.3, 0.05);
        if (fury.share() > 0.5f && tickCount % 3 == 0) {
            level.sendParticles(ParticleTypes.FLAME, getRandomX(0.6), getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.6), 1, 0, 0.02, 0, 0.01);
        }
    }

    @Override
    protected Component bossBarName(int phase) {
        Component base = super.bossBarName(phase);
        if (phase < 2 || fury.value() < 1) return base;
        return Component.empty().append(base).append(Component.literal("  ").append(Component.translatable("hud.supernaturalcraft.war.fury"))
                .append(" " + Math.round(fury.share() * 100) + "%").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        float before = trueHealth();
        boolean hurt = super.hurt(source, amount);
        float dealt = before - trueHealth();
        if (hurt && dealt > 0 && phase() >= 2) fury.onHurt(dealt);
        return hurt;
    }

    @Override
    protected void onTransitionStart(int to) {
        super.onTransitionStart(to);
        if (to == 2) marked.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level && reason.shouldDestroy()) {
            for (UUID id : standards) if (level.getEntity(id) instanceof WarStandardEntity s) s.discard();
        }
        super.remove(reason);
    }

    @Override
    protected SoundEvent ambientSound() {
        return AllSounds.WAR_AMBIENT.get();
    }

    @Override
    protected SoundEvent hurtSound() {
        return AllSounds.WAR_HURT.get();
    }

    @Override
    protected SoundEvent deathSound() {
        return AllSounds.WAR_DEATH.get();
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.WAR_RAGE.get();
    }

    /** Where War stands in the arena's centre column for his trench fires. */
    List<BlockPos> trenchFloor() {
        return ground() == null ? List.of() : ground().positionsOf("minecraft:spruce_slab");
    }

    Vec3 centre() {
        ArenaController a = arena();
        return a != null ? a.centerVec() : position();
    }

    // --- persistence -----------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Fury", fury.value());
        tag.putBoolean("StandardsRaised", standardsRaised);
        ListTag list = new ListTag();
        for (UUID id : standards) list.add(NbtUtils.createUUID(id));
        tag.put("Standards", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fury.set(tag.getFloat("Fury"));
        standardsRaised = tag.getBoolean("StandardsRaised");
        standards.clear();
        for (Tag t : tag.getList("Standards", Tag.TAG_INT_ARRAY)) standards.add(NbtUtils.loadUUID(t));
    }
}
