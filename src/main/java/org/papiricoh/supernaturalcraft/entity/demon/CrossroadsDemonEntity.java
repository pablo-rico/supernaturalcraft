package org.papiricoh.supernaturalcraft.entity.demon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms;
import org.papiricoh.supernaturalcraft.crossroads.Deals;
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animation.AnimationController;

import java.util.EnumSet;
import java.util.UUID;

/**
 * The crossroads demon: a sharp suit, red eyes and a smile. Answers a bowl at night, and makes
 * deals.
 *
 * <p><b>Neutral</b> (summoned): it waits on its summoner for {@link DealTerms#NEUTRAL_LIFETIME}
 * ticks, watching and drifting close, never attacking. Right-click to hear its offer. Any blow
 * and it leaves in smoke; so it does if the summoner goes away, or once the deal is sealed (with
 * a kiss).
 *
 * <p><b>Hostile</b> (a deal being broken): it walks again to keep its debtor's soul, and fights.
 * Killing it before the debt falls due sets the debtor free; if it flees its vessel it comes back
 * on a later night.
 *
 * <p><b>Wild</b> (v0.18): called by a crossroads box buried at a natural crossroads, no bowl needed. Neutral like any summoned
 * demon, but its offer is a wild bargain: better wishes, a worse price ({@code Deals.offer(player, true)}).
 */
public class CrossroadsDemonEntity extends DemonEntity {

