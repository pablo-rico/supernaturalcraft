package org.papiricoh.supernaturalcraft.apocalypse.ruby;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.BossShares;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import org.papiricoh.supernaturalcraft.util.Players;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Ruby revealed (v0.40): the short fight after the last seal breaks, in Lilith's own arena (or, for a hunter who lost or fled it,
 * by night wherever she finds them: {@link RubyFight#ambush}). She is no great enemy: a demon fast with her knife, a telekinetic
 * shove (a red ring round her) and a dash through smoke (a red lane). Nothing but a demon-killing blade ({@code #demon_bane}) in
 * the hand that strikes the last blow puts her down; anything else leaves her on her feet. Beaten, every hunter who fought her
 * gets the Colt she carried (one each, {@link BossShares}).
 */
public class RubyEntity extends Monster implements GeoEntity {

    /** Ticks she gloats before the fight (untouchable), how often her tricks come, how long she waits for nobody before going. */
    public static final int INTRO_TICKS = 60, TRICK_EVERY = 90, LEAVE_TICKS = 60 * 20;
    public static final float SHOVE_RADIUS = 3.5f, SHOVE_DAMAGE = 4f, DASH_LENGTH = 10f, DASH_WIDTH = 1.4f, DASH_DAMAGE = 7f;
    public static final int SHOVE_WINDUP = 16, DASH_WINDUP = 14;
    /** Her health: a share of Lilith's real health (so the season-4 hunter's kit is enough), more for each extra hunter. */
    public static final float HEALTH_SHARE = 0.25f, PER_EXTRA = 0.5f, BASE_DAMAGE = 6f;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.supernaturalcraft.ruby"), BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);
    private @Nullable UUID arenaId;
    private int intro = INTRO_TICKS, nextTrick = TRICK_EVERY, lonely, trick, trickAt;
    private float trickYaw;
    private Vec3 trickFrom = Vec3.ZERO;

    public RubyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 120;
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Her numbers for {@code hunters} hunters (real health from Lilith's curve). */
    public void tune(int hunters) {
        float lilith = org.papiricoh.supernaturalcraft.balance.ProgressionScale.trueHealth(org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss.LILITH);
        float health = Math.min(1000f, lilith * HEALTH_SHARE * (1 + PER_EXTRA * Math.max(0, hunters - 1)));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        setHealth(health);
        float k = org.papiricoh.supernaturalcraft.balance.ProgressionScale.of(org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss.LILITH).damageMultiplier();
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BASE_DAMAGE * k);
    }

    public void bindArena(ArenaController arena) {
        arenaId = arena.id();
        arena.setBoss(getUUID());
    }

    public @Nullable ArenaController arena() {
        if (arenaId == null || !(level() instanceof ServerLevel level)) return null;
        for (ArenaController a : org.papiricoh.supernaturalcraft.arena.ArenaSavedData.get(level).all()) if (a.id().equals(arenaId)) return a;
        return null;
    }

    public boolean introducing() {
        return intro > 0;
    }

    // --- damage ----------------------------------------------------------------------------------------------------------------

    /** Whether {@code source} is a blow that can end her: a demon-killing blade in the striking hand (or a command's). */
    public static boolean bane(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        return source.getDirectEntity() instanceof LivingEntity hitter && source.getEntity() == hitter
                && hitter.getMainHandItem().is(AllTags.Items.DEMON_BANE);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (introducing() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (!bane(source) && amount >= getHealth()) {
            amount = Math.max(0, getHealth() - 1);
            if (amount <= 0) {
                if (source.getEntity() instanceof ServerPlayer p && random.nextInt(3) == 0) {
                    p.displayClientMessage(Component.translatable("message.supernaturalcraft.ruby.needs_bane").withStyle(ChatFormatting.GRAY), true);
                }
                playSound(AllSounds.apocalypse("ruby.laugh"), 1f, 1.2f);
                return false;
            }
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt) triggerAnim("action", "hurt");
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.FLASH, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1, getZ(), 60, 0.4, 0.8, 0.4, 0.06);
        ItemStack colt = new ItemStack(AllItems.THE_COLT.get());
        colt.set(AllDataComponents.COLT_AMMO.get(), org.papiricoh.supernaturalcraft.reward.ColtItem.CAPACITY);
        if (!BossShares.share(level, this, colt)) spawnAtLocation(colt);
        for (UUID id : BossShares.fighters(level, this, 48)) {
            if (Players.byUuid(level, id) instanceof ServerPlayer p) {
                Rubys.advance(p, RubyRules.Step.DONE);
                ChorusRewards.award(p, "main/sympathy_for_the_devil");
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.ruby.dead").withStyle(ChatFormatting.GOLD), false);
            }
        }
        ArenaController arena = arena();
        if (arena != null) arena.beginRestore(true);
        bar.removeAllPlayers();
    }

    /** She goes in smoke (nobody left to fight): the hunters will find her again. */
    public void flee() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1, getZ(), 50, 0.4, 0.8, 0.4, 0.05);
            level.playSound(null, blockPosition(), AllSounds.apocalypse("ruby.smoke_out"), SoundSource.HOSTILE, 1.5f, 1f);
        }
        ArenaController arena = arena();
        if (arena != null) arena.beginRestore(false);
        bar.removeAllPlayers();
        discard();
    }

    // --- per tick -------------------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        bar.setProgress(getHealth() / getMaxHealth());
        for (ServerPlayer p : level.players()) {
            boolean near = p.distanceToSqr(this) < 40 * 40;
            if (near && !bar.getPlayers().contains(p)) bar.addPlayer(p);
            else if (!near && bar.getPlayers().contains(p)) bar.removePlayer(p);
        }
        if (intro > 0) {
            if (intro == INTRO_TICKS) {
                triggerAnim("action", "laugh");
                level.playSound(null, blockPosition(), AllSounds.apocalypse("ruby.laugh"), SoundSource.HOSTILE, 2f, 1f);
                for (ServerPlayer p : bar.getPlayers()) {
                    p.displayClientMessage(Component.translatable("message.supernaturalcraft.ruby.reveal").withStyle(ChatFormatting.DARK_RED), false);
                    org.papiricoh.supernaturalcraft.network.Send.toPlayer(p, new org.papiricoh.supernaturalcraft.network.RubyFxPayload(getId(),
                            org.papiricoh.supernaturalcraft.network.RubyFxPayload.TITLE, 0, 0, position(), 70));
                }
            }
            intro--;
            getNavigation().stop();
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            if (++lonely > LEAVE_TICKS) flee();
            return;
        }
        lonely = 0;
        if (trick != 0) {
            if (tickCount >= trickAt) landTrick(level, target);
            return;
        }
        if (--nextTrick <= 0) {
            nextTrick = TRICK_EVERY + random.nextInt(30);
            startTrick(level, target);
        }
    }

    private void startTrick(ServerLevel level, LivingEntity target) {
        if (distanceToSqr(target) < 16 && random.nextBoolean()) {
            trick = 1;
            trickAt = tickCount + SHOVE_WINDUP;
            TelegraphMarker.ring(level, position(), SHOVE_RADIUS, TelegraphMarker.RED, SHOVE_WINDUP + 4);
        } else {
            trick = 2;
            trickAt = tickCount + DASH_WINDUP;
            trickFrom = position();
            Vec3 to = target.position().subtract(position());
            trickYaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
            TelegraphMarker.line(level, trickFrom, trickYaw, DASH_WIDTH, DASH_LENGTH, TelegraphMarker.RED, DASH_WINDUP + 4);
        }
        getNavigation().stop();
    }

    private void landTrick(ServerLevel level, LivingEntity target) {
        int which = trick;
        trick = 0;
        if (which == 1) {
            triggerAnim("action", "shove");
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SHOVE_RADIUS), x -> x != this && x instanceof Player)) {
                if (e.distanceToSqr(this) > SHOVE_RADIUS * SHOVE_RADIUS) continue;
                e.hurt(damageSources().mobAttack(this), SHOVE_DAMAGE);
                Vec3 out = e.position().subtract(position()).multiply(1, 0, 1).normalize();
                e.push(out.x * 1.4, 0.45, out.z * 1.4);
                e.hurtMarked = true;
            }
            level.playSound(null, blockPosition(), AllSounds.apocalypse("ruby.shove"), SoundSource.HOSTILE, 1.5f, 1f);
            return;
        }
        triggerAnim("action", "smoke_dash");
        Vec3 dir = new Vec3(-Math.sin(Math.toRadians(trickYaw)), 0, Math.cos(Math.toRadians(trickYaw)));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(trickFrom, trickFrom).inflate(DASH_LENGTH + 1, 3, DASH_LENGTH + 1),
                x -> x != this && x instanceof Player)) {
            Vec3 to = e.position().subtract(trickFrom).multiply(1, 0, 1);
            double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
            if (along >= 0 && along <= DASH_LENGTH && across <= DASH_WIDTH / 2) e.hurt(damageSources().mobAttack(this), DASH_DAMAGE);
        }
        Vec3 end = trickFrom.add(dir.scale(DASH_LENGTH * 0.9));
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.04);
        if (level.noCollision(this, getBoundingBox().move(end.subtract(position())))) teleportTo(end.x, end.y, end.z);
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.04);
        level.playSound(null, blockPosition(), AllSounds.apocalypse("ruby.slash"), SoundSource.HOSTILE, 1.5f, 1.1f);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("action", random.nextInt(3) == 0 ? "kick" : random.nextBoolean() ? "slash" : "lunge");
            playSound(AllSounds.apocalypse("ruby.slash"), 1f, 1f);
        }
        return hit;
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.removeAllPlayers();
        super.remove(reason);
    }

    /** She is held for her whole death clip (the demon's flare), not vanilla's 20 ticks. */
    @Override
    protected void tickDeath() {
        if (++deathTime >= RubyAssets.CLIP_TICKS.getOrDefault("death", 80) && !level().isClientSide && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.apocalypse("ruby.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.apocalypse("ruby.death");
    }

    // --- animation and saving -------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = "animation." + RubyAssets.RIG + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), run = RawAnimation.begin().thenLoop(pre + "run");
        controllers.add(new AnimationController<>(this, "base", 4, state -> {
            if (isDeadOrDying()) return PlayState.STOP;
            return state.setAndContinue(state.isMoving() ? run : idle);
        }));
        AnimationController<RubyEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : RubyAssets.TRIGGERED) {
            action.triggerableAnim(clip, clip.equals("death") ? RawAnimation.begin().thenPlayAndHold(pre + clip) : RawAnimation.begin().thenPlay(pre + clip));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (arenaId != null) tag.putUUID("Arena", arenaId);
        tag.putInt("Intro", intro);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        arenaId = tag.hasUUID("Arena") ? tag.getUUID("Arena") : null;
        intro = tag.getInt("Intro");
        bar.setName(getDisplayName());
    }
}