    private static final EntityDataAccessor<Boolean> HOSTILE =
            SynchedEntityData.defineId(CrossroadsDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WILD =
            SynchedEntityData.defineId(CrossroadsDemonEntity.class, EntityDataSerializers.BOOLEAN);
    /** How long it lingers after sealing a deal (the kiss), before leaving. */
    public static final int SEAL_TICKS = 30;
    private static final double SUMMONER_RANGE = 32;
    private static final int AWAY_TICKS = 100;

    private @Nullable UUID summonerId;
    private @Nullable Player summonerRef;
    private @Nullable UUID debtorId;
    private @Nullable Player debtorRef;
    private int life = DealTerms.NEUTRAL_LIFETIME;
    private int leaveIn = -1;
    private int away;

    public CrossroadsDemonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    /** Answers a bowl: a neutral demon beside it, bound to whoever called. */
    public static @Nullable CrossroadsDemonEntity summon(ServerLevel level, Vec3 at, Player summoner) {
        CrossroadsDemonEntity demon = AllEntities.CROSSROADS_DEMON.get().create(level);
        if (demon == null) return null;
        demon.summonerId = summoner.getUUID();
        demon.summonerRef = summoner;
        demon.arrive(level, at, summoner);
        return demon;
    }

    /** Answers a box buried at a natural crossroads (v0.18): a neutral demon at {@code centre} whose offer is a wild bargain. */
    public static @Nullable CrossroadsDemonEntity summonWild(ServerLevel level, Vec3 centre, @Nullable Player summoner) {
        CrossroadsDemonEntity demon = AllEntities.CROSSROADS_DEMON.get().create(level);
        if (demon == null) return null;
        demon.entityData.set(WILD, true);
        if (summoner != null) {
            demon.summonerId = summoner.getUUID();
            demon.summonerRef = summoner;
        }
        Player facing = summoner != null ? summoner : level.getNearestPlayer(centre.x, centre.y, centre.z, 32, false);
        demon.arrive(level, centre, facing);
        level.playSound(null, centre.x, centre.y, centre.z, AllSounds.heaven("crossroads.wild_arrive"), SoundSource.HOSTILE, 1.4f, 1f);
        return demon;
    }

    /** A deal is being broken: the demon walks again, hostile, after {@code debtor}. */
    public static @Nullable CrossroadsDemonEntity hunt(ServerLevel level, Vec3 at, Player debtor) {
        CrossroadsDemonEntity demon = AllEntities.CROSSROADS_DEMON.get().create(level);
        if (demon == null) return null;
        demon.entityData.set(HOSTILE, true);
        demon.debtorId = debtor.getUUID();
        demon.debtorRef = debtor;
        demon.arrive(level, at, debtor);
        demon.setTarget(debtor);
        return demon;
    }

    private void arrive(ServerLevel level, Vec3 at, @Nullable Player facing) {
        double dx = facing != null ? facing.getX() - at.x : 0, dz = facing != null ? facing.getZ() - at.z : 1;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        moveTo(at.x, at.y, at.z, yaw, 0);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        setPersistenceRequired();
        level.addFreshEntity(this);
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), at.x, at.y + 1, at.z, 50, 0.3, 0.9, 0.3, 0.04);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.5, at.z, 20, 0.4, 0.6, 0.4, 0.02);
        level.playSound(null, at.x, at.y, at.z, AllSounds.CROSSROADS_ARRIVE.get(), SoundSource.HOSTILE, 1.2f, 1f);
    }

    @Override
    protected String animPrefix() {
        return "crossroads_demon";
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOSTILE, false);
        builder.define(WILD, false);
    }

    public boolean isHostile() {
        return entityData.get(HOSTILE);
    }

    /** Called at a natural crossroads (v0.18): it offers a wild bargain. */
    public boolean isWild() {
        return entityData.get(WILD);
    }

    public @Nullable UUID summonerId() {
        return summonerId;
    }

    public @Nullable UUID debtorId() {
        return debtorId;
    }

    public boolean isSummoner(Player player) {
        return summonerId != null && summonerId.equals(player.getUUID());
    }

    /** Whether it is on its way out (sealed, offended or bored). */
    public boolean isLeaving() {
        return leaveIn >= 0 || isSmoking();
    }

    /** The summoner, if in this level. */
    public @Nullable Player summoner() {
        return resolve(summonerId, summonerRef, false);
    }

    /** The debtor, wherever they are on the server. */
    public @Nullable Player debtor() {
        return resolve(debtorId, debtorRef, true);
    }

    private @Nullable Player resolve(@Nullable UUID id, @Nullable Player ref, boolean anyLevel) {
        if (id == null) return null;
        if (ref != null && !ref.isRemoved() && (anyLevel || ref.level() == level())) return ref;
        Player p = level().getPlayerByUUID(id);
        if (p == null && anyLevel && level().getServer() != null) p = level().getServer().getPlayerList().getPlayer(id);
        return p;
    }

    // --- goals ----------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            public boolean canUse() {
                return isHostile() && super.canUse();
            }
        });
        goalSelector.addGoal(4, new AttendSummoner());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return isHostile() && super.canUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return isHostile() && super.canUse();
            }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return isHostile() && super.canUse();
            }
        });
    }

    /** Neutral: drifts to within a few steps of its summoner and keeps its eyes on them. */
    private class AttendSummoner extends Goal {
        AttendSummoner() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player s = summoner();
            return !isHostile() && !isLeaving() && s != null;
        }

        @Override
        public void tick() {
            Player s = summoner();
            if (s == null) return;
            getLookControl().setLookAt(s, 30, 30);
            if (distanceToSqr(s) > 3.5 * 3.5) getNavigation().moveTo(s, 0.7);
            else getNavigation().stop();
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return isHostile() && super.canAttack(target);
    }

    // --- behaviour --------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isHostile() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // A business call, not a brawl: it takes offence and leaves.
            if (!isLeaving()) {
                if (source.getEntity() instanceof Player p) {
                    p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.offended")
                            .withStyle(ChatFormatting.DARK_RED), true);
                }
                leave();
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || isHostile() || isLeaving()) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (summonerId == null) {
            // Not called by anyone (a spawn egg, a command): it deals with whoever speaks first.
            summonerId = player.getUUID();
            summonerRef = player;
            life = DealTerms.NEUTRAL_LIFETIME;
        }
        if (player instanceof ServerPlayer sp) Deals.openOffer(sp, this);
        return InteractionResult.CONSUME;
    }

    /** The deal is sealed: a kiss, and soon it is gone. */
    public void sealed() {
        triggerAnim("action", "seal");
        playSound(AllSounds.CROSSROADS_SEAL.get(), 1.2f, 1f);
        getNavigation().stop();
        leaveIn = SEAL_TICKS;
    }

    /** Plays its "listen to this" gesture as it makes an offer. */
    public void offering() {
        triggerAnim("action", "offer");
    }

    /** Leaves in a column of black smoke. */
    public void leave() {
        if (!isSmoking()) startSmoking();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isHostile() || isSmoking()) return;
        if (leaveIn >= 0) {
            if (leaveIn-- == 0) leave();
            return;
        }
        if (summonerId == null) return; // waits, unbound, for someone to talk to it
        Player s = summoner();
        boolean present = s != null && s.isAlive() && distanceToSqr(s) < SUMMONER_RANGE * SUMMONER_RANGE;
        away = present ? 0 : away + 1;
        if (--life <= 0 || away > AWAY_TICKS) {
            if (s != null && life <= 0) {
                s.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.bored")
                        .withStyle(ChatFormatting.DARK_RED), true);
            }
            leave();
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && dead && isHostile()) Debts.onDemonSlain(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        boolean fled = !level().isClientSide && isHostile() && reason == RemovalReason.DISCARDED && !dead && !isDeadOrDying();
        super.remove(reason);
        if (fled) Debts.onDemonFled(this);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return isHostile();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.CROSSROADS_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    // --- GeckoLib / saving -----------------------------------------------------------------------

    @Override
    protected void registerExtraTriggers(AnimationController<DemonEntity> action) {
        action.triggerableAnim("seal", once("seal"));
        action.triggerableAnim("offer", once("offer"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Hostile", isHostile());
        tag.putBoolean("Wild", isWild());
        if (summonerId != null) tag.putUUID("Summoner", summonerId);
        if (debtorId != null) tag.putUUID("Debtor", debtorId);
        tag.putInt("Life", life);
        tag.putInt("LeaveIn", leaveIn);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(HOSTILE, tag.getBoolean("Hostile"));
        entityData.set(WILD, tag.getBoolean("Wild"));
        summonerId = tag.hasUUID("Summoner") ? tag.getUUID("Summoner") : null;
        debtorId = tag.hasUUID("Debtor") ? tag.getUUID("Debtor") : null;
        if (tag.contains("Life")) life = tag.getInt("Life");
        leaveIn = tag.contains("LeaveIn") ? tag.getInt("LeaveIn") : -1;
    }

    /** Test hook: ages the demon (ticks of its waiting left). */
    public void setLife(int ticks) {
        life = ticks;
    }
}
